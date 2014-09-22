package com.apple.iad.rhq.flume;

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

import static org.testng.AssertJUnit.*;

public class FlumeTest extends ComponentTest {

    private String flume = "Flume";
    // Use SSH port forwarding if you want to test remotely...
    private int port = 8013;
    private String url = "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi";

    @Override
    protected void before() throws Exception {
        log.info("before");
        super.before();
        manuallyAdd(flume);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(flume)) {
            configuration.getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY).
                setStringValue(url);
        }
    }

    @Test
    public void version() throws Exception {
        log.info("version");
        Resource r = getResource(flume);
        assertEquals("1.10", r.getVersion());
    }

    @Test
    public void measurements() throws Exception {
        log.info("measurements");
        String types[] = { "Flume Channel", "Flume Sink", "Flume Source", "Flume Sink Group" };
        double sum = 0;
        for (String type : types) {
            ResourceComponent<?> rc = getComponent(type);
            ResourceDescriptor rd = getResourceDescriptor(type);
            if (rc instanceof MeasurementFacet) {
                MeasurementReport report = getMeasurementReport(rc);
                log.info("testing " + type);
                log.info("numeric " + report.getNumericData());
                log.info("trait   " + report.getTraitData());
                if (type.equals(types[1])) { // sink
                    sum += (Double)map(report).get("EventDrainSuccessCount");
                }
                if (type.equals(types[3])) { // group sink
                    double total = (Double)map(report).get("EventDrainSuccessCount");
                    log.info("sum " + sum + " total " + total);
                    assertEquals(total, sum, 100);
                }
                try {
                    assertAll(report, rd);
                } catch (AssertionError e) {
                    log.error("cannot find", e);
                }
            }
        }
    }

}
