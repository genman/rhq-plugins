package com.apple.iad.rhq.snmp;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.testng.annotations.Test;

/**
 * Tests the test agent.
 */
@Test
public class TestAgentTest {

    private final Log log = LogFactory.getLog(getClass());

    private static final String HI = "hi";
    int port = 35324;

    public void testIt() throws Exception {
        TestAgent ta = new TestAgent(port);
        ta.start();
        Configuration c = new Configuration();
        c.setSimpleValue("transportAddress", "localhost/" + port);
        c.setSimpleValue("version", "1");
        SnmpComponent snmp = new SnmpComponent(c);

        log.info("SNMP get");
        Variable v = snmp.get(SnmpConstants.sysUpTime);

        OID oid = new OID(TestAgent.sysOID).append(new OID("1.0"));
        ta.register(oid);

        VariableBinding vb = new VariableBinding(oid, new OctetString(HI));
        log.info("SNMP set " + oid);
        snmp.set(vb);

        log.info("SNMP get " + oid);
        v = snmp.get(oid);
        assertEquals(HI, v.toString());

        log.info("SNMP stop");
        snmp.stop();
        ta.stop();
    }

    /**
     * Quick test of an SNMPv1 resource.
     * @param arg first arg is transport, second arg is community name
     */
    @Test(enabled=false)
    public static void main(String arg[]) throws IOException {
        Configuration c = new Configuration();
        c.setSimpleValue("transportAddress", arg[0]);
        c.setSimpleValue("version", "1");
        c.setSimpleValue("securityName", arg[1]);
        SnmpComponent snmp = new SnmpComponent(c);
        Variable v = snmp.get(SnmpConstants.sysUpTime);
        System.out.println("SNMP get " + v);
    }

}
