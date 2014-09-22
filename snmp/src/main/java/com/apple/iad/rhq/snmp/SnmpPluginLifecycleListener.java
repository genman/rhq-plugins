package com.apple.iad.rhq.snmp;

import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.util.DefaultThreadFactory;
import org.snmp4j.util.DefaultTimerFactory;

/**
 * Initializes the threading, etc.
 */
public class SnmpPluginLifecycleListener implements PluginLifecycleListener {

    private Threading threading;

    public void initialize(PluginContext context) throws Exception {
        threading = new Threading(5);
        SNMP4JSettings.setThreadFactory(threading);
        SNMP4JSettings.setTimerFactory(threading);
        org.snmp4j.log.LogFactory.setLogFactory(new Log4jLogFactory());
    }

    public void shutdown() {
        threading.close();
        SNMP4JSettings.setThreadFactory(new DefaultThreadFactory());
        SNMP4JSettings.setTimerFactory(new DefaultTimerFactory());
        org.snmp4j.log.LogFactory.setLogFactory(null);
    }
}
