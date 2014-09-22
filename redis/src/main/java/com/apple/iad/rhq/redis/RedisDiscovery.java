package com.apple.iad.rhq.redis;

import static redis.clients.jedis.Protocol.DEFAULT_PORT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import redis.clients.jedis.exceptions.JedisException;

/**
 * Discovers a list of redis instance using process scanning.
 * Optionally allows manual adding.
 */
public class RedisDiscovery implements ResourceDiscoveryComponent, ManualAddFacet {

    private static final Pattern pport = Pattern.compile("^port\\s+(\\d+)");

    private static final Log log = LogFactory.getLog(RedisDiscovery.class);

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
        List<ProcessScanResult> processes = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult p : processes) {
            String[] cl = p.getProcessInfo().getCommandLine();
            int port = DEFAULT_PORT;
            for (int i = 1; i < cl.length; i++) { // 0 = command
                String arg = cl[i];
                if (!arg.startsWith("-")) {
                    File f = new File(arg);
                    if (!f.isAbsolute())
                        f = new File(p.getProcessInfo().getCurrentWorkingDirectory(), arg);
                    port = port(f);
                }
                if (arg.equals("--port"))
                    port = Integer.parseInt(cl[++i]);
            }
            // load conf file?
            log.debug("connecting to " + port);
            Properties props;
            Client2 client = new Client2("localhost", port);
            try {
                props = client.infoAll();
                log.info("discovered redis " + port + " props " + props.size());
            } catch (JedisException e) {
                log.warn("failed connection to " + port, e);
                continue;
            } finally {
                client.disconnect();
            }
            client.info();
            String name = "redis " + port;
            Configuration pluginConfiguration = context.getDefaultPluginConfiguration();
            pluginConfiguration.put(new PropertySimple(RedisComponent.PORT, port));
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                    context.getResourceType(),
                    name, // database key
                    name, // UI name
                    props.getProperty("redis_version"), // Version
                    "Redis Server "+ port,
                    pluginConfiguration,
                    null); // process info

            details.add(detail);
        }
        return details;
    }

    static int port(File conf) throws IOException {
        log.debug("loading " + conf);
        BufferedReader br = new BufferedReader(new FileReader(conf));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = pport.matcher(line);
                if (matcher.matches()) {
                    String port = matcher.group(1);
                    log.debug("found port " + port);
                    return Integer.parseInt(port);
                }
            }
        } finally {
            br.close();
        }
        log.debug("did not find port");
        return DEFAULT_PORT;
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration config, ResourceDiscoveryContext rdc)
            throws InvalidPluginConfigurationException {
        String portStr = config.getSimpleValue(RedisComponent.PORT, null);
        if (portStr == null)
            throw new InvalidPluginConfigurationException("port not set");
        int port = Integer.parseInt(portStr);
        Client2 client = new Client2("localhost", port);
        Properties props;
        try {
            props = client.infoAll();
            log.info("discovered redis " + port);
        } finally {
            client.disconnect();
        }
        String name = "redis " + port;
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                rdc.getResourceType(),
                name, // database key
                name, // UI name
                props.getProperty("redis_version"), // Version
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
