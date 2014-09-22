package com.apple.iad.rhq.datatorrent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovers {@link ContainerComponent}.
 */
public class ContainerDiscovery implements ResourceDiscoveryComponent<AppComponent> {

    private static Pattern KEY = Pattern.compile("\\d+_\\d+$");

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<AppComponent> rdc)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        AppComponent ac = rdc.getParentResourceComponent();
        for (Map<String, Object> app : ac.getContainerData()) {
            String id = app.get("id").toString();
            Matcher m = KEY.matcher(id);
            if (!m.find()) {
                throw new IllegalStateException(KEY + " no match " + id);
            }
            String name = "c_" + m.group();
            String key = m.group();
            String ver = null;
            Configuration config = rdc.getDefaultPluginConfiguration();
            DiscoveredResourceDetails drd = new DiscoveredResourceDetails(
                    rdc.getResourceType(), key, name, ver, "DT Container " + key,
                    config, null);
            set.add(drd);
        }
        return set;

    }

}
