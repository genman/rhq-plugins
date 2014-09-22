package com.apple.iad.rhq.port;

import static java.lang.Integer.parseInt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
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

/**
 * Discovers a group of MBeans where the metrics should be added together.
 * The key to group by should be called {@link key}
 */
public class PortDiscovery implements ResourceDiscoveryComponent, ManualAddFacet<ResourceComponent<?>> {

    public static final String ADDRESS = "address";

    public static final String SOURCE  = "source";

    public static final String PATTERN  = "pattern";

    /**
     * Regex identifying the host/port parts.
     */
    private static final Pattern apattern = Pattern.compile("(.*):(\\d+)");

    private static final InetAddress localhost;

    private final Log log = LogFactory.getLog(this.getClass());

    static {
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new Error(e);
        }
    }

    static InetSocketAddress parse(String address) throws UnknownHostException {
        Matcher matcher = apattern.matcher(address);
        if (matcher.find()) {
            String host = matcher.group(1);
            String port = matcher.group(2);
            InetSocketAddress isa = new InetSocketAddress(host, parseInt(port));
            if (isa.isUnresolved())
                throw new UnknownHostException(host);
            return new InetSocketAddress(localhost, parseInt(port));
        } else {
            return new InetSocketAddress(localhost, parseInt(address));
        }
    }

    static void probe(InetSocketAddress isa) throws IOException {
        Socket s = new Socket(isa.getAddress(), isa.getPort());
        s.getInputStream(); // Does this do anything?
        s.close();
    }

    private DiscoveredResourceDetails discover(Configuration conf, ResourceDiscoveryContext context, InetSocketAddress isa) {
        log.debug("discover " + isa);
        try {
            String addr;
            if (isa.getAddress().equals(localhost))
                addr = String.valueOf(isa.getPort());
            else
                addr = isa.getHostName() + ":" + isa.getPort();
            String name = context.getResourceType().getName() + " " + addr;
            probe(isa);
            DiscoveredResourceDetails drd = new DiscoveredResourceDetails(context.getResourceType(),
                    addr, name, "", name,
                    conf, null);
            return drd;
        } catch (IOException e) {
            log.debug("failed to connect " + e);
        }
        return null;
    }

    /**
     * Groups object names by a pattern seen in key.
     * Parent class does the work of finding the object names; we reduce this list
     * by stripping out the common pattern.
     */
    @Override
    public Set discoverResources(ResourceDiscoveryContext context) throws Exception {
        Set<DiscoveredResourceDetails> drds = new HashSet<DiscoveredResourceDetails>();
        Configuration conf = context.getDefaultPluginConfiguration();
        discoverResource0(drds, conf, context);
        return drds;
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration conf,
            ResourceDiscoveryContext<ResourceComponent<?>> context)
            throws InvalidPluginConfigurationException {
        try {
            Set<DiscoveredResourceDetails> drds = new HashSet<DiscoveredResourceDetails>();
            discoverResource0(drds, conf, context);
            if (drds.isEmpty())
                throw new InvalidPluginConfigurationException("Configuration missing " + ADDRESS + " or " + SOURCE);
            return drds.iterator().next();
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException("Could not discover" , e);
        }
    }

    public void discoverResource0(Set<DiscoveredResourceDetails> drds, Configuration conf,
            ResourceDiscoveryContext<ResourceComponent<?>> context)
            throws Exception
    {
        String address = conf.getSimpleValue(ADDRESS, null);
        log.debug("address " + address);
        if (address != null && !address.isEmpty()) {
            DiscoveredResourceDetails drd = discover(conf, context, parse(address));
            if (drd != null)
                drds.add(drd);
            return;
        }
        String source = conf.getSimpleValue(SOURCE, null);
        log.debug("source " + source);
        if (source != null && !source.isEmpty()) {
            InputStream is;
            try {
                URL url = new URL(source);
                is = url.openStream();
            } catch (MalformedURLException e) {
                File file = new File(source);
                if (!file.canRead()) {
                    log.debug("cannot read " + file);
                    return;
                }
                is = new FileInputStream(file);
            }
            String ps = conf.getSimpleValue(PATTERN, ".*?(\\d+)");
            Pattern pattern = Pattern.compile(ps);
            log.debug("address pattern " + pattern);
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = br.readLine()) != null) {
                    Matcher m = pattern.matcher(line);
                    if (m.matches()) {
                        String addr = m.group(1);
                        log.debug("found address " + addr);
                        DiscoveredResourceDetails drd = discover(conf, context, parse(addr));
                        if (drd != null)
                            drds.add(drd);
                    }
                }
            } finally {
                is.close();
            }
        }
    }

}
