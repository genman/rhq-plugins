package com.apple.iad.rhq.hadoop;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.xml.sax.InputSource;

/**
 * Provides data about a job tracker queue from a HTML page.
 */
public class QueueData {

    int mapTotal;
    int reducersTotal;
    int mapComplete;
    int reducersComplete;
    int jobCount;

    /**
     * Constructs an instance accessing a given URL.
     */
    public QueueData(URL url) throws IOException {
        URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(JobTrackerQueue.TIMEOUT);
        urlConnection.setReadTimeout(JobTrackerQueue.TIMEOUT);
        InputStream is = urlConnection.getInputStream();
        try {
            JobParser parser = new JobParser();
            parser.parse(new InputSource(is));
            mapTotal         = parser.col[0];
            reducersTotal    = parser.col[1];
            mapComplete      = parser.col[2];
            reducersComplete = parser.col[3];
            jobCount = parser.count;
        } catch (Exception e) {
            throw new IOException("unable to parse document", e);
        } finally {
            is.close();
        }
    }

    @Override
    public String toString() {
        return "QueueData [mapTotal=" + mapTotal + ", reducersTotal="
                + reducersTotal + ", mapComplete=" + mapComplete
                + ", reducersComplete=" + reducersComplete + "]";
    }

}