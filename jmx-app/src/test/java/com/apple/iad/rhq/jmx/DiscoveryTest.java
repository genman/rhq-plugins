package com.apple.iad.rhq.jmx;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

@Test
public class DiscoveryTest extends ComponentTest {

    public void test() throws Exception {
        ResourceType resourceType = getResourceType("JMX Server");
        Configuration config = getConfiguration(resourceType);
        config.setSimpleValue("type", "org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor");
        config.setSimpleValue("connectorAddress", "vm");
        manuallyAdd(resourceType, config);
        ResourceComponent c = getComponent("JLang");
        assertUp(c);
    }

}
