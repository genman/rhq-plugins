package com.apple.iad.rhq.dns;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.xbill.DNS.ResolverConfig;

/**
 * DNS lookup discovery.
 */
public class DNSDiscovery implements ResourceDiscoveryComponent {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException, Exception {
        
        ResolverConfig conf = new ResolverConfig();
        
        Set<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();
        for (String server: conf.servers()) {
            String name = "DNS server " + server;
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                    context.getResourceType(),
                    server, // database key
                    name, // UI name
                    null, // Version
                    name,
                    context.getDefaultPluginConfiguration(),
                    null); // process info
            set.add(detail); 
        }
        return set;
    }

}
