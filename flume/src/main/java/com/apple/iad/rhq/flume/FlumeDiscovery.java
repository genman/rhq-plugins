package com.apple.iad.rhq.flume;

import static java.util.regex.Pattern.compile;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

public class FlumeDiscovery extends JMXDiscoveryComponent {

    private static final String v = "([0-9\\.+\\-\\w]+).jar";
    private static final Pattern PATTERN = compile("flume-ng-core-" + v);

    @Override
    protected String getJavaVersion(ProcessInfo process, JMXServiceURL url) {
        try {
            JMXConnector con = JMXConnectorFactory.connect(url);
            RuntimeMXBean runtimeMXBean = ManagementFactory.newPlatformMXBeanProxy(con.getMBeanServerConnection(),
                    ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
            String cp = runtimeMXBean.getClassPath();
            con.close();
            return getVersion(cp);
        } catch (Exception e) {
            return super.getJavaVersion(process, url);
        }
    }

    private String getVersion(String line) {
        Matcher m = PATTERN.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

}
