package com.apple.iad.rhq.hadoop.hive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovers a list of Hadoop Hive tables matching a query and regular expression.
 * Uses (MySQL) JDBC to access the metadata, not the Hive JDBC driver.
 */
public class HiveTableDiscovery implements ResourceDiscoveryComponent {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException, Exception {
        
        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
        
        HiveServerComponent server = (HiveServerComponent) context.getParentResourceComponent();
        Configuration conf = context.getParentResourceContext().getPluginConfiguration();
        String sql = conf.getSimpleValue("table.query", "select distinct t.tbl_name from TBLS");
        Pattern pattern = Pattern.compile(conf.getSimpleValue("table.match", ".*"));
        Connection connection = server.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                if (!pattern.matcher(name).find())
                    continue;
            
                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                        context.getResourceType(),
                        name, // database key
                        name, // UI name
                        null, // Version
                        "Hive Table " + name,
                        context.getDefaultPluginConfiguration(),
                        null); // process info
                details.add(detail);
            }

        } finally {
            ps.close();
        }
        return details;
    }
    
}
