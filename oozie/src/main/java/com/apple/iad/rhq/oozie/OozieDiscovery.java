package com.apple.iad.rhq.oozie;

import static java.util.regex.Pattern.compile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

import com.apple.iad.rhq.http.HttpComponent;
import com.apple.iad.rhq.http.HttpDiscovery;
import com.apple.iad.rhq.http.JSONProvider;

/**
 * Finds Oozie and the URL for connecting to administration.
 *
 * @author Elias Ross
 */
// Could use JMX discovery as well, but this is neater
// public class OozieDiscovery extends JMXDiscoveryComponent {
public class OozieDiscovery extends HttpDiscovery {

    private static final Log log = LogFactory.getLog(OozieDiscovery.class);
    private static final Pattern BASE_URL = compile("-Doozie.base.url=(\\S+)");
    private static final Pattern HOST = compile("-Doozie.http.hostname=(\\S+)");
    private static final Pattern PORT = compile("-Doozie.http.port=(\\d+)");

    @Override
    protected int getDiscoveryTimeout() {
        return 15 * 1000;
    }

    /**
     * Discover Oozie resources.
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext resourceDiscoveryContext) {
        try {
            return discoverResources0(resourceDiscoveryContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Set<DiscoveredResourceDetails> discoverResources0(
            ResourceDiscoveryContext resourceDiscoveryContext) throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        @SuppressWarnings("unchecked")
        List<ProcessScanResult> parentProcessScans = resourceDiscoveryContext.getAutoDiscoveredProcesses();
        log.debug("process scans " + parentProcessScans);

        for (ProcessScanResult psr : parentProcessScans) {
            ProcessInfo pi = psr.getProcessInfo();

            String[] commandLineArgs = pi.getCommandLine();
            URL url = getUrl(commandLineArgs);
            Configuration conf = resourceDiscoveryContext.getDefaultPluginConfiguration();
            conf.setSimpleValue("url", url.toString());
            DiscoveredResourceDetails detail = super.discoverResource(conf, resourceDiscoveryContext);

            log.info("Discovered " + detail);
            log.info("plugin configuration " + detail.getPluginConfiguration());
            details.add(detail);
        }

        return details;
    }

    @Override
    protected String getVersion(ResourceDiscoveryContext context, String ver, URL url) {
        try {
            URL vurl = url.toURI().resolve("v1/admin/build-version").toURL();
            JSONProvider json = new JSONProvider(HttpComponent.getBody(vurl));
            return json.extractValue("buildVersion").toString();
        } catch (Exception e) {
            log.info("failed to obtain version", e);
        }
        return ver;
    }

    @Override
    protected String getResourceName(ResourceDiscoveryContext context, URL url) {
        String name = context.getResourceType().getName() + ":" + url.getPort();
        return name;
    }

    @Override
    protected String getResourceDescription(ResourceDiscoveryContext context, URL url) {
        return "Oozie Service " + url.getPort();
    }

    /**
     * Return the URL from command line by looking at system property values.
     * @param commandLine Command line args for the java executable
     */
    URL getUrl(String[] commandLine) throws MalformedURLException {
        String port = "11000";
        String host = "localhost";
        for (String line : commandLine) {
            Matcher m = BASE_URL.matcher(line);
            if (m.find()) {
                return new URL(m.group(1));
            }
            m = PORT.matcher(line);
            if (m.find()) {
                port = m.group(1);
            }
            m = HOST.matcher(line);
            if (m.find()) {
                host = m.group(1);
            }
        }
        return new URL("http://" + host + ":" + port + "/oozie");
    }


}
