package com.apple.iad.rhq.hadoop;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Measures DFS performance.
 */
public class DFSPerformance {

    private final Log log = LogFactory.getLog(HadoopDiscovery.class);

    private final FileSystem fs;
    private final Path path;
    // : not supported
    // https://issues.apache.org/jira/browse/HDFS-13
    private final SimpleDateFormat df = new SimpleDateFormat("HHmmss");
    private final int count;
    private final int size;
    private final byte buff[] = new byte[1024 * 10];

    static {
        // DataStreamer Exception: java.lang.NoClassDefFoundError:
        // Could not initialize class org.apache.hadoop.hdfs.protocol.FSConstants
        // ... seems that FSConstants does new Configuration(), which loads resources
        try {
            Class.forName("org.apache.hadoop.hdfs.protocol.FSConstants");
        } catch (Throwable t) {
        }
    }

    public DFSPerformance(FileSystem fs, String path, int count, int size) throws IOException {
        this.fs = fs;
        this.path = new Path(path);
        this.count = count;
        this.size = size;
        new Random().nextBytes(buff); // non-compressed
        log.debug("cleanup");
        fs.delete(this.path, true);
        fs.mkdirs(this.path);
    }

    /**
     * Returns milliseconds to complete this test.
     *
     * @throws IOException if it fails
     */
    public long test() throws IOException {
        log.debug("test count=" + count + " size=" + size);
        long start = System.currentTimeMillis();
        Date d = new Date();
        write(d);
        read(d);
        long end = System.currentTimeMillis();
        return (end - start);
    }

    private void write(Date d) throws IOException {
        log.debug("write");
        for (int i = 0; i < count; i++) {
            Path f = new Path(path, now(d, i));
            FSDataOutputStream os = fs.create(f, true, buff.length);
            int remain = size;
            do {
                os.write(buff);
            } while ((remain -= buff.length) > 0);
            os.flush();
            os.close();
            log.debug("create " + f);
        }
    }

    private void read(Date d) throws IOException {
        log.debug("read");
        for (int i = 0; i < count; i++) {
            Path f = new Path(path, now(d, i));
            FSDataInputStream is = fs.open(f);
            while (true) {
                int read = is.read(buff);
                if (read == -1)
                    break;
            }
            is.close();
            log.debug("read " + f);
        }
    }

    private String now(Date d, int i) {
        return df.format(d) + "_" + i;
    }
}
