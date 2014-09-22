package com.apple.iad.rhq.verdad;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.StringUtil;

/**
 * Runs a filesystem executable ('script' usually) as defined in Verdad,
 * assuming these script commands behave similarly to a Nagios script, which
 * return 0 on okay, 1 on warning, and 2 on error.
 * <p/>
 * Events are returned to RHQ when the exit code changes (say, 0 to 1) and
 * alerting should be done by watching for events. Severity values correspond to
 * exit code.
 * <p/>
 * This resource is considered UP if the script executes to completion with 0 or 1 exit code.
 * DOWN means the script failed to run at all or returned non 0 or 1 status.
 * <p/>
 * Settings are obtained from JSON output by the vd command. The parse tree appears like so:
 * <pre>
 {
   "hostname" : {
      "rhq" : {
         "monitor" : {
            "enabled" : [
               "true"
            ],
            "env" : {
               "PATH" : [
                  "/opt/iad/monitor/bin:/usr/lib64/nagios/plugins:/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/sbin"
               ]
            },
            "interval" : [
               "5"
            ],
            "service" : {
               "check_mem" : {
                  "interval" : [ "60" ],
                  "source" : [ "global" ],
                  "enabled" : [ "true" ],
                  "description" : [ "Checks" ]
               },
 * </pre>
 */
