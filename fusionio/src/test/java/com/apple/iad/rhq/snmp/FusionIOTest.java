package com.apple.iad.rhq.snmp;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

@Test
public class FusionIOTest extends ComponentTest {

    private final String name = "SNMP Component";
    private final String resourceName  = "Fusion IO";

    public void test() throws Exception {
        ResourceComponent snmp = manuallyAdd(name);
        assertUp(snmp);
        ResourceType resourceType = resourceTypes.get(resourceName);
        Configuration configuration = resourceType.getPluginConfigurationDefinition().getDefaultTemplate().createConfiguration();
        ResourceComponent rc = manuallyAdd(resourceType, configuration, snmp);
        assertUp(rc);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(name)) {
            // TODO: Fusion IO in the Lab?
            // set(configuration, "transportAddress", "vg61l01ad-netscaler001/161");
        }
    }

}
