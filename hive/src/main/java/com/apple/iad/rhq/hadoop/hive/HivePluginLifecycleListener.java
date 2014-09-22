package com.apple.iad.rhq.hadoop.hive;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;
import org.rhq.core.util.exception.ThrowableUtil;

public class HivePluginLifecycleListener implements PluginLifecycleListener {
    private final Log log = LogFactory.getLog(HivePluginLifecycleListener.class);

    public void initialize(PluginContext context) throws Exception {
        // no-op
    }

    public void shutdown() {
        // so we do not cause our classloader to leak perm gen, we need to de-register
        // any and all JDBC drivers this plugin registered
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            try {
                Driver driver = drivers.nextElement();
                DriverManager.deregisterDriver(driver);
                log.debug("Deregistered JDBC driver: " + driver.getClass());
            } catch (Exception e) {
                log.warn("Failed to deregister JDBC drivers - memory might leak" + ThrowableUtil.getAllMessages(e));
            }
        }

        log.debug(this.getClass().getSimpleName() + " completed shutdown.");
        return;
    }
}
