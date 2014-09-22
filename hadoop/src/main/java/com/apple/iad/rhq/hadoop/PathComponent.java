package com.apple.iad.rhq.hadoop;

import static java.lang.Integer.parseInt;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Looks recursively at the size of files in a path.
 */
public class PathComponent implements ResourceComponent, MeasurementFacet {

    private static final String MODIFICATION_TIME = "modificationTime";

    private static final String MODIFICATION_RECENCY = "modificationRecency";

    protected final Log log = LogFactory.getLog(getClass());

    /**
     * URL key.
     */
    public static final String URL = "url";

    /**
     * Path key.
     */
    public static final String DEPTH = "depth";

    /**
     * Size key.
     */
    public static final String SIZE = "size";

    /**
     * Files key.
     */
    public static final String FILES = "files";

    private Path path;
    private int maxDepth;

    private FileSystem fs;

    private final boolean debug = log.isDebugEnabled();
    private boolean refresh = false;
    private long size;
    private long files;
    private long unavailable;

    @Override
    public AvailabilityType getAvailability() {
        try {
            return fs.exists(path) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (IOException e) {
            log.warn("fs.exist failed for " + path, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(ResourceContext context)
            throws InvalidPluginConfigurationException, Exception
    {
        Configuration conf = context.getPluginConfiguration();
        String path = conf.getSimpleValue(URL);
        this.fs = FileSystem.get(new URI(path), new org.apache.hadoop.conf.Configuration());
        this.path = new Path(path);
        this.maxDepth = parseInt(conf.getSimpleValue(DEPTH, "100"));
    }

    @Override
    public void stop() {
        try {
            fs.close();
        } catch (IOException e) {
        }
    }

    @Override
    public void getValues(MeasurementReport report,
            Set<MeasurementScheduleRequest> metrics) throws Exception {
        refresh = true;
        for (MeasurementScheduleRequest r : metrics) {
            String name = r.getName();
            if (name.equals(SIZE)) {
                calc0(path);
                report.addData(new MeasurementDataNumeric(r, (double)size));
            } else if (name.equals(FILES)) {
                calc0(path);
                report.addData(new MeasurementDataNumeric(r, (double)files));
            } else if (name.equals(MODIFICATION_RECENCY)) {
                long m = System.currentTimeMillis() - fs.getFileStatus(path).getModificationTime();
                report.addData(new MeasurementDataNumeric(r, (double)m));
            } else if (name.equals(MODIFICATION_TIME)) {
                long m = fs.getFileStatus(path).getModificationTime();
                report.addData(new MeasurementDataNumeric(r, (double)m));
            }
        }
    }

    private void calc0(Path path) throws IOException {
        // only calculate size/files once per call
        if (refresh) {
            refresh = false;
            size = 0;
            files = 0;
            unavailable = 0;
            calc(path, 0);
            log.debug("done");
        } else {
            log.debug("cached");
        }
    }

    private void calc(Path path, int depth) throws IOException {
        if (debug)
            log.debug("calc " + path);
        FileStatus status = fs.getFileStatus(path);
        size += status.getLen();
        files++;

        if (status.isDir() && depth < maxDepth) {
            try {
                FileStatus[] ss = fs.listStatus(path);
                for (FileStatus fs : ss) {
                    calc(new Path(path, fs.getPath().getName()), depth + 1);
                }
            } catch (Exception e) {
                log.warn("could not access " + path + " " + e);
                unavailable++;
            }
        }
    }

    @Override
    public String toString() {
        return "PathComponent [path=" + path + ", fs=" + fs + "]";
    }

}
