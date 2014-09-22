package com.apple.iad.rhq.hadoop.hive;

import static java.util.Collections.emptySet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

public class HiveTable implements ResourceComponent<HiveTable>, MeasurementFacet {

    private final Log log = LogFactory.getLog(getClass());
    private volatile Set<String> paths = emptySet();
    private HiveServerComponent server;
    private String table;
    private FileSystem fileSystem;

    @Override
    public AvailabilityType getAvailability() {
        try {
            updatePaths();
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
        return AvailabilityType.UP;
    }

    @Override
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        server = (HiveServerComponent)context.getParentResourceComponent();
        table = context.getResourceKey();
        fileSystem = server.getFileSystem();
        updatePaths();
    }

    @Override
    public void stop() {
        server = null;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        try {
            getValues0(report, metrics);
        } catch (IOException e) {
            log.warn("could not obtain values " + this, e);
        }
    }

    public void getValues0(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        long length = 0;
        long fileCount = 0;
        long directoryCount = 0;
        long spaceConsumed = 0;
        for (String path : paths) {
            log.debug("get summary " + path);
            try {
                ContentSummary cs = fileSystem.getContentSummary(new Path(path));
                log.debug("got summary " + cs);
                length += cs.getLength();
                fileCount += cs.getFileCount();
                directoryCount += cs.getDirectoryCount();
                spaceConsumed += cs.getSpaceConsumed();
            } catch (FileNotFoundException e) {
                log.debug("file not found");
            }
        }
        for (MeasurementScheduleRequest mr : metrics) {
            String n = mr.getName();
            if (n.equals("length"))
                report.addData(new MeasurementDataNumeric(mr, (double)length));
            if (n.equals("fileCount"))
                report.addData(new MeasurementDataNumeric(mr, (double)fileCount));
            if (n.equals("directoryCount"))
                report.addData(new MeasurementDataNumeric(mr, (double)directoryCount));
            if (n.equals("spaceConsumed"))
                report.addData(new MeasurementDataNumeric(mr, (double)spaceConsumed));
        }
    }

    private void updatePaths() throws SQLException {
        long start = System.currentTimeMillis();
        // the parent MySql connection is shared, do not close it
        Connection connection = server.getConnection();
        PreparedStatement ps = connection.prepareStatement(server.getLocationQuery());
        Set<String> paths2 = new HashSet<String>();
        try {
            ps.setString(1, table);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String path = rs.getString(1);
                path = fixPath(path);
                if (fileSystem.exists(new Path(path))) {
                    paths2.add(path);
                } else {
                    // this can happen if there are old partitions still around
                    if (log.isDebugEnabled())
                        log.debug("path does not exist " + path);
                }
            }
            this.paths = paths2;
            rs.close();
        } catch (Exception e) {
            log.info("unable to get information for table " + table, e);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("update table " + table + " in " + elapsed + "ms");
            ps.close();
        }
    }

    /**
     * Remove all the partition stuff
     */
    static String fixPath(String path) {
        try {
            path = path.replace(' ', '+');
            path = path.replaceAll("/[^/=]+=[^/=]+", "");
            path = path.replaceAll("/\\d{4}-\\d{2}-\\d{2}", ""); // dates
            path = path.replaceAll("/\\d{4,}\\b", ""); // 201111040900
            path = new URI(path).getPath();
            return path;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "HiveTable [table=" + table + ", paths=" + paths.size() + "]";
    }

}
