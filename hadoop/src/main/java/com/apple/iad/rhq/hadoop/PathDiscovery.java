package com.apple.iad.rhq.hadoop;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.apple.iad.rhq.hadoop.HadoopClassLoaderFacet;

/**
 * Manual discovery.
 */
public class PathDiscovery implements ResourceDiscoveryComponent, ManualAddFacet, ClassLoaderFacet {

    /**
     * Manual add facet.
     */
    @Override
    public DiscoveredResourceDetails discoverResource(
            Configuration conf,
            ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException
    {
        String url = conf.getSimpleValue("url");
        if (url == null)
            throw new InvalidPluginConfigurationException("url");
        String name = "PathInfo " + url;
        String key = url;
        String description = name;
        ResourceType resourceType = context.getResourceType();
        DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceType, key, name, null, description,
            conf, null);
        return details;
    }

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException, Exception {
        return Collections.emptySet();
    }

    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext rdc, DiscoveredResourceDetails drd)
            throws Exception
    {
        return new HadoopClassLoaderFacet().getAdditionalClasspathUrls(rdc, drd);
    }

}
