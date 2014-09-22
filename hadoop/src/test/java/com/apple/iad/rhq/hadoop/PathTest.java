package com.apple.iad.rhq.hadoop;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

@Test
public class PathTest extends ComponentTest {

    public void test() throws Exception {
        ResourceType type = getResourceType("PathInfo");
        Configuration configuration = getConfiguration(type);
        configuration.setSimpleValue("url", "file:/var/lib/postfix/");
        configuration.setSimpleValue("depth", "10");
        ResourceComponent rc = manuallyAdd(type, configuration);
        assertUp(rc);
        MeasurementReport mr = getMeasurementReport(rc);
        assertAll(mr, getResourceDescriptor(type));
        log.info(" report " + map(mr));
    }
}
