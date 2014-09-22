package com.apple.iad.rhq.tten;

import static java.lang.Integer.parseInt;
import static java.util.Collections.singletonList;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.database.BasePooledConnectionProvider;

/**
 * Discovers TimesTen instance.
 */
public class TimesTenDiscovery implements ResourceDiscoveryComponent<ResourceComponent<?>>, ManualAddFacet<ResourceComponent<?>>, ClassLoaderFacet {

    private final Log log = LogFactory.getLog(getClass());

    private Pattern pport = Pattern.compile("-p\\s+(\\d+)");
    private Pattern pname = Pattern.compile("TimesTen/(\\w+)/");
    private Pattern pversion = Pattern.compile("product/([^/]+)/");

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<ResourceComponent<?>> context)
            throws InvalidPluginConfigurationException, Exception {
        log.debug("find TTEN");

        Set<DiscoveredResourceDetails> drd = new HashSet<DiscoveredResourceDetails>();

        for (ProcessScanResult proc : context.getAutoDiscoveredProcesses()) {
            int port = 53397;
            String cl = join(proc.getProcessInfo().getCommandLine());
            log.debug("found TTEN " + cl);
            Matcher m = pport.matcher(cl);
            String name = "tten";
            String version = "";
            if (m.matches()) {
                port = parseInt(m.group(1));
            }
            m = pname.matcher(cl);
            if (m.matches()) {
                name = m.group(1);
            }
            m = pversion.matcher(cl);
            if (m.matches()) {
                version = m.group(1);
            }
            log.debug("name " + name + " port " + port + " version " + version);
            Configuration config = context.getDefaultPluginConfiguration();
            config.setSimpleValue(BasePooledConnectionProvider.URL, "jdbc:timesten:client:TT_SERVER=localhost;TT_SERVER_DNS=Name_Of_DSN;TCP_PORT=" + port);
            try {
                String command = proc.getProcessInfo().getCommandLine()[0];
                log.debug("command " + command);
               String lib = new File(command).getParentFile().getParent() + File.separator + "lib";
                log.debug("lib " + lib);
                config.setSimpleValue(TimesTenComponent.JAVA_LIBRARY_PATH, lib);
            } catch (Exception e) {
                log.debug("cannot find lib " + e);
            }
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                    context.getResourceType(),
                    name + ":" + port, // key
                    "TimesTen " + name, // name
                    version, // Version
                    "Database server " + name,
                    config,
                    null); // process info
            drd.add(detail);
            // /u01/app/timesten/product/11.2.2.6.4/TimesTen/ttstg1/bin/ttcserver -verbose -userlog
            // tterrors.log -supportlog ttmesg.log -id 1000004 -p 53397 -facility user -group ttusers
        }

        return drd;
    }

    private String join(String[] cl) {
        StringBuilder sb = new StringBuilder();
        for (String s : cl)
            sb.append(s).append(" ");
        return sb.toString();
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration config,
            ResourceDiscoveryContext<ResourceComponent<?>> context)
            throws InvalidPluginConfigurationException {
        String url = config.getSimpleValue("url", "jdbc:timesten:");
        String version = null;
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                context.getResourceType(),
                url,
                "TimesTen " + url, // name
                version, // Version
                "Database server " + url,
                config,
                null); // process info
        return detail;
    }

    private File getJarPath(Configuration config) {
        String path = config.getSimpleValue(TimesTenComponent.JAVA_LIBRARY_PATH, "");
        String driver = config.getSimpleValue(TimesTenComponent.JAVA_JAR, "ttjdbc6.jar");
        return new File(path, driver);
    }

    /**
     * Obtains the classpath for the JDBC driver.
     */
    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext rdc, DiscoveredResourceDetails drd)
            throws Exception
    {
        File file = getJarPath(drd.getPluginConfiguration());
        if (!file.canRead())
            throw new IOException("Cannot read " + file);
        log.info("found driver " + file);
        return singletonList(file.toURI().toURL());
    }

}
