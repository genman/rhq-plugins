package com.apple.iad.rhq.hadoop;

import java.io.IOException;
import java.net.URL;

import org.testng.annotations.Test;

public class JobTrackerInfoTest {

    @Test
    public void testHtml() throws IOException {
        URL url = new URL("http://vg61l01ad-hadoop002:50030/jobtracker.jsp");
        JobTrackerInfoHtml jt = new JobTrackerInfoHtml(url);
        jt.getAvailability();
    }
}
