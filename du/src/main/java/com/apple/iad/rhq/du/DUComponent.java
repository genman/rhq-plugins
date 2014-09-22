package com.apple.iad.rhq.du;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;

/**
 * Component for monitoring disk usage ('DU').
 */
public class DUComponent implements ResourceComponent<ResourceComponent<?>>, MeasurementFacet {

    public static final String DIR = "dir";

    private static final String EXECUTABLE = "executable";

    private static final String WAIT_FOR_COMPLETION = "waitForCompletion";

    private static final Log log = LogFactory.getLog(DUComponent.class);

    private final static Pattern splitter = Pattern.compile("\\s+");
    private final static Pattern sizes = Pattern.compile("^(\\d+)");

    private SystemInfo systemInformation;

    /**
     * 'du' exec path.
     */
    private String exec;

    /**
     * List of arguments for 'du'.
     */
    private List<String> args;

    /**
     * Directory to monitor.
     */
    private String dir;

    /**
     * How many seconds to wait for exec completion.
     */
    private int waitForCompletion;

    @Override
    public AvailabilityType getAvailability() {
        return new File(dir).canRead() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        if (metrics.isEmpty())
            return;
        ProcessExecution pe = new ProcessExecution(exec);
        pe.setCaptureOutput(true);
        pe.setWaitForCompletion(waitForCompletion * 1000);
        pe.setKillOnTimeout(true);
        pe.setArguments(args);

        long start = System.nanoTime();
        ProcessExecutionResults result = systemInformation.executeProcess(pe);
        long stop = System.nanoTime() - start;

        if (result.getExitCode() == null || result.getError() != null) {
            throw new Exception("unable to execute " + result);
        }
        long size = 0;
        boolean found = false;
        log.debug("results of " + this);
        log.debug(result.getCapturedOutput());
        Matcher m = sizes.matcher(result.getCapturedOutput());
        while (m.find()) {
            found = true;
            size += Long.parseLong(m.group(1));
        }
        if (!found) {
            throw new Exception("no size in output " + result);
        }

        for (MeasurementScheduleRequest msr : metrics) {
            String name = msr.getName();
            if (name.equals("time")) {
                report.addData(new MeasurementDataNumeric(msr, (double) stop));
            } else if (name.equals("k")) {
                report.addData(new MeasurementDataNumeric(msr, (double) size));
            }
        }
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws InvalidPluginConfigurationException, Exception {
        systemInformation = context.getSystemInformation();
        Configuration config = context.getPluginConfiguration();
        exec = config.getSimpleValue(EXECUTABLE, "/usr/bin/du");
        dir = config.getSimpleValue(DIR, "/tmp");
        waitForCompletion = parseInt(config.getSimpleValue(WAIT_FOR_COMPLETION, "60"));
        if (waitForCompletion <= 0)
            throw new InvalidPluginConfigurationException(WAIT_FOR_COMPLETION);
        String s = config.getSimpleValue("args", "-k -s");
        args = new ArrayList<String>();
        args.addAll(asList(splitter.split(s)));
        args.addAll(asList(splitter.split(dir)));
    }

    @Override
    public void stop() {
        systemInformation = null;
    }

    @Override
    public String toString() {
        return "DUComponent [exec=" + exec + ", args=" + args + "]";
    }

    void setDir(String dir) {
        this.dir = dir;
    }

}
