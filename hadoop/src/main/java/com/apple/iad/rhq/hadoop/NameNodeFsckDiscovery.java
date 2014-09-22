package com.apple.iad.rhq.hadoop;

import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import static com.apple.iad.rhq.hadoop.NameNodeFsckComponent.*;

/**
 * Manual discovery.
 */
public class NameNodeFsckDiscovery implements ResourceDiscoveryComponent<NameNodeComponent>, ManualAddFacet<NameNodeComponent> {

    /**
     * Manual add facet.
     */
    @Override
    public DiscoveredResourceDetails discoverResource(
            Configuration conf,
            ResourceDiscoveryContext<NameNodeComponent> context)
            throws InvalidPluginConfigurationException
    {
        String path = conf.getSimpleValue(PATH, "/");
        String name = "NameNode FSCK " + path;
        String key = path;
        String description = name;
        ResourceType resourceType = context.getResourceType();
        DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceType, key, name, null, description,
            conf, null);
        return details;
    }

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<NameNodeComponent> context)
            throws InvalidPluginConfigurationException, Exception {
        return Collections.emptySet();
    }

}
