package com.apple.iad.rhq.hadoop.hive;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Connects to Hadoop Hive.
 * Doesn't provide any stats at the moment.
 *
 * @author Elias Ross
 */
public class HiveServerComponent implements ResourceComponent<HiveServerComponent> {

    private static final int IS_VALID_TIME = 2;

    public static final String JDBC_URL = "jdbc.url";

    public static final String JDBC_DRIVER = "jdbc.driver";

    public static final String driverClass = "com.mysql.jdbc.Driver";

    public static final String FS_DEFAULT_NAME = "fs.default.name";

    public static final String LOCATION_QUERY = "location.query";

    private final Log log = LogFactory.getLog(getClass());

    private Connection connection;

    private ResourceContext<ResourceComponent<?>> resourceContext;

    private FileSystem fileSystem;

    private String locationQuery;

    /**
     * Starts this instance.
     */
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        debug_cl();
        this.resourceContext = resourceContext;
        this.connection = buildConnection(resourceContext.getPluginConfiguration());

        Configuration c = resourceContext.getPluginConfiguration();
        org.apache.hadoop.conf.Configuration hc = new org.apache.hadoop.conf.Configuration();
        String name = c.getSimpleValue(FS_DEFAULT_NAME, "");
        hc.set(FS_DEFAULT_NAME, name);
        fileSystem = FileSystem.get(hc);
        locationQuery = c.getSimpleValue(LOCATION_QUERY, "");
    }

    void debug_cl() throws Exception {
        log.info("debug_cl - testing class loader");
        try {
            log.info("thread.ccl " + Thread.currentThread().getContextClassLoader());
            log.info("class cl" + getClass().getClassLoader());
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class clazz;

            clazz = cl.loadClass("org.apache.hadoop.security.Groups");
            log.info("class " + clazz);
            Object o = clazz.getMethod("getUserToGroupsMappingService").invoke(null);

            clazz = cl.loadClass("org.apache.hadoop.security.UserGroupInformation");
            log.info("class " + clazz);
            o = clazz.getMethod("getCurrentUser").invoke(null);
            log.info("invoked " + o);

            clazz = cl.loadClass("org.apache.hadoop.fs.FileSystem");
            log.info("class " + clazz);
            o = clazz.getMethod("getAllStatistics").invoke(null);
            log.info("invoked " + o);

            log.info("current user");
            log.info(UserGroupInformation.class.getClassLoader());
            log.info(UserGroupInformation.getCurrentUser());
        } catch (Throwable t) {
            log.warn("debug_cl ", t);
        }
    }

    @Override
    public void stop() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                log.debug("close", e);
            }
        }
        try {
            fileSystem.close();
        } catch (IOException e) {
            log.debug("close", e);
        }
    }

    public AvailabilityType getAvailability() {
        try {
            if (getConnection() != null)
                return AvailabilityType.UP;
        } catch (SQLException e) {
            log.warn("failed connecting", e);
        }
        return AvailabilityType.DOWN;
    }

    /**
     * Returns the database connection for Hive.
     * Returns null if one could not be obtained.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = buildConnection(resourceContext.getPluginConfiguration());
        }
        if (!connection.isValid(IS_VALID_TIME)) {
            connection.close();
        }
        return this.connection;
    }

    /**
     * Returns the Hadoop filesystem.
     */
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    private Connection buildConnection(Configuration configuration) throws SQLException {
        String driver = configuration.getSimpleValue(JDBC_DRIVER, driverClass);
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new InvalidPluginConfigurationException("Specified JDBC driver class (" + driver + ") not found.");
        }

        String url = buildUrl(configuration);
        log.debug("Attempting JDBC connection to [" + url.replaceAll("\\?.*", "") + "]");

        return DriverManager.getConnection(url, new Properties());
    }

    static String scrub(String url) {
        return url.replaceAll("\\?.*", "");
    }

    private static String buildUrl(Configuration configuration) {
        return configuration.getSimpleValue(JDBC_URL, "jdbc:mysql://localhost/metastore_stglab?" +
                "user=monty&password=test");
    }

    /**
     * Configured location query.
     */
    String getLocationQuery() {
        return locationQuery;
    }

}