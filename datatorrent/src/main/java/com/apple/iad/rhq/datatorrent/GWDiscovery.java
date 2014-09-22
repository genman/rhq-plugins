package com.apple.iad.rhq.datatorrent;

import java.io.IOException;
import java.net.URL;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.apple.iad.rhq.http.HttpComponent;
import com.apple.iad.rhq.http.HttpDiscovery;

/**
 * Discovers {@link GWComponent}.
 */
public class GWDiscovery extends HttpDiscovery {

    @Override
    protected String getResourceName(ResourceDiscoveryContext context, URL url) {
        return "DataTorrent " + url.getHost();
    }

    @Override
    protected HttpComponent createHttpComponent(Configuration configuration, ResourceDiscoveryContext context) throws IOException {
        return new GWComponent(configuration, context);
    }

}
