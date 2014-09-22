package com.apple.iad.rhq.du;

import static java.util.Collections.emptySet;

import java.io.File;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovery for adding {@link DUComponent} manually.
 */
public class DUDiscovery implements ManualAddFacet<ResourceComponent<?>>, ResourceDiscoveryComponent<ResourceComponent<?>> {

    /**
     * Returns a new component keyed from directory.
     */
    @Override
    public DiscoveredResourceDetails discoverResource(
            Configuration configuration,
            ResourceDiscoveryContext<ResourceComponent<?>> context)
            throws InvalidPluginConfigurationException
    {
        String dir = configuration.getSimpleValue(DUComponent.DIR);
        File f = new File(dir);
        if (!f.isAbsolute())
            throw new InvalidPluginConfigurationException("not absolute " + dir);
        if (!f.canRead())
            throw new InvalidPluginConfigurationException("cannot read " + dir);
        String ver = "";
        String name = context.getResourceType().getName() + " " + dir;
        DiscoveredResourceDetails server = new DiscoveredResourceDetails(context.getResourceType(),
                dir, // key
                name,
                ver,
                context.getResourceType().getDescription(),
                configuration,
                null);
        return server;
    }

    /**
     * Returns empty set.
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<ResourceComponent<?>> context)
            throws InvalidPluginConfigurationException, Exception {
        return emptySet();
    }

}
