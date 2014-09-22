package com.apple.iad.rhq.hadoop;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Calls 'fsck' on a NameNode.
 */
public class NameNodeFsckComponent implements ResourceComponent<NameNodeComponent>, MeasurementFacet {

    /**
     * URL key.
     */
    public static final String URL = "url";

    /**
     * Path key.
     */
    public static final String PATH = "path";

    /**
     * Size key.
     */
    public static final String SIZE = "size";

    protected final Log log = LogFactory.getLog(getClass());

    final static Pattern PATTERN = Pattern.compile("(/[/\\S]+)[^/]+\\bOPENFORWRITE\\b");
    private final String defaultUrl = "http://localhost:50070/fsck?ugi=rhq";
    private NameNodeComponent parent;
    private Path path;
    private String url;

    /**
     * Ignore files older than this (in milliseconds).
     */
    private int age;

    /**
     * Ignore files smaller or equal to this (in bytes).
     */
    private int size = -1;

    @Override
    public AvailabilityType getAvailability() {
        try {
            return parent.getFileSystem().exists(path) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(ResourceContext<NameNodeComponent> context)
            throws InvalidPluginConfigurationException, Exception
    {
        parent = context.getParentResourceComponent();
        Configuration conf = context.getPluginConfiguration();
        String path = conf.getSimpleValue(PATH, "/tmp");
        this.path = new Path(path);
        url = conf.getSimpleValue(URL, defaultUrl);
        age = Integer.parseInt(conf.getSimpleValue("ignore.age", "60")) * 1000;
        size = Integer.parseInt(conf.getSimpleValue(SIZE, "-1"));
    }

    @Override
    public void stop() {
    }

    @Override
    public void getValues(MeasurementReport report,
            Set<MeasurementScheduleRequest> metrics) throws Exception {
        for (MeasurementScheduleRequest r : metrics) {
            if (r.getName().equals("OpenForWrite")) {
                int open = getOpenForWrite();
                report.addData(new MeasurementDataNumeric(r, (double)open));
            }
        }
    }

    private int getOpenForWrite() throws IOException {
        URL url = getURL();
        log.debug("getOpenForWrite " + url);
        InputStream is = url.openStream();
        try {
            return countOpen(is);
        } finally {
            is.close();
        }
    }

    private URL getURL() throws MalformedURLException {
        return new URL(this.url + "&path=" + this.path.toUri().getPath() + "&openforwrite=1");
    }

    int countOpen(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        int count = 0;
        long before = System.currentTimeMillis() - age;
        String line = null;
        boolean debug = log.isDebugEnabled();
        while ((line = br.readLine()) != null) {
            if (debug)
                log.debug(">> " + line);
            Matcher m = PATTERN.matcher(line);
            while (m.find()) {
                String path = m.group(1);
                if (debug)
                    log.debug("check " + path);
                FileSystem fileSystem = parent.getFileSystem();
                assert fileSystem != null;
                FileStatus status;
                try {
                    status = fileSystem.getFileStatus(new Path(path));
                } catch (FileNotFoundException e) {
                    log.warn("file not found");
                    continue;
                }
                if (status.getLen() <= size) {
                    log.debug("too small");
                    continue;
                }
                if (status.getModificationTime() < before) {
                    log.info("open file: " + path);
                    count++;
                } else {
                    log.debug("too new");
                }
            }
        }
        return count;
    }

}
