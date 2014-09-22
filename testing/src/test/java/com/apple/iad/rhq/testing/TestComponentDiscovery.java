package com.apple.iad.rhq.testing;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovers a list of component instance.
 */
public class TestComponentDiscovery implements ResourceDiscoveryComponent {

    private final Log log = LogFactory.getLog(getClass());
    public static String name = "name";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException, Exception {

        log.debug("discovery");
        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
        
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                context.getResourceType(),
                name, // database key
                name, // UI name
                null, // Version
                name,
                context.getDefaultPluginConfiguration(),
                null); // process info

        details.add(detail);
        return details;
    }

}
