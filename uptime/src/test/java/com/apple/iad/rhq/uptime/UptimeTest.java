package com.apple.iad.rhq.uptime;

import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

public class UptimeTest extends ComponentTest {

    private String protocol = "Uptime Data";
    private ResourceComponent rc;

    @Override
    protected void before() throws Exception {
        log.info("before");
        super.before();
        rc = manuallyAdd(protocol);
        log.info("added " + protocol);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
    }

    @Test
    public void measurements() throws Exception {
        log.info("measurements");
        ResourceDescriptor rd = getResourceDescriptor(protocol);
        MeasurementReport report = getMeasurementReport(rc);
        log.info("testing " + protocol);
        log.info("numeric " + report.getNumericData());
        log.info("trait   " + report.getTraitData());
        assertAll(report, rd);
    }

}
