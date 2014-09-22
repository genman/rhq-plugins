package com.apple.iad.rhq.oozie;

import java.net.URL;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;


/**
 * Discovers the jobs page.
 */
public class JobsDiscovery extends OozieHttpDiscovery {

    /**
     * Fixed resource key.
     */
    @Override
    protected String getResourceKey(ResourceDiscoveryContext context, URL url) {
        return "jobs";
    }

}
