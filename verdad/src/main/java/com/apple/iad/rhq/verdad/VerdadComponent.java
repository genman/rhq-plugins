package com.apple.iad.rhq.verdad;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.parser.JSONParser;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InventoryContext;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;

/**
 * Stores a JSon tree representing 'the truth' about this host obtained from
 * Verdad.
 */
public class VerdadComponent implements ResourceComponent<VerdadComponent>, MeasurementFacet, ThreadFactory {

    private static final Log log = LogFactory.getLog(VerdadComponent.class);

    /**
     * Number of VerdadComponent instances created.
     */
    private static int instance = 0;

    /**
     * Configuration key; path to verdad.
     */
    public static final String EXECUTABLE = "executable";

    /**
     * Default path to 'vd'.
     */
    private static final String VD_PATH = "/usr/local/bin/vd";

    /**
     * Plugin configuration.
     */
    private Configuration conf;

    /**
     * Plugin system information.
     */
    private SystemInfo sysInfo;

    /**
     * Inventory context; useful for refreshing children.
     */
    private InventoryContext inventoryContext;

    /**
     * Information about this host.
     */
    private Map verdad = null;

    /**
     * Hashcode of output. This will hint if discovery should be run.
     */
    private int verdadHash = 0;

    /**
     * Hostname information.
     */
    private String hostname;

    /**
     * For async requests.
     */
    private ScheduledExecutorService pool;

    /**
     * @see #getAvailability()
     */
    private AvailabilityType avail = AvailabilityType.DOWN;

    /**
     * Registered observers.
     */
    private final Set<VerdadObserver> observers = new CopyOnWriteArraySet<VerdadObserver>();

    /**
     * Time in nanoseconds to refresh verdad information.
     */
    private int elapsed;

    /**
     * Root element, under the host.
     */
    private String root;

    /**
     * Always returns 'available' once the Verdad settings are loading. Note:
     * Availability could be set 'DOWN', like during a Verdad outage, but it
     * would stop child resources which need to run.
     */
    @Override
    public AvailabilityType getAvailability() {
        return avail;
    }

    /**
     * Returns the Verdad JSON document.
     */
    public Map getVerdad() {
        return verdad;
    }

    /**
     * Starts by running Verdad and then schedules a refresh thread.
     */
    @Override
    public void start(ResourceContext context) {
        conf = context.getPluginConfiguration();
        sysInfo = context.getSystemInformation();
        hostname = context.getSystemInformation().getHostname();
        inventoryContext = context.getInventoryContext();
        pool = Executors.newScheduledThreadPool(1, this);
        root = conf.getSimpleValue("root", "iad");
        refresh();

        int refresh = Integer.parseInt(conf.getSimpleValue("refresh", "15"));
        if (refresh > 0) {
            Runnable r = new Runnable() {
                public void run() {
                    refresh();
                }
            };
            pool.scheduleWithFixedDelay(r, refresh, refresh, TimeUnit.MINUTES);
        } else {
            log.info("refresh 0");
        }
    }

    /**
     * Called from the thread pool.
     */
    private void refresh() {
        log.debug("refresh");
        try {
            refresh(this.conf);
            refreshChildren();
        } catch (Throwable t) {
            log.error("could not refresh verdad: " + t);
            log.debug(t, t);
        }
    }

    void refreshChildren() {
        for (VerdadObserver o : observers)
            o.observe(verdad);
    }

    private void refresh(Configuration conf) throws Exception {
        long start = System.nanoTime();
        ProcessExecutionResults res = callVD(conf);
        String output = res.getCapturedOutput();
        if (res.getExitCode() == null)
            throw new RuntimeException("Process expired");
        int code = res.getExitCode();
        if (code != 0) {
            log.warn("vd failed with output: " + output);
            throw new RuntimeException("Non-zero exit status");
        }
        JSONParser reader = new JSONParser();
        verdad = (Map)reader.parse(output);
        avail = AvailabilityType.UP;

        int hc = output.hashCode();
        if (verdadHash != 0 && verdadHash != hc) {
            log.debug("vd tree changed, request discovery");
            inventoryContext.requestDeferredChildResourcesDiscovery();
        }
        verdadHash = hc;
        elapsed = (int) (System.nanoTime() - start);
    }

    private ProcessExecutionResults callVD(Configuration conf) {
        String vd = getVD(conf);
        long time = Long.parseLong(conf.getSimpleValue("timeout", "60"));
        ProcessExecution pe = new ProcessExecution(vd);
        String args = "expand -f json2_tree " + hostname;
        pe.setArguments(args.split(" "));
        pe.setKillOnTimeout(true);
        pe.setWaitForCompletion(time * 1000);
        pe.setCaptureOutput(true);
        ProcessExecutionResults res = sysInfo.executeProcess(pe);
        return res;
    }

    static String getVD(Configuration conf) {
        return conf.getSimpleValue(EXECUTABLE, VD_PATH);
    }

    /**
     * Registers a callback when Verdad is updated.
     */
    public void observe(VerdadObserver observer) {
        observers.add(observer);
    }

    /**
     * Removes a callback when Verdad is updated.
     */
    public void remove(VerdadObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void stop() {
        pool.shutdownNow();
        observers.clear();
    }

    int observerCount() {
        return observers.size();
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("verdad-" + instance++);
        return t;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();
            if (name.equals(MonitorComponent.ELAPSED)) {
                report.addData(new MeasurementDataNumeric(request, (double)elapsed));
            }
        }
    }

    public String getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return "VerdadComponent [verdadHash=" + verdadHash + ", hostname="
                + hostname + ", avail=" + avail + "]";
    }

}
