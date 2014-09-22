package com.apple.iad.rhq.hadoop;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Set;

import org.apache.hadoop.fs.FileSystem;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

public class NameNodeComponent extends HadoopComponent implements MeasurementFacet {

    /**
     * CDH4 and up should use fs.defaultFS instead.
     */
    private static final String FS_DEFAULT_NAME = "fs.default.name";
    private FileSystem fileSystem;
    private DFSPerformance dfs;
    private JSONProvider provider;
    private boolean standby;

    @Override
    public void start(ResourceContext context) throws Exception {
        super.start(context);
        log.debug("start");
        try {
            init();
        } catch (Exception e) {
            log.warn("cannot initialize HDFS connection", e);
        }
    }

    private void init() throws IOException {
        if (fileSystem != null)
            return;
        Configuration c = getResourceContext().getPluginConfiguration();
        org.apache.hadoop.conf.Configuration hc = new org.apache.hadoop.conf.Configuration();
        // this can cause issues; disable
        // hc.setQuietMode(false);
        hc.setClassLoader(hc.getClass().getClassLoader());
        // should be set in core-site.xml
        // hc.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        // hc.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
        String name = c.getSimpleValue(FS_DEFAULT_NAME, "");
        String hostname = InetAddress.getLocalHost().getHostName();
        name = name.replace("localhost", hostname);
        hc.set(FS_DEFAULT_NAME, name);
        fileSystem = FileSystem.get(hc);

        String url = c.getSimpleValue("json.url", "");
        provider = new JSONProvider(new URL(url));

        log.debug("configure DFS testing");
        String path = c.getSimpleValue("dfs.test.dir", "/tmp");
        int count = Integer.parseInt(c.getSimpleValue("dfs.test.files", "0"));
        standby = Boolean.parseBoolean(c.getSimpleValue("standby", "false"));
        if (count > 0 && !standby) {
            int size = Integer.parseInt(c.getSimpleValue("dfs.test.bytes", "0"));
            dfs = new DFSPerformance(fileSystem, path, count, size);
        }
    }

    /**
     * Returns UP if we can get the status of the root file and connect
     * via JMX.
     */
    @Override
    public AvailabilityType getAvailability() {
        if (Thread.interrupted()) {
            // some reason RHQ interrupts this thread before we even connect
            log.info("thread was interrupted, clear state, return UP");
            return AvailabilityType.UP;
        }
        try {
            init();
            log.debug("getStatus");
            fileSystem.getStatus();
            log.debug("done");
        } catch (IOException e) {
            if (standby && e.toString().contains("StandbyException")) {
                log.debug("standby=true and standby exception; return up");
                return super.getAvailability();
            }
            log.warn("filesystem get status failed", e);
            try {
                if (fileSystem != null)
                    fileSystem.close();
            } catch (IOException e2) {
            }
            fileSystem = null; // re-initialize next time
            return AvailabilityType.DOWN;
        }
        return super.getAvailability();
    }

    @Override
    public void stop() {
        super.stop();
        try {
            if (fileSystem != null)
                fileSystem.close();
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    public JSONProvider getProvider() {
        return provider;
    }

    /**
     * Returns this file system.
     */
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        for (MeasurementScheduleRequest metric : metrics) {
            if (metric.getName().equals("DFSPerformance") && dfs != null) {
                report.addData(new MeasurementDataNumeric(metric, (double)dfs.test()));
            }
        }
    }

}
