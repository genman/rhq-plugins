package com.apple.iad.rhq.verdad;

import static org.testng.AssertJUnit.assertEquals;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

/**
 * Runs tests for Verdad.
 */
public class VerdadTest extends ComponentTest {

    private final String name = "Verdad";

    @Test
    public void testDown() throws Exception {
        log.info("cannot load verdad");
        VerdadComponent vd = (VerdadComponent)getComponent(name);
        assertDown(vd);
        assertEquals(0, vd.observerCount());
        restart(vd);
        Resource resource = getResource(name);
        assertEquals(null, resource.getVersion());
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(name)) {
            ConfigurationDefinition def = resourceType.getPluginConfigurationDefinition();
            Configuration conf = def.getDefaultTemplate().getConfiguration();
            conf.getSimple(VerdadComponent.EXECUTABLE).setStringValue("/usr/bin/false");
        }
    }

}
