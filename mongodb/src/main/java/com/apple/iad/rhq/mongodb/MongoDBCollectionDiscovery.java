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
import com.mongodb.DBCollection;

/**
 * Discovers a single mongodb database collection instance.
 */
public class MongoDBCollectionDiscovery implements ResourceDiscoveryComponent<MongoDBComponent> {

    private static final Log log = LogFactory.getLog(MongoDBCollectionDiscovery.class);

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<MongoDBComponent> context)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        MongoDBComponent component = context.getParentResourceComponent();
        if (component.isMaster()) {
            DB db = component.getDB();
            for (String name : db.getCollectionNames()) {
                details.add(discover(context, db, name));
            }
        }
        return details;
    }

    private DiscoveredResourceDetails discover(ResourceDiscoveryContext context, DB db, String name) {
        DBCollection collection = db.getCollection(name);
        collection.getStats();

        String rname = "mongodb " + db.getName() + "." + name;
        log.debug("discovered mongodb collection " + rname);
        Configuration pluginConfiguration = context.getDefaultPluginConfiguration();
        pluginConfiguration.put(new PropertySimple(MongoDBCollectionComponent.COLLECTION, name));
        String ver = null;
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                context.getResourceType(),
                name, // database key
                rname, // UI name
                ver,
                "MongoDB Collection " + name,
                pluginConfiguration,
                null); // process info

        return detail;
    }

}
