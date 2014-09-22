package com.apple.iad.rhq.hadoop;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Test
public class QueueDetailsTest {

    public void test() throws SAXException, IOException {
       JobParser p = new JobParser(); 
       InputStream is = getClass().getResourceAsStream("/jobqueue_details.jsp.html");
       assert is != null;
       p.parse(new InputSource(is));
       assertEquals(22, p.count);
       assertEquals(716, p.col[0]);
       assertEquals(297, p.col[1]);
       assertEquals(22, p.col[2]);
       assertEquals(2, p.col[3]);
    }
}
