package org.rhq.plugins.snmptrapd;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.testng.annotations.Test;

import com.apple.iad.rhq.snmp.MibComponent;
import com.apple.iad.rhq.testing.ComponentTest;
import com.apple.iad.rhq.testing.TestEventContext;


public class SnmpTrapdComponentTest extends ComponentTest {

    private static final String TYPE = "SnmpTrapd";
    private Snmp snmp;
    private TransportMapping peer;
    private InetAddress address;
    private int port = 1234;
    public static final OctetString community = new OctetString("public");

    static final OID alertName = oid("1.3.6.1.4.1.18016.2.1.1");
    // private static final OID alertResourceName = oid("1.3.6.1.4.1.18016.2.1.2");
    // private static final OID alertPlatformName = oid("1.3.6.1.4.1.18016.2.1.3");
    private static final OID alertSeverity = oid("1.3.6.1.4.1.18016.2.1.5");
    // private static final OID alertUrl = oid("1.3.6.1.4.1.18016.2.1.6");

    /**
     * Alert source OID, for testing only, not part of the MIB.
     */
    private static final OID alertSource = oid("1.3.6.1.4.1.18016.2.1.7");

    private static OID oid(String string) {
        return new OID(string);
    }

    public SnmpTrapdComponentTest() {
    }

    @Override
    protected void before() throws Exception {
        super.before();
        try {
            address = InetAddress.getLocalHost();
            peer = new DefaultUdpTransportMapping(); //new UdpAddress(address, getPort()));
            snmp = new Snmp(peer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDiscovery() throws Exception {
        log.info("testDiscovery");
        MibComponent mb = new MibComponent(null);
        ResourceType type = resourceTypes.get("SnmpTrapd");
        Configuration conf = new Configuration();
        port++;
        conf.setSimpleValue("port", "" + port);

        ResourceComponent c = manuallyAdd(type, conf, mb);
        SnmpTrapdDiscovery disc = new SnmpTrapdDiscovery();
        ResourceDiscoveryContext context;
        PluginContainerDeployment pluginContainerDeployment = null;
        context = new ResourceDiscoveryContext(type, mb, null, null, null, (String)null, pluginContainerDeployment);
        Set<DiscoveredResourceDetails> set = disc.discoverResources(context);
        assert set.size() == 1;
        c.stop();
    }

    @Test
    public void test() throws Exception {
        log.info("sending snmp trap...");

        ResourceType type = getResourceType(TYPE);
        Configuration conf = getConfiguration(type);
        conf.setSimpleValue("port", "" + port);
        conf.setSimpleValue("filter", "(\\d+-\\d+-\\d+\\s+)?(\\d+:\\d+:\\d+(,\\d+)?\\s*)?");
        conf.setSimpleValue(SnmpTrapdComponent.SOURCE_LOCATION, alertSource.toString());
        ResourceComponent trapd = manuallyAdd(type, conf);

        String prefix = "2013-06-03 22:23:38,858 ";
        String hello = "hello \u2013 world";
        sendTrap(prefix + hello);
        log.info("listening...");
        Event event = null;
        TestEventContext eventContext = getEventContext(trapd);
        for (int i = 0; i < 16; i++) {
            Thread.sleep(250);
            List<Event> events = eventContext.getEvents();
            log.info("events " + events);
            if (events.size() > 0) {
                event = events.get(0);
                break;
            }
        }
        assertTrue("Did not get event (in time)", event != null);
        assertEquals(EventSeverity.INFO, event.getSeverity());
        assertEquals("SnmpTrap", event.getType());
        assertEquals("alertName: " + hello + "\n" +
                "alertSeverity: medium\n" +
                alertSource + ": rhq.org\n", event.getDetail());
        assertEquals("rhq.org", event.getSourceLocation());

        trapd.stop();
    }

    enum Severity {
        high, medium, info;
    }

    protected void sendTrap(String message) throws Exception {
        PDU pdu = new PDU();
        pdu.setType(PDU.TRAP);
        add(pdu, alertName, message);
        add(pdu, alertSeverity, Severity.medium);
        add(pdu, alertSource, "rhq.org");

        CommunityTarget target = new CommunityTarget();
        target.setCommunity(community);
        target.setVersion(SnmpConstants.version2c);
        target.setAddress(new UdpAddress(address, port));
        target.setTimeout(1000);
        target.setRetries(2);

        try {
            snmp.send(pdu, target);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void add(PDU pdu, OID oid, Object message) throws Exception {
        String s = String.valueOf(message);
        oid = new OID(oid).append(0);
        byte[] bytes = s.getBytes("UTF-8");
        pdu.add(new VariableBinding(oid, new OctetString(bytes)));
    }

}
