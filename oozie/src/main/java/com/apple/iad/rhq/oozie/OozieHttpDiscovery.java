package com.apple.iad.rhq.oozie;

import java.net.URL;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.apple.iad.rhq.http.HttpDiscovery;

/**
 * Base class for Oozie HTTP components.
 */
public class OozieHttpDiscovery extends HttpDiscovery {

    @Override
    protected int getDiscoveryTimeout() {
        return 1000 * 30;
    }

    @Override
    protected String getResourceName(ResourceDiscoveryContext context, URL url) {
        return context.getResourceType().getName();
    }

}