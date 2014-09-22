package com.apple.iad.rhq.dns;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

/**
 * Runs DNS plugin test.
 */
@Test
public class DNSTest extends ComponentTest {

    @Override
    protected void before() throws Exception {
        super.before();
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        configuration.setSimpleValue(DNSComponent.HOST, "apple.com");
    }

    public void test() throws Exception {
        for (ResourceComponent r : components.keySet()) {
            assertUp(r);
            MeasurementReport mr = getMeasurementReport(r);
            log.info(map(mr));
        }
    }
}
