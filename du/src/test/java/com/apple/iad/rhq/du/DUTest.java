package com.apple.iad.rhq.du;

import static org.testng.AssertJUnit.fail;

import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

public class DUTest extends ComponentTest {

    private final String du = "du";
    private ResourceComponent rc;

    @Override
    protected void before() throws Exception {
        log.info("before");
        super.before();
        rc = manuallyAdd(du);
        log.info("added " + rc);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
    }

    @Test
    public void measurements() throws Exception {
        log.info("measurements");
        assertUp(rc);
        ResourceDescriptor rd = getResourceDescriptor(du);
        MeasurementReport report = getMeasurementReport(rc);
        log.info("testing " + du);
        log.info("numeric " + report.getNumericData());
        assertAll(report, rd);
        assertUp(rc);

        ResourceType dut = getResourceType(du);
        Configuration config = getConfiguration(dut);
        config.setSimpleValue("dir", System.getProperty("java.home"));
        config.setSimpleValue("args", "--bad-arg");
        rc = manuallyAdd(dut, config);
        assertUp(rc);
        try {
            report = getMeasurementReport(rc);
            fail("output issue");
        } catch (Exception e) {}

        ((DUComponent)rc).setDir("/invalid_dir_1231232131");
        assertDown(rc);
    }

}
