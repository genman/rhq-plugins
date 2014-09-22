package com.apple.iad.rhq.memcached;

import static java.lang.Integer.parseInt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.List;
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

/**
 * Discovers a list of memcached instance using process scanning.
 * Optionally allows manual adding.
 */
public class MemcachedDiscovery implements ResourceDiscoveryComponent, ManualAddFacet {

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Timeout in milliseconds.
     */
    private static final int TIMEOUT = 1000 * 10;

    /**
     * Default port.
     */
    public static final int DEFAULT_PORT = 11211;

    private static final Pattern pattern = Pattern.compile("-(p|l)\\s*([^:\\s]+)(?::(\\d+))?");

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
        List<ProcessScanResult> processes = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult p : processes) {
            DiscoveredResourceDetails detail = discover(context, p);
            details.add(detail);
        }
        return details;
    }

    static Socket connect(SocketAddress ia) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(TIMEOUT);
        socket.connect(ia, TIMEOUT);
        return socket;
    }

    private DiscoveredResourceDetails discover(ResourceDiscoveryContext context, ProcessScanResult p) throws SocketException, IOException {
        String[] cl = p.getProcessInfo().getCommandLine();
        InetSocketAddress ia = address(cl);
        log.info("connecting to " + ia);
        Socket socket = connect(ia);
        Stats stats = new Stats(socket);
        try {
            stats.info();
            log.info("discovered " + ia + " stats " + stats.info().size());
        } finally {
            stats.close();
        }
        String name = "memcached " + toString(ia);
        Configuration pluginConfiguration = context.getDefaultPluginConfiguration();
        pluginConfiguration.put(new PropertySimple(MemcachedComponent.PORT, ia.getPort()));
        pluginConfiguration.put(new PropertySimple(MemcachedComponent.HOSTNAME, ia.getHostName()));
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                context.getResourceType(),
                name, // database key
                name, // UI name
                null, // Version
                name,
                pluginConfiguration,
                null); // process info
        return detail;
    }

    InetSocketAddress address(String[] cl) {
        Matcher matcher = pattern.matcher(join(cl));
        InetSocketAddress ia;
        if (matcher.find()) {
            char opt = matcher.group(1).charAt(0);
            String val = matcher.group(2);
            if (opt == 'p') {
                int port = Integer.parseInt(val);
                ia = new InetSocketAddress(port);
            } else {
                String s = matcher.group(3);
                int port = s != null ? parseInt(s) : DEFAULT_PORT;
                ia = new InetSocketAddress(val, port);
            }
        } else {
            ia = new InetSocketAddress(DEFAULT_PORT);
        }
        return ia;
    }

    private String join(String sa[]) {
        StringBuilder sb = new StringBuilder();
        for (String s : sa)
            sb.append(s).append(' ');
        return sb.toString();
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration config, ResourceDiscoveryContext rdc)
            throws InvalidPluginConfigurationException {
        String portStr = config.getSimpleValue(MemcachedComponent.PORT, null);
        if (portStr == null)
            throw new InvalidPluginConfigurationException("port not set");
        // TODO
        // String hostname = config.getSimpleValue(MemcachedComponent.HOSTNAME, null);
        int port = Integer.parseInt(portStr);
        String name = "memcached " + port;
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                rdc.getResourceType(),
                name, // database key
                name, // UI name
                null, // Version
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
