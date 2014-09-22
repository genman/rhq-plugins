package com.apple.iad.rhq.hadoop;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.testng.annotations.Test;

public class HadoopClassLoaderFacetTest {

    private static Log log = LogFactory.getLog(HadoopClassLoaderFacetTest.class);

    File file = new File(getClass().getResource("/hadoop.sh").getFile());

    /**
     * Tests using a mock command that can show a classpath.
     */
    @Test
    public void test() throws Exception {

        ResourceType type = new ResourceType();
        ResourceComponent parent = null;
        ResourceContext parentResourceContext = null;
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        String pluginContainerName = null;
        PluginContainerDeployment pluginContainerDeployment = null;
        ResourceDiscoveryContext rdc = new ResourceDiscoveryContext(type, parent,
                parentResourceContext, systemInfo,
                null, Collections.emptyList(),
                pluginContainerName, pluginContainerDeployment);

        HadoopClassLoaderFacet h = new HadoopClassLoaderFacet();
        Configuration config = new Configuration();
        config.setSimpleValue(HadoopClassLoaderFacet.HADOOP_CLASSPATH_COMMAND, file + " classpath");
        ProcessInfo processInfo = null;
        DiscoveredResourceDetails drd = new DiscoveredResourceDetails(type,
                "key", "name", "ver", "desc", config, processInfo);
        List<URL> url = h.getAdditionalClasspathUrls(rdc, drd);
        log.info(url);
        assert url.size() > 2;
    }

}
