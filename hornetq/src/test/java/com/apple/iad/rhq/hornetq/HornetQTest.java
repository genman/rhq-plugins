package com.apple.iad.rhq.hornetq;

import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

public class HornetQTest extends ComponentTest {

    private final String hornet = "HornetQ";
    // Use SSH port forwarding if you want to test remotely...
    private final int port = 8016;
    private final String url = "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi";

    @Override
    protected void before() throws Exception {
        log.info("before");
        super.before();
        manuallyAdd(hornet);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(hornet)) {
            configuration.getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY).
                setStringValue(url);
        }
    }

    @Test
    public void version() throws Exception {
        log.info("version");
        Resource r = getResource(hornet);
        // TODO JMX plugin doesn't work with manual add
        // assertEquals("1.10", r.getVersion());
    }

    @Test
    public void measurements() throws Exception {
        log.info("measurements");
        for (Resource r : this.components.values()) {
            log.info("resources " + r.getName());
        }
        String types[] = { "Core", "JMS", "ConnectionFactory", "Acceptor", "Address", "Queue" };
        for (String type : types) {
            ResourceComponent<?> rc = getComponent(type);
            ResourceDescriptor rd = getResourceDescriptor(type);
            if (rc instanceof MeasurementFacet) {
                MeasurementReport report = getMeasurementReport(rc);
                log.info("testing " + type);
                log.info("numeric " + report.getNumericData());
                log.info("trait   " + report.getTraitData());
                try {
                    assertAll(report, rd);
                } catch (AssertionError e) {
                    log.error("cannot find", e);
                }
            }
        }
    }

}
