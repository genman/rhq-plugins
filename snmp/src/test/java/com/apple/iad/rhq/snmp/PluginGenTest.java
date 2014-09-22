package com.apple.iad.rhq.snmp;

import static com.apple.iad.rhq.snmp.PluginGen.trait;
import static com.apple.iad.rhq.snmp.PluginGen.units;
import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

@Test
public class PluginGenTest {

    private Log log = LogFactory.getLog(getClass());

    public void desc() throws IOException {
        String s = "SNMPv2-MIB";
        StringWriter sw = new StringWriter();
        PluginGen pg = new PluginGen(new String[] { s });
        pg.output(sw);
        log.info("Output:\n" + sw);
    }

    @Test
    public void testUnits() {
        assertEquals("bytes", units("number of Bytes sent"));
        assertEquals("kilobytes", units("number of KB sent"));
        assertEquals("megabytes", units("number of Mbytes sent"));
        assertEquals("seconds", units("number of SECONDS since"));
        assertEquals("milliseconds", units("number of milliSECONDS since"));
        assertEquals("kilobytes", units("The total disk space in KBytes that is free for use on the referenced file system. This object returns all of the 64 bit unsigned integer."));

        String desc = "The number of NFS read calls with request sizes between 0-511 bytes received from this client.";
        assertEquals(false, trait(desc));
        desc = "The state of this client.";
        assertEquals(true, trait(desc));
        desc = "The Version of this client.";
        assertEquals(true, trait(desc));
        desc = "The number of bytes read.";
        assertEquals(false, trait(desc));
    }

}
