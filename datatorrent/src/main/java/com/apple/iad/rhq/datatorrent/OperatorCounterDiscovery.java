package com.apple.iad.rhq.datatorrent;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovers {@link ContainerComponent}.
 */
public class OperatorCounterDiscovery implements ResourceDiscoveryComponent<OperatorComponent> {

    /**
     * Matching operator name, key for discovery.
     */
    public static final String OPERATOR = "operator";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<OperatorComponent> rdc)
            throws InvalidPluginConfigurationException, Exception {

        OperatorComponent oc = rdc.getParentResourceComponent();
        Map<String, Object> detail = oc.getDetailTree();
        Map<String, String> counters = (Map<String, String>) detail.get(OperatorCounterComponent.COUNTERS);
        if (counters == null)
            return emptySet();

        Configuration conf = rdc.getDefaultPluginConfiguration();
        String oname = oc.getName();
        String rname = rdc.getResourceType().getName();
        String onameWant = conf.getSimpleValue(OPERATOR, "");
        if (onameWant.isEmpty()) {
            if (rname.isEmpty())
                throw new IllegalStateException();
            onameWant = rname;
        }
        if (Pattern.matches(onameWant, oname)) {
            String key = oname;
            String name = oname + " Counters";
            String ver = null;
            Configuration config = rdc.getDefaultPluginConfiguration();
            DiscoveredResourceDetails drd = new DiscoveredResourceDetails(
                    rdc.getResourceType(), key, name, ver, "",
                    config, null);
            return singleton(drd);
        } else {
            return emptySet();
        }

    }

}
