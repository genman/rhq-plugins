package com.apple.iad.rhq.snmp;

import static org.testng.AssertJUnit.assertTrue;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

@Test
public class SnmpDiscoveryTest extends ComponentTest {

    private final String name = "SNMP Component";
    private int port = 12313;
    private TestAgent agent;

    @Override
    protected void before() throws Exception {
        new SnmpComponent(); // init logging
        agent = new TestAgent(port);
        agent.start();
        super.before();
    }

    @Override
    protected void after() throws Exception {
        agent.stop();
    }

    public void testBasics() throws Exception {
        log.info("testBasics");
        SnmpComponent component = (SnmpComponent) getComponent(name);
        assertUp(component);
    }

    public void testThreadLeak() throws Exception {
        log.info("testThreadLeak");
        int ac = Thread.activeCount();
        for (int i = 0; i < 100; i++) {
            log.info("thread active count " + Thread.activeCount());
            try {
                ResourceComponent rc = manuallyAdd(name);
                assertUp(rc);
                getMeasurementReport(rc);
                rc.stop();
            } catch (InvalidPluginConfigurationException e) {
                log.warn("no response, seems to happen sometimes?", e);
            }
        }
        log.info("test discovery failure not leaking...");
        for (int i = 0; i < 10; i++) {
            agent.stop();
            try {
                ResourceComponent rc = manuallyAdd(name);
                rc.stop();
            } catch (InvalidPluginConfigurationException e) {
                assert e.getCause() instanceof NoResponseException;
            }
        }
        int ac2 = Thread.activeCount();
        assertTrue("active " + ac + " not too many over " + ac2, ac > ac2 - 5);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(name)) {
            log.info("setConfiguration " + port);
            Configuration c = resourceType.getPluginConfigurationDefinition().getDefaultTemplate().
                getConfiguration();
            c.setSimpleValue("transportAddress", "localhost/" + port);
            c.setSimpleValue("timeout", "100");
        }
    }

}
