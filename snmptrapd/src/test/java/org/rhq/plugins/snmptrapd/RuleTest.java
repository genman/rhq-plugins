package org.rhq.plugins.snmptrapd;

import java.util.regex.Pattern;
import static org.rhq.core.domain.event.EventSeverity.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.testng.annotations.Test;

@Test
public class RuleTest {

    private static final Log log = LogFactory.getLog(RuleTest.class);

    OID oid1 = new OID("1.2.1");
    OID oid2 = new OID("1.2.2");
    OID oid3 = new OID("1.2.3");
    OID oid4 = new OID("1.2.4");

    public void testV1() {
        log.debug("testV1");
        // link down
        Rule r = new Rule(3, 0, new OID(), new OID(), new OID(), Pattern.compile(""), FATAL);
        PDUv1 pdu = new PDUv1();
        pdu.setEnterprise(oid1);
        pdu.setGenericTrap(3);
        assert r.match(pdu) == FATAL;
        pdu.setGenericTrap(1);
        assert r.match(pdu) == null;

        r = new Rule(Rule.UNSET, 0, oid2, new OID(), new OID(), Pattern.compile(""), WARN);
        assert r.match(pdu) == null;
        pdu.setEnterprise(oid2);
        assert r.match(pdu) == WARN;

        r = new Rule(Rule.UNSET, 0, new OID(), new OID(), oid3, Pattern.compile("foo.*bar"), INFO);
        assert r.match(pdu) == null;
        pdu.add(new VariableBinding(oid3, new OctetString("foodbar")));
        assert r.match(pdu) == INFO;
        pdu.clear();
        pdu.add(new VariableBinding(oid3, new OctetString("fobar")));
        assert r.match(pdu) == null;
    }

    public void testV2() {
        log.debug("testV2");
        // link down
        Rule r = new Rule(3, 0, new OID(), new OID(), new OID(), Pattern.compile(""), FATAL);
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, SnmpConstants.linkUp));
        assert r.match(pdu) == null;
        pdu.clear();

        pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, SnmpConstants.linkDown));
        assert r.match(pdu) == FATAL;
    }

}
