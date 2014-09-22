package com.apple.iad.rhq.mongodb;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.mongodb.DB;
import com.mongodb.MongoClient;

/**
 * Discovers a single mongodb database instance.
 */
public class MongoDBDiscovery implements ResourceDiscoveryComponent<MongoDBServerComponent> {

    private static final Log log = LogFactory.getLog(MongoDBDiscovery.class);

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<MongoDBServerComponent> context)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        MongoClient client = context.getParentResourceComponent().getClient();
        for (String name : client.getDatabaseNames()) {
            details.add(discover(context, client, name));
        }
        return details;
    }

    private DiscoveredResourceDetails discover(ResourceDiscoveryContext context, MongoClient client, String name) {
        DB db = client.getDB(name);
        db.getStats();

        String rname = "mongodb " + name;
        log.debug("discovered " + rname);
        Configuration pluginConfiguration = context.getDefaultPluginConfiguration();
        pluginConfiguration.put(new PropertySimple(MongoDBComponent.DB, name));
        String ver = null;
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                context.getResourceType(),
                name,  // database key
                rname, // UI name
                ver,
                "MongoDB " + name,
                pluginConfiguration,
                null); // process info

        return detail;
    }

}
