package com.apple.iad.rhq.snmp;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import net.percederberg.mibble.MibLoaderException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class MibIndexTest {

    private Log log = LogFactory.getLog(getClass());

    public final static String SNMPV2 = "SNMPv2-MIB";

    private final MibIndex index = new MibIndex();
    private static final OID up = new OID(SnmpConstants.sysUpTime);

    @BeforeClass
    void setUp() throws IOException {
        index.setExtra(true);
        index.load(SNMPV2);
    }

    public void testBasic() throws IOException, MibLoaderException {
        assertEquals(up, index.getNameRecord("sysUpTime").getOid0());
        assertEquals(up, index.toOid("sysUpTime.0"));
        assertEquals("sysUpTime", index.toName(new OID("1.3.6.1.2.1.1.3")));
        assertEquals("sysUpTime.0", index.toName(new OID("1.3.6.1.2.1.1.3.0")));
        assertEquals("sysUpTime.1.2.3", index.toName(new OID("1.3.6.1.2.1.1.3.1.2.3")));
        assertEquals("1.2.3", index.toName(new OID("1.2.3")));
        String s = "1.3.6.1.4.1.18016.2.1.7";
        assertEquals(new OID(s), index.toOid(s));
        up.trim(1);
        assertEquals("sysUpTime", index.getName(up));
        assertEquals(true, index.getNameRecord("sysUpTime").getDesc().contains("time"));
        log.debug(index);
    }

    public void testMapping() {
        Map<Integer, String> map = index.getMapping("snmpEnableAuthenTraps");
        assertEquals(2, map.size());
        assertEquals("enabled", map.get(1));
        assertEquals("disabled", map.get(2));

        map = index.getMapping("snmpXYZ");
        assertEquals(null, map);
    }

    public void testTables() {
        log.debug("testTables");
        assertTrue(index.getTables().containsKey("sysORTable"));
        TableRecord tr = index.getTableRecord("sysORTable");
        assertEquals(true, tr.getColumns().contains("sysORID"));
        OID oid = index.toOid("sysORUpTime");
        assertEquals(true, Arrays.asList(tr.getOids()).contains(oid));
    }

    public void testPaths() throws IOException {
        URL url = getClass().getResource("/test.mib");
        index.load(url.toString());
        String file = url.getFile();
        index.load(file);
    }

}
