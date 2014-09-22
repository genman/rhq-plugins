package com.apple.iad.rhq.hadoop;

import static org.testng.AssertJUnit.assertEquals;

import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

@Test
public class NameNodeTest extends ComponentTest {

    protected final Log log = LogFactory.getLog(getClass());

    // Use SSH port forwarding if you want to test remotely...
    private final int port = 8004;
    private final String url = "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi";

    public ResourceComponent getComponent(ResourceType name) {
        for (Map.Entry<ResourceComponent, Resource> c : components.entrySet())
            if (c.getValue().getResourceType().equals(name))
                return c.getKey();
        return null;
    }

    public void manualAdd() throws Exception {
        ResourceType resourceType = getResourceType("NameNode");
        Configuration conf = getConfiguration(resourceType);
        conf.setSimpleValue(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY, url);
        conf.setSimpleValue("fs.default.name", "file:/tmp");

        ResourceComponent rc = manuallyAdd(resourceType, conf);
        assertUp(rc);

        for (ResourceType rt : resourceType.getChildResourceTypes()) {
            ResourceComponent component = getComponent(rt);
            if (component != null) {
                MeasurementReport report = getMeasurementReport(component);
                log.info(rt + " " + map(report));
            }
        }
    }

    public void parse() throws Exception {
        String s = "http://vg61l01ad-hadoop002:50070/metrics?format=json";
        log.info(new JSONProvider(new URL(s)).getDocument());
    }

    public void pattern() {
        String s = "..../pipeline/ng_txn_log/2013-02-15/0400/FlumeData-17.172.18.160-s1.1360900807113.tmp 0 bytes, 0 block(s), OPENFORWRITE: ..";
        Matcher matcher = NameNodeFsckComponent.PATTERN.matcher(s);
        assert matcher.find();
        String g = matcher.group(1);
        assertEquals("/pipeline/ng_txn_log/2013-02-15/0400/FlumeData-17.172.18.160-s1.1360900807113.tmp", g);
    }

}
