package com.apple.iad.rhq.splunk;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

public class MetricsTailTest {

    private static final Log log = LogFactory.getLog(MetricsTailTest.class);

    @Test
    public void testit() throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        ArrayList<URL> list = Collections.list( cl.getResources("metrics.log"));
        log.info("list " + list);
        URL url = cl.getResource("metrics.log");
        assert url != null;
        String file = url.getFile();
        MetricsTail tail = new MetricsTail(new File(file));
        log.info("read()");
        assert tail.read();
        log.info("tail " + tail);
        assertEquals(42.0, tail.getValue( MetricsTail.names[0]), .1);
        assertEquals(13976121.0, tail.getValue( MetricsTail.names[1]), .1);
        assertEquals(2.0, tail.getValue( MetricsTail.names[2]), .1);
        assertEquals(1568588.0, tail.getValue( MetricsTail.names[3]), .1);
        assertEquals(2, tail.getValue("events"), .1);

        log.info("expect eof");
        assert !tail.read();
    }

}
