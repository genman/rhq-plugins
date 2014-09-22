package com.apple.iad.rhq.snmp;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

/**
 * Tests the indexing of an Oracle TimesTen trap MIB.
 */
public class TimesTenTest {

    private final Log log = LogFactory.getLog(getClass());

    private static final String TIMES_TEN_MIB_TXT = "/TimesTen-MIB.txt";

    @Test
    public void test() throws IOException {
        URL url = getClass().getResource(TIMES_TEN_MIB_TXT);
        MibIndex index = MibIndexCache.getIndex();
        index.load(url.toString());
        for (Entry<String, NameRecord> e : index.getNames().entrySet()) {
            log.debug(e);
        }
        assertEquals("1.3.6.1.4.1.5549.5.1", index.getNameRecord("ttAssertFailTrap").getOid().toString());
    }

}
