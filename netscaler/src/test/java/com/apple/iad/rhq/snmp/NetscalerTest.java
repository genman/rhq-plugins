package com.apple.iad.rhq.snmp;

import java.io.IOException;
import java.util.Map.Entry;

import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

@Test
public class NetscalerTest extends ComponentTest {

    private final String name = "SNMP Component";
    private final String netscalerName  = "Netscaler";
    private final String netscalerIP  = "Netscaler IP";

    public void test() throws Exception {
        ResourceType snmpType = resourceTypes.get(name);
        Configuration configuration = getConfiguration(snmpType);
        set(configuration, "version", "0"); // version 2 actually
        set(configuration, "securityName", "iad-snmp");
        set(configuration, "transportAddress", "vp25q03ad-netscaler001.iad.apple.com");
        set(configuration, "timeout", "5000");
        ResourceComponent snmp = manuallyAdd(snmpType, configuration);
        assertUp(snmp);
        ResourceComponent rc = getComponent(netscalerName);
        assertUp(rc);

        assertUp(this.getComponent(netscalerIP));

        for (Entry<ResourceComponent, Resource> entry : components.entrySet()) {
            ResourceComponent component = entry.getKey();
            assertUp(component);
            if (!(component instanceof MeasurementFacet))
                continue;
            try {
                MeasurementReport report = getMeasurementReport(component);
                log.info("got numeric " + report.getNumericData());
                log.info("got traits  " + report.getTraitData());
                Resource resource = entry.getValue();
                String typeName = resource.getResourceType().getName();
                ResourceDescriptor rd = getResourceDescriptor(typeName);
                assertAll(report, rd);
            } catch (IOException e) {
                log.error(component + " can't get measurements", e);
            }
        }
    }

}
