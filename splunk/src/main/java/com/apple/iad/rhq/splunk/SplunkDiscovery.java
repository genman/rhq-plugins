package com.apple.iad.rhq.splunk;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

import com.splunk.Service;

/**
 * Discovers a group of MBeans where the metrics should be added together.
 * The key to group by should be called {@link key}
 */
public class SplunkDiscovery implements ResourceDiscoveryComponent, ManualAddFacet<ResourceComponent<?>> {

    public static final String PORT = "port";
    public static final String HOME = "home";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    private static final String HOST = "host";

    private static final Log log = LogFactory.getLog(SplunkDiscovery.class);

    /**
     * Discover our Splunk instance.
     */
    @Override
    public Set discoverResources(ResourceDiscoveryContext context) throws Exception {
        Set<DiscoveredResourceDetails> drds = new HashSet<DiscoveredResourceDetails>();
        List<ProcessScanResult> scan = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult r : scan) {
            DiscoveredResourceDetails drd = discover(context, r);
            drds.add(drd);
        }
        return drds;
    }

    static Service service(Configuration conf) throws IOException {
        String hostname = conf.getSimpleValue(HOST, "localhost");
        String port = conf.getSimpleValue(PORT);
        String user = conf.getSimpleValue(USER, "admin");
        String pass = conf.getSimpleValue(PASSWORD, "changeme");

        Service service = new Service(hostname, Integer.parseInt(port));
        log.debug("login");
        service.login(user, pass);
        return service;
    }

    static String version(Configuration conf) throws IOException {
        Service service = service(conf);
        String version = service.getInfo().getVersion();
        service.logout();
        return version;
    }

    DiscoveredResourceDetails discover(ResourceDiscoveryContext context, ProcessScanResult r) throws Exception {
        ProcessInfo pi = r.getProcessInfo();
        String home = pi.getEnvironmentVariable("SPLUNK_HOME");
        boolean next = false;
        String port = null;
        for (String arg : pi.getCommandLine()) {
            if (next) {
                port = arg;
                break;
            }
            if (arg.equals("-p")) {
                next = true;
            }
        }
        Configuration conf = context.getDefaultPluginConfiguration();
        if (port != null)
            conf.setSimpleValue(PORT, port);
        if (home != null) {
            conf.setSimpleValue(HOME, home);
        }

        log.debug("discovering splunk with conf " + conf.getSimpleProperties());
        String version = version(conf);
        log.debug("version " + version);

        DiscoveredResourceDetails drd = new DiscoveredResourceDetails(
                context.getResourceType(),
                conf.getSimpleValue(PORT), // key
                "Splunk",
                version,
                "Splunk instance; could be forwarder",
                conf, null);
        return drd;
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration conf,
            ResourceDiscoveryContext<ResourceComponent<?>> context)
            throws InvalidPluginConfigurationException {

        String port = conf.getSimpleValue(PORT);
        if (port == null)
            throw new InvalidPluginConfigurationException("null port");
        String version;
        try {
            version = version(conf);
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException("port check", e);
        }
        DiscoveredResourceDetails drd = new DiscoveredResourceDetails(
                context.getResourceType(),
                port,
                "Splunk",
                version,
                "Splunk instance; could be forwarder",
                conf, null);
        return drd;
    }

}
