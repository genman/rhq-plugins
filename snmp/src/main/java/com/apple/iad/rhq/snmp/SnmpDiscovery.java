package com.apple.iad.rhq.snmp;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.snmp4j.mp.SnmpConstants;

/**
 * SNMP component discovery, supporting auto-discovery and manual discovery.
 * Auto-discovery expects the default configuration to find a running service
 * on the default host and port.
 */
public class SnmpDiscovery
    implements ResourceDiscoveryComponent<ResourceComponent<?>>, ManualAddFacet<ResourceComponent<?>>
{

    private final Log log = LogFactory.getLog(getClass());

    private static final Pattern VERSION_PAT = Pattern.compile("(\\d+\\.[.\\d]+)\\b");

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ResourceComponent<?>> rdc) {
        Configuration conf = rdc.getDefaultPluginConfiguration();
        log.debug("trying to discover SNMP resource");
        try {
            DiscoveredResourceDetails drd = discoverResource(conf, rdc);
            return Collections.singleton(drd);
        } catch (InvalidPluginConfigurationException e) {
            log.debug("failed discovery", e);
            return Collections.emptySet();
        }
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration conf,
            ResourceDiscoveryContext<ResourceComponent<?>> rdc)
            throws InvalidPluginConfigurationException
    {
        log.debug("discovery " + conf.getMap());
        String name;
        String desc;
        try {
            log.debug("trying to connect and get name and desc");
            SnmpComponent component = new SnmpComponent(conf);
            try {
                name = component.get(SnmpConstants.sysName).toString();
                desc = component.get(SnmpConstants.sysDescr).toString();
            } finally {
                component.stop();
            }
        } catch (IOException e) {
            throw new InvalidPluginConfigurationException(e);
        }

        log.debug("attempt to find a version from desc: " + desc);
        String version = version(desc);
        String transportAddress = conf.getSimpleValue("transportAddress", null);
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                rdc.getResourceType(), // ResourceType
                transportAddress,
                name + "@" + transportAddress,
                version, // Version
                desc,
                conf,
                null // process information
        );

        return detail;
    }

    /**
     * Extracts a version from a description.
     */
    static String version(String desc) {
        Matcher matcher = VERSION_PAT.matcher(desc);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

}
