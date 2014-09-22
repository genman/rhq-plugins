package com.apple.iad.rhq.tomcat;

import java.io.IOException;
import java.net.URL;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.apple.iad.rhq.http.HttpComponent;
import com.apple.iad.rhq.http.HttpDiscovery;

/**
 * Extracts the HTTP port value for a service running within Tomcat.
 */
public class TomcatHttpDiscovery extends HttpDiscovery {

    @Override
    protected HttpComponent createHttpComponent(Configuration configuration,
            ResourceDiscoveryContext context) throws IOException {
        return new TomcatHttpComponent(configuration, context);
    }

    @Override
    protected String getResourceName(ResourceDiscoveryContext context, URL url) {
        return context.getResourceType().getName();
    }

}
