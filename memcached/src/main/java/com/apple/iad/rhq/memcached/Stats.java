package com.apple.iad.rhq.memcached;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides memcached stats.
 */
public class Stats implements Closeable {

    public static final String GET_HITS = "get_hits";

    public static final String GET_MISSES = "get_misses";

    private static final Charset charset = Charset.forName("US-ASCII");
    private static final Pattern pattern = Pattern.compile("STAT (\\w+) (\\S+)");
    private final Socket s;
    private final BufferedReader reader;
    private final Writer writer;

    /**
     * Constructs a new Stats instance.
     */
    public Stats(Socket s) throws IOException {
        this.s = s;
        this.reader = new BufferedReader(new InputStreamReader(s.getInputStream(), charset));
        this.writer = new OutputStreamWriter(s.getOutputStream(), charset);
    }

    /**
     * Statistic information from memcached as a string-string map.
     * @throws IOException if stats can't be read or parsed
     */
    public Map<String, String> info() throws IOException {
        Map m = new HashMap<String, String>();
        writer.write("stats\r\n");
        writer.flush();
        while (true) {
            String line = reader.readLine();
            if (line == null)
                throw new EOFException("not expected");
            if (line.equals("END"))
                break;
            Matcher matcher = pattern.matcher(line);
            if (!matcher.matches())
                throw new IOException(" stats line '" + line + "' does not match " + pattern);
            m.put(matcher.group(1), matcher.group(2));
        }
        writer.write("stats slabs\r\n");
        writer.flush();
        while (true) {
            String line = reader.readLine();
            if (line == null)
                throw new EOFException("not expected");
            if (line.equals("END"))
                break;
            Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                // don't bother matching line with slab number
                //throw new IOException("stats slabs line " + line + " does not match " + pattern);
                continue;
            }
            m.put(matcher.group(1), matcher.group(2));
        }
        return m;
    }

    /**
     * Close the connection to memcached.
     */
    @Override
    public void close() throws IOException {
        writer.close();
        reader.close();
        s.close();
    }

}
