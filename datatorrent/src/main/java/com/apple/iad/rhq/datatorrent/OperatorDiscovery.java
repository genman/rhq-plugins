package com.apple.iad.rhq.datatorrent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovers {@link ContainerComponent}.
 */
public class OperatorDiscovery implements ResourceDiscoveryComponent<AppComponent> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<AppComponent> rdc)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        AppComponent ac = rdc.getParentResourceComponent();

        // track name -> id
        Map<String, Integer> counts = new HashMap<String, Integer>();

        for (Map<String, Object> app : ac.getOperatorData()) {
            String name = app.get("name").toString();
            Integer i = counts.get(name);
            if (i == null) {
                i = 0;
            }
            String key = name + "|" + i;
            counts.put(name, i + 1);
            String ver = null;
            Configuration config = rdc.getDefaultPluginConfiguration();
            DiscoveredResourceDetails drd = new DiscoveredResourceDetails(
                    rdc.getResourceType(), key, name, ver, "DataTorrent Operator " + name,
                    config, null);
            set.add(drd);
        }
        return set;

    }

}