public class MonitorComponent
    implements MeasurementFacet, ResourceComponent<VerdadComponent>, VerdadObserver, OperationFacet
{

    /**
     * Environment variable tag.
     */
    private static final String ENV = "env";

    /**
     * Default shell to run if command is not absolute path defined.
     */
    private static final String SHELL = "/bin/sh";

    /**
     * Truncates output to this number of characters.
     */
    private static final int OUTPUT_MAX = 1024;

    /**
     * Event type this component generates.
     */
    public static final String EVENT_TYPE = "vdmonitor";

    /**
     * Verdad setting timeout.
     */
    public static final String TIMEOUT = "timeout";

    /**
     * Verdad setting enabled; if absent means true.
     */
    public static final String ENABLED = "enabled";

    /**
     * Verdad setting args; assumed to be an array.
     */
    public static final String ARGS = "args";

    /**
     * Verdad setting command; should be command in the path or fully qualified command.
     */
    public static final String COMMAND = "command";

    /**
     * Verdad setting and metric trait 'group'.
     */
    public static final String GROUP = "group";

    /**
     * Metric 'elapsed' time.
     */
    public static final String ELAPSED = "elapsed";

    /**
     * Metric trait command output.
     */
    public static final String OUTPUT = "output";

    /**
     * Operation 'invoke'.
     */
    public static final String INVOKE = "invoke";

    private static final int SEC_TO_MS = 1000;

    private static final Log log = LogFactory.getLog(MonitorComponent.class);

    /**
     * Parent reference; for getting document settings.
     */
    private VerdadComponent parent;

    /**
     * Resource key; which is the name of the service monitored.
     */
    private String key;

    /**
     * Settings for this script.
     */
    private volatile Map<String, Object> settings = emptyMap();

    /**
     * Shell env.
     */
    private Map<String, String> env = emptyMap();

    /**
     * Last output string.
     */
    private volatile String output = "";

    /**
     * Last exit code.
     */
    private volatile int lastExitCode = 0;

    /**
     * Last elapsed time to execute this command.
     */
    private volatile int elapsed;

    /**
     * Plugin provided; used to execute commands.
     */
    private SystemInfo info;

    /**
     * Event context for this resource.
     */
    private EventContext eventContext;

    /**
     * Default constructor.
     */
    public MonitorComponent() {
    }

    /**
     * Constructor used by discovery.
     */
    MonitorComponent(VerdadComponent parent, String key) throws MissingSettingsException {
        this.parent = parent;
        this.key = key;
        settings(parent.getVerdad());
    }

    MonitorComponent(VerdadComponent parent) {
        this.parent = parent;
    }

    /**
     * Starts monitoring a command.
     * Monitoring happens asynchronously, using the availability collector thread pool.
     */
    public void start(ResourceContext<VerdadComponent> context) {
        parent = context.getParentResourceComponent();
        key = context.getResourceKey();
        eventContext = context.getEventContext();
        parent.observe(this);
        info = context.getSystemInformation();
        observe(parent.getVerdad());
    }

    /**
     * Returns the current availability.
     */
    AvailabilityType getAvailabilityCurrent() {
        log.debug("getAvailabilityCurrent() " + key);
        if (settings.isEmpty()) {
            log.debug("no settings; nothing to do");
            return UP;
        }
        if (!settingTrue(ENABLED)) {
            log.debug("disabled");
            return UP;
        }
        ProcessExecution pe = getExecution();
        try {
            if (log.isDebugEnabled())
                log.debug("executing: " + pe);
            long start = System.nanoTime();
            ProcessExecutionResults result = info.executeProcess(pe);
            if (result.getError() != null)
                throw new Exception(result.getError());
            if (result.getExitCode() == null)
                throw new IllegalStateException("unable to get exit code; process timed out?");
            output = result.getCapturedOutput();
            if (output.length() > OUTPUT_MAX)
                output = output.substring(0, OUTPUT_MAX);
            if (log.isDebugEnabled())
                log.debug("output:\n" + output);

            int exitCode = result.getExitCode();
            elapsed = (int)(System.nanoTime() - start);
            if (exitCode != lastExitCode) {
                log.debug("exit code " + exitCode + " changed, send event");
                EventSeverity es = severity(exitCode);
                if (output.length() == 0)
                    output = "exit code " + exitCode;
                Event event = new Event(EVENT_TYPE, key, System.currentTimeMillis(), es, output);
                eventContext.publishEvent(event);
                lastExitCode = exitCode;
            }

        } catch (Exception e) {
            log.error("failed to execute", e);
            return DOWN;
        }
        switch (lastExitCode) {
        case 0:
        case 1: return UP;
        default: return DOWN;
        }
    }

    /**
     * Stops this resource.
     */
    public void stop() {
        parent.remove(this);
        parent = null;
        settings = emptyMap();
    }

    /**
     * Returns a setting for a key, or default if not defined.
     */
    private int setting(String key, int defaultValue) {
        String s = (String) settings.get(key);
        if (s == null || s.isEmpty())
            return defaultValue;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            log.warn(this.key + "." + key + " invalid: " + s);
            return defaultValue;
        }
    }

    /**
     * Returns a setting for a key, or default if not defined.
     */
    String setting(String key, String defaultValue) {
        String s = (String) settings.get(key);
        if (s == null || s.isEmpty())
            return defaultValue;
        return s;
    }

    /**
     * Returns a setting.
     */
    private String setting(String name) {
        return (String)settings.get(name);
    }

    /**
     * Returns true if the setting is set to true or not defined.
     */
    boolean settingTrue(String key) {
        return Boolean.parseBoolean(setting(key, "true"));
    }

    /**
     * Returns all settings. These cannot be modified.
     */
    Map<String, Object> getSettings() {
        return settings;
    }

    /**
     * Returns UP if beans exist in this group.
     */
    public AvailabilityType getAvailability() {
        return getAvailabilityCurrent();
    }

    /**
     * Calculates the settings for this service.
     */
    @Override
    public void observe(Map verdad) {
        try {
            settings(verdad);
        } catch (MissingSettingsException e) {
            settings = emptyMap();
            log.info(e.toString());
        }
    }

    private void settings(Map verdad) throws MissingSettingsException {
        if (verdad == null) {
            // should not happen once the service starts
            throw new MissingSettingsException("verdad is down; so can't build settings");
        }
        Map<String, Map> monitor = monitorNode(verdad);
        if (monitor == null) {
            throw new MissingSettingsException("monitor removed");
        }
        Map<String, List> service = serviceNode(monitor, key);
        if (service == null) {
            throw new MissingSettingsException("key " + key + " not found; service removed?");
        }
        env = baseEnv();
        Map<String, Object> map = new HashMap<String, Object>();
        toMap(map, monitor);
        toMap(map, service); // service override settings
        if (!map.containsKey(ENABLED))
            map.put(ENABLED, Boolean.toString(true));
        settings = unmodifiableMap(map);
    }

    static Map<String, List> serviceNode(Map<String, Map> monitor, String key) {
        return (Map<String, List>) monitor.get("service").get(key);
    }

    Map<String, Map> monitorNode(Map verdad) {
        if (verdad.isEmpty()) // empty document
            return null;
        Map<String, Map> host = (Map<String, Map>) verdad.values().iterator().next();
        Map<String, Map> root = host.get(parent.getRoot());
        if (root == null)
            return null;
        Map<String, Map> monitor = root.get("monitor");
        return monitor;
    }

    private void toMap(Map<String, Object> result, Map<String,?> monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        List<String> args = new ArrayList<String>();
        map.put(ARGS, args);
        for (Map.Entry<String, ?> e : monitor.entrySet()) {
            String name = e.getKey();
            if (name.equals(ENV)) {
                Map<String, List> v = (Map<String, List>) e.getValue();
                for (Entry<String, List> e2: v.entrySet()) {
                    env.put(e2.getKey(), toString(e2.getValue()));
                }
                continue;
            }
            if (!(e.getValue() instanceof List)) {
                continue;
            }
            List<String> value = (List<String>) e.getValue();
            if (name.equals(ARGS)) {
                args.addAll(value);
            } else {
                map.put(name, toString(value));
            }
        }
        result.putAll(map);
    }

    private String toString(List values) {
        StringBuilder sb = new StringBuilder();
        for (Object o : values) {
            sb.append(o);
        }
        return sb.toString();
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();
            if (name.equals(ELAPSED)) {
                report.addData(new MeasurementDataNumeric(request, (double)elapsed));
            } else if (name.equals(ENABLED)) {
                report.addData(new MeasurementDataTrait(request, setting(ENABLED, Boolean.toString(false))));
            } else if (name.equals(GROUP)) {
                report.addData(new MeasurementDataTrait(request, setting(GROUP)));
            } else if (name.equals(OUTPUT)) {
                report.addData(new MeasurementDataTrait(request, setting(OUTPUT)));
            } else {
                log.warn("unknown metric " + name);
            }
        }
    }

    private ProcessExecution getExecution() {
        int timeout = setting(TIMEOUT, 60);
        String command = setting(COMMAND, MonitorComponent.this.key);
        List<String> args = (List<String>) settings.get(ARGS);
        if (!new File(command).isAbsolute()) {
            // execute using a shell
            args = new LinkedList<String>(args);
            args.add(0, command);
            String scmd = StringUtil.iteratorToString(args.iterator(), " ", "'");
            command = SHELL;
            args.clear();
            args.add("-c");
            args.add(scmd);
        }

        ProcessExecution pe = new ProcessExecution(command);
        pe.setArguments(args);
        pe.setCaptureOutput(true);
        pe.setKillOnTimeout(true);
        pe.setWaitForCompletion(timeout * SEC_TO_MS);
        pe.setEnvironmentVariables(env);
        return pe;
    }

    private static Map<String, String> baseEnv() {
        Map<String, String> env = new HashMap<String, String>();
        env.put("HOME", System.getProperty("user.home"));
        return env;
    }

    private EventSeverity severity(int exitCode) throws Exception {
        switch (exitCode) {
        case 0: return EventSeverity.INFO;
        case 1: return EventSeverity.WARN;
        case 2: return EventSeverity.ERROR;
        default: throw new Exception("unexpected exit code " + exitCode + ": " + output.trim());
        }
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (settings.isEmpty()) {
            throw new MissingSettingsException("no settings");
        }
        if (name.equals(INVOKE)) {
            ProcessExecution pe = getExecution();
            ProcessExecutionResults result = info.executeProcess(pe);
            if (result.getError() != null)
                throw new Exception(result.getError());

            OperationResult or = new OperationResult();
            Configuration config = or.getComplexResults();
            config.setSimpleValue("exitCode", valueOf(result.getExitCode()));
            config.setSimpleValue("output", valueOf(result.getCapturedOutput()));
            return or;
        }
        throw new UnsupportedOperationException("unknown operation " + name);
    }

    @Override
    public String toString() {
        return "MonitorComponent [key=" + key
                + ", settings=" + settings
                + ", output=" + output + ", lastExitCode=" + lastExitCode
                + ", elapsed=" + elapsed + "]";
    }

}
