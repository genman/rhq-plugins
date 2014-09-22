package com.apple.iad.rhq.hornetq;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

/**
 * Discovers HornetQ version.
 */
public class HornetQDiscovery extends JMXDiscoveryComponent {

    private static final ObjectName name;
    static {
        try {
            name = new ObjectName("org.hornetq:module=Core,type=Server");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected DiscoveredResourceDetails buildResourceDetails(ResourceDiscoveryContext context, ProcessInfo process, JMXServiceURL url, Integer port) {
        DiscoveredResourceDetails drd = super.buildResourceDetails(context, process, url, port);
        drd.setResourceName("HornetQ " + port);
        return drd;
    }

    @Override
    protected String getJavaVersion(JMXConnector con) throws Exception {
        try {
            return (String) con.getMBeanServerConnection().getAttribute(name, "Version");
        } catch (Exception e) {
            return super.getJavaVersion(con);
        }
    }

}
