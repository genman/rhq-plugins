package com.apple.iad.rhq.netstat;

import java.util.Map;

import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

public class NetstatTest extends ComponentTest {

    String protocol = "Netstat Protocol Data";
    private String dir;

    @Override
    protected void before() throws Exception {
        dir = System.getProperty("user.dir");
        log.info("dir " + dir);
        log.info("before");
        super.before();
        manuallyAdd(protocol);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(protocol)) {
            ConfigurationDefinition configurationDefinition = resourceType.getPluginConfigurationDefinition();
            configurationDefinition.getDefaultTemplate().getConfiguration().
                getSimple("executable").setStringValue(dir + "/src/test/script/netstat");
        }
    }

    @Test
    public void measurements() throws Exception {
        log.info("measurements");
        ResourceComponent<?> rc = getComponent(protocol);
        ResourceDescriptor rd = getResourceDescriptor(protocol);
        MeasurementReport report = getMeasurementReport(rc);
        log.info("testing " + protocol);
        log.info("numeric " + report.getNumericData());
        log.info("trait   " + report.getTraitData());
        assertAll(report, rd);
    }

}
