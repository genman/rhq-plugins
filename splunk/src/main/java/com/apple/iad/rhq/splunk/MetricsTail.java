package com.apple.iad.rhq.splunk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Metrics tail file.
 */
public class MetricsTail {

    /**
     * Number of milliseconds since the metrics file was written to.
     */
    public static final String RECENCY = "recency";

    private static final Log log = LogFactory.getLog(MetricsTail.class);

    private final Map<String, Double> values = new HashMap<String, Double>();

    /**
     * Supported metrics to return.
     */
    public static final String names[] = {
        "queue.tcpout_ssl_group.current_size",
        "thruput.thruput.total_k_processed",
        "queue.parsingqueue.current_size_kb",
        "thruput.index_thruput.total_k_processed",
    };

    /**
     * Pattern matching metric line.
     */
    public final static Pattern metrics =
            Pattern.compile("group=(\\w+), name=(\\w+), (.*)");

    /**
     * Pattern matching event line.
     */
    public final static Pattern event =
            Pattern.compile("eventType=(\\w+)");

    /**
     * Pattern matching key-value.
     * max_size_kb=500, current_size_kb=0, current_size=0, largest_size=0, smallest_size=0");
     */
    public final static Pattern nv =
            Pattern.compile("(\\w+)=(\\d+)");

    /**
     * Metrics logfile.
     */
    private final File logfile;

    /**
     * Metrics logfile position.
     */
    private long pos;

    /**
     * Count of number of events (cumulative).
     * @see #event
     */
    private long events = 0;

    private static final Charset charset = Charset.forName("ASCII");

    /**
     * Metrics tailer.
     */
    public MetricsTail(File logfile) {
        this.logfile = logfile;
    }

    /**
     * Tails additional data.
     * @return true if more data was read, false if not.
     */
    public boolean read() throws IOException {
        long filelen = logfile.length();
        boolean read = false;
        if (!logfile.canRead())
            throw new IOException("cannot read " + logfile);
        if (pos > filelen) {
            log.debug("file changed renamed");
            pos = 0;
        }
        Map<String, Double> allvalues = new HashMap<String, Double>();
        if (pos < filelen) {
            log.debug("more data to tail");
            FileInputStream fis = new FileInputStream(logfile);
            FileChannel channel = fis.getChannel();
            channel.position(pos);
            BufferedReader reader = new BufferedReader(Channels.newReader(channel, charset.newDecoder(), -1));
            try {
                parse(allvalues, reader);
                pos = channel.position();
            } finally {
                reader.close(); // this closes everything
                fis.close(); // but just to suppress warnings
            }
            log.debug("parsed");
            read = true;
        } else if (pos == filelen) {
            log.debug("eof");
        }
        for (String name : names) {
            Double d = allvalues.get(name);
            if (d != null)
                this.values.put(name, d);
        }
        long recency = System.currentTimeMillis() - logfile.lastModified();
        values.put(RECENCY, (double)recency);
        values.put("events", (double)events);
        return read;
    }

    private void parse(Map<String, Double> allvalues, BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher m = metrics.matcher(line);
            if (m.find()) {
                String g = m.group(1);
                String n = m.group(2);
                String nvs = m.group(3);
                m = nv.matcher(nvs);
                while (m.find()) {
                    String k = m.group(1);
                    String v = m.group(2);
                    k = g + "." + n + "." + k;
                    // log.info(k + "=" + v);
                    allvalues.put(k, Double.parseDouble(v));
                }
            } else if (event.matcher(line).find()) {
                events++;
            } else {
                // log.debug("no match");
            }
        }
    }

    /**
     * Return the value for this key.
     */
    public Double getValue(String key) {
        return values.get(key);
    }

    @Override
    public String toString() {
        return "MetricsTail [values=" + values + ", logfile=" + logfile + ", pos=" + pos + "]";
    }

}
