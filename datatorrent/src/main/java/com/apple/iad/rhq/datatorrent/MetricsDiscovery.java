package com.apple.iad.rhq.datatorrent;

import java.net.URL;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.apple.iad.rhq.http.HttpDiscovery;

public class MetricsDiscovery extends HttpDiscovery {

    @Override
    protected String getResourceName(ResourceDiscoveryContext context, URL url) {
        return "DT Metrics";
    }

}
