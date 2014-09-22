package com.apple.iad.rhq.mongodb;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.mongodb.DBAddress;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

/**
 * Discovers a list of mongodb instance using process scanning.
 * Optionally allows manual adding.
 */
public class MongoDBServerDiscovery implements ResourceDiscoveryComponent, ManualAddFacet {

    private static final Log log = LogFactory.getLog(MongoDBServerDiscovery.class);

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
        List<ProcessScanResult> processes = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult p : processes) {
            String[] cl = p.getProcessInfo().getCommandLine();
            int port = DBAddress.defaultPort();
            String host = "localhost";
            for (int i = 1; i < cl.length; i++) { // 0 = command
                if (cl[i].equals("--port"))
                    port = Integer.parseInt(cl[++i]);
                if (cl[i].equals("--bind_ip"))
                    host = cl[++i].split(",")[0];
            }
            // load conf file?
            log.debug("connecting to " + port);
            MongoClient client = new MongoClient(host, port);
            try {
                log.debug("databases " + client.getDatabaseNames());
                discover(context, details, client);
            } catch (Exception e) {
                log.warn("failed connection to " + port, e);
                continue;
            } finally {
                client.close();
            }
        }
        return details;
    }

    private void discover(ResourceDiscoveryContext context, Set<DiscoveredResourceDetails> details, MongoClient client) {
        int port = client.getAddress().getPort();
        String host = client.getAddress().getHost();

        String rname = "mongodb server " + port;
        log.info("discovered mongodb " + rname);
        Configuration pluginConfiguration = context.getDefaultPluginConfiguration();
        String uri = "mongodb://" + host + ":" + port;
        pluginConfiguration.put(new PropertySimple(MongoDBServerComponent.URI, uri));
        StatClient statClient = new StatClient(client);
        String ver = statClient.getString("version");
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                context.getResourceType(),
                uri, // database key
                rname, // UI name
                ver,
                "MongoDB " + port,
                pluginConfiguration,
                null); // process info

        details.add(detail);
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration config, ResourceDiscoveryContext rdc)
            throws InvalidPluginConfigurationException {
        String uri = config.getSimpleValue(MongoDBServerComponent.URI, null);
        if (uri == null)
            throw new InvalidPluginConfigurationException("uri not set");
        MongoClientURI muri = new MongoClientURI(uri);
        MongoClient client = null;
        try {
            client = new MongoClient(muri);
            client.getDatabaseNames();
            log.info("discovered mongodb " + uri);
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException(e);
        } finally {
            if (client != null)
                client.close();
        }
        String name = "mongodb server " + muri;
        String ver = "";
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                rdc.getResourceType(),
                muri.toString(), // database key
                name, // UI name
                ver,
                name,
                config,
                null); // process info
        return detail;
    }

    public String toString(InetSocketAddress ia) {
        if (ia.getAddress().isAnyLocalAddress())
            return String.valueOf(ia.getPort());
        return ia.toString();
    }
}
