package com.apple.iad.rhq.hadoop;

import static java.util.regex.Pattern.compile;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.support.metadata.LocalVMTypeDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

import com.apple.iad.rhq.hadoop.HadoopClassLoaderFacet;

/**
 * Discover individual Hadoop or HBase processes.
 */
public class HadoopDiscovery extends JMXDiscoveryComponent implements ClassLoaderFacet {

    private final Log log = LogFactory.getLog(HadoopDiscovery.class);
    private static final String v = "([0-9\\.+\\-\\w]+).jar";
    private static final Pattern PATTERNS[] = {
        compile("hbase-" + v),
        compile("hadoop-core-" + v),
        compile("zookeeper-" + v),
    };

    /**
     * Discover JMX resources.
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext resourceDiscoveryContext) {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        @SuppressWarnings("unchecked")
        List<ProcessScanResult> parentProcessScans = resourceDiscoveryContext.getAutoDiscoveredProcesses();
        log.debug("process scans " + parentProcessScans);
        ResourceType resourceType = resourceDiscoveryContext.getResourceType();
        String rtName = resourceType.getName();

        for (ProcessScanResult psr : parentProcessScans) {
            ProcessInfo pi = psr.getProcessInfo();

            String[] commandLineArgs = pi.getCommandLine();
            String version = getVersion(commandLineArgs);

            DiscoveredResourceDetails detail = super.discoverResourceDetails(resourceDiscoveryContext, pi);
            if (detail != null) {
                // connect via JMX port mechanism
                // override the key etc.
                detail.setResourceKey(rtName);  // won't run multiple instances (probably)
                detail.setResourceName(rtName); // TaskTracker, etc.
                detail.setResourceVersion(version);
                detail.setResourceDescription("Hadoop Service " + rtName);
            } else {
                // connect via Attach API; this is not recommended
                // 1) Attach API doesn't work when running the Agent as rhq user
                // 2) Doesn't work as root most of the time either ...
                Configuration pluginConfiguration = resourceDiscoveryContext.getDefaultPluginConfiguration();
                detail = new DiscoveredResourceDetails(
                        resourceType, // ResourceType
                        rtName, // won't run multiple instances (probably)
                        rtName, // resource name
                        version, // Version
                        "Hadoop Server " + rtName,
                        pluginConfiguration,
                        psr.getProcessInfo() // process info
                );

                pluginConfiguration.put(new PropertySimple(JMXDiscoveryComponent.COMMAND_LINE_CONFIG_PROPERTY,
                        join(commandLineArgs)));
                pluginConfiguration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                        LocalVMTypeDescriptor.class.getName()));
            }

            log.debug("Discovered " + detail);
            log.debug("plugin configuration " + detail.getPluginConfiguration());
            details.add(detail);
        }

        return details;
    }

    /**
     * Return the full command line that 'jps -l' would list
     * @param args Command line args
     */
    private String join(String[] args) {
        StringBuilder b = new StringBuilder();
        boolean found = false;
        for (String arg : args) {
            if (arg.startsWith("org.apache."))
                found = true;
            if (found) {
                if (b.length() > 0)
                    b.append(" ");
                b.append(arg);
            }
        }
        return b.toString();
    }

    /**
     * Return the Hadoop version from command line by looking at jar file names.
     * @param commandLine Command line args for the java executable
     */
    String getVersion(String[] commandLine) {
        for (String line : commandLine) {
            for (Pattern p : PATTERNS) {
                Matcher m = p.matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        return null;
    }

    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext rdc, DiscoveredResourceDetails drd)
            throws Exception
    {
        return new HadoopClassLoaderFacet().getAdditionalClasspathUrls(rdc, drd);
    }

}
