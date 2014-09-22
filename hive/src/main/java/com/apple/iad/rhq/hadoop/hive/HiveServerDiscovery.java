package com.apple.iad.rhq.hadoop.hive;

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

public class HiveServerDiscovery implements ResourceDiscoveryComponent, ManualAddFacet, ClassLoaderFacet
{

    /**
     * Auto discovery.
     */
    @Override
    public Set discoverResources(ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException, Exception {
        return Collections.emptySet();
    }

    /**
     * Manual discovery
     */
    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
                  ResourceDiscoveryContext ctx) throws InvalidPluginConfigurationException {

        String url = pluginConfiguration.getSimpleValue(HiveServerComponent.JDBC_URL, null);
        url = HiveServerComponent.scrub(url);
        String key = "Hive Server " + url;
        String name = key;
        String description = "Hive Server connecting to " + url;
        ResourceType resourceType = ctx.getResourceType();
        DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceType, key, name, null, description,
            pluginConfiguration, null);

        return details;
    }

    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext rdc, DiscoveredResourceDetails drd)
            throws Exception
    {
        return new HadoopClassLoaderFacet().getAdditionalClasspathUrls(rdc, drd);
    }

}
