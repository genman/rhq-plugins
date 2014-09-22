package com.apple.iad.rhq.snmp;

import static com.apple.iad.rhq.snmp.SetOperation.ERROR_INDEX;
import static com.apple.iad.rhq.snmp.SetOperation.ERROR_STATUS;
import static com.apple.iad.rhq.snmp.SetOperation.ERROR_STATUS_TEXT;
import static com.apple.iad.rhq.snmp.SetOperation.FORMAT;
import static com.apple.iad.rhq.snmp.SetOperation.OID;
import static com.apple.iad.rhq.snmp.SetOperation.VALUE;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

@Test
public class SnmpComponentTest extends ComponentTest {

    static {
        new SnmpComponent(); // init logging
    }
    private final String name = "SNMP Component";
    private int port = 12312;
    private TestAgent agent;
    private SnmpComponent component;

    @Override
    protected void before() throws Exception {
        super.before();
        agent = new TestAgent(port);
        agent.start();
        log.info("*** manually add " + name);
        component = (SnmpComponent)manuallyAdd(name);
        log.info("*** component " + component);
    }

    @Override
    protected void afterClass() throws Exception {
        super.afterClass();
        if (agent != null) {
            agent.stop();
            assertDown(component);
        }
    }

    public void testVerParse() throws Exception {
        String ver = "NetScaler NS9.2: Build 48.6.nc, Date: Sep 23 2010, 09:54:40";
        ver = SnmpDiscovery.version(ver);
        System.err.println(ver);
        assert ver.equals("9.2");
    }

    public void testBasics() throws Exception {
        assertUp(component);

        MeasurementReport report = getMeasurementReport(component);
        ResourceDescriptor rd = getResourceDescriptor(name);
        assertAll(report, rd);

        Variable v = component.get(SnmpConstants.sysUpTime);
        assert v.toLong() > 0;

        Resource resource = getResource(component);
        assertEquals("TestAgent@localhost/" + port, resource.getName());
        assertEquals(TestAgent.DESCRIPTION, resource.getDescription());
        assertEquals("1.0", resource.getVersion());
    }

    public void testSetOperation() throws Exception {
        log.info("testSetOperation");

        OID oid = new OID(TestAgent.sysOID).append(new OID("1.1.0"));
        agent.register(oid);

        OperationResult response;
        Configuration c2;

        OperationFacet of = ((OperationFacet)component);
        Configuration c = new Configuration();
        String msg = "hello";
        c.setSimpleValue(OID, "sysDescr");
        c.setSimpleValue(FORMAT, "OctetString");
        c.setSimpleValue(VALUE, msg);
        response = set(of, c);
        assertNotNull(response.getErrorMessage());
        c2 = response.getComplexResults();
        assertEquals("1", c2.getSimpleValue(ERROR_INDEX));
        assertEquals("17", c2.getSimpleValue(ERROR_STATUS));
        assertEquals("Not writable", c2.getSimpleValue(ERROR_STATUS_TEXT));

        c.setSimpleValue(OID, oid.toString());
        response = set(of, c);
        assertEquals(null, response.getErrorMessage());
        c2 = response.getComplexResults();
        assertEquals("0", c2.getSimpleValue(ERROR_INDEX));
        assertEquals("0", c2.getSimpleValue(ERROR_STATUS));
        assertEquals("Success", c2.getSimpleValue(ERROR_STATUS_TEXT));
        assertEquals(msg, component.get(oid).toString());

        c.setSimpleValue(FORMAT, "Counter32");
        c.setSimpleValue(VALUE, "1234");
        c2 = set(of, c).getComplexResults();
        assertEquals("1", c2.getSimpleValue(ERROR_INDEX));
        assertEquals("7", c2.getSimpleValue(ERROR_STATUS));
        assertEquals("Wrong type", c2.getSimpleValue(ERROR_STATUS_TEXT));

        try {
            c.setSimpleValue(VALUE, "not a number");
            c2 = set(of, c).getComplexResults();
            fail("bad variable");
        } catch (NumberFormatException e) {
            log.debug("expected " + e);
        }

        log.debug("byte array");
        c.setSimpleValue(FORMAT, "OctetString");
        c.setSimpleValue(VALUE, "0x4455");
        c2 = set(of, c).getComplexResults();
        assertEquals("0", c2.getSimpleValue(ERROR_INDEX));

        log.debug("GET format");
        c.setSimpleValue(FORMAT, "GET");
        c2 = set(of, c).getComplexResults();
        assertEquals("0", c2.getSimpleValue(ERROR_INDEX));
        assertEquals("DU", component.get(oid).toString());

        log.debug("null format");
        c.setSimpleValue(FORMAT, null);
        assertEquals(null, set(of, c).getErrorMessage());
    }

    private OperationResult set(OperationFacet of, Configuration c) throws Exception {
        return of.invokeOperation("set", c);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(name)) {
            log.info("setConfiguration " + port);
            set(configuration, "transportAddress", "localhost/" + port);
        }
    }


}
