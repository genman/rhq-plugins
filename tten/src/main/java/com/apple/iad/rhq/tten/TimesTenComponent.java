package com.apple.iad.rhq.tten;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.database.ConnectionPoolingSupport;
import org.rhq.plugins.database.DatabasePluginUtil;
import org.rhq.plugins.database.PooledConnectionProvider;

/**
 * Component to connect to TimesTen database.
 */
public class TimesTenComponent implements ResourceComponent<ResourceComponent<?>>, MeasurementFacet, ConnectionPoolingSupport {

    private static final int TIMEOUT = 10;

    /**
     * Logger.
     */
    private final Log log = LogFactory.getLog(getClass());

    /**
     * Library path for loading native libraries.
     * This is in addition to LD_LIBRARY_PATH required at runtime.
     */
    public static final String JAVA_LIBRARY_PATH = "java.library.path";

    /**
     * Name of the JDBC jar for the driver.
     */
    public static final String JAVA_JAR = "jar";

    private TimesTenPooledConnectionProvider connectionProvider;

    @Override
    public AvailabilityType getAvailability() {
        log.debug("getAvail");
        try {
            Connection con = connectionProvider.getPooledConnection();
            boolean valid = con.isValid(TIMEOUT);
            con.close();
            if (valid)
                return AvailabilityType.UP;
        } catch (SQLException e) {
            log.debug("invalid", e);
        }
        return AvailabilityType.DOWN;
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws InvalidPluginConfigurationException, Exception {

        Configuration config = context.getPluginConfiguration();
        String path = config.getSimpleValue(JAVA_LIBRARY_PATH, "");
        if (!path.isEmpty()) {
            try {
                log.debug("addLibraryPath " + path);
                addLibraryPath(path);
            } catch (Exception e) {
                log.warn("cannot set " + JAVA_LIBRARY_PATH, e);
            }
        }

        connectionProvider = new TimesTenPooledConnectionProvider(config);
    }

    /**
     * Hack to add path.
     * @see http://fahdshariff.blogspot.com/2011/08/changing-java-library-path-at-runtime.html
     */
    private static void addLibraryPath(String path) throws Exception{
        Field field = ClassLoader.class.getDeclaredField("usr_paths");
        field.setAccessible(true);
        String[] paths = (String[])field.get(null);
        if (Arrays.asList(paths).contains(path))
            return;
        String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length-1] = path;
        field.set(null, newPaths);
    }

    @Override
    public void stop() {
        connectionProvider.close();
    }

    /**
     * Load all stats from SYS.SYSTEMSTATS, put in hashtable, then retrieve.
     * Not the most efficient but whatever.
     */
    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> reqs) throws Exception {
        Connection con = connectionProvider.getPooledConnection();
        PreparedStatement ps = con.prepareStatement("select name, value from SYS.SYSTEMSTATS");
        ResultSet rs = ps.executeQuery();
        try {
            Map<String, Long> stats = new HashMap<String, Long>();
            while (rs.next()) {
                String name = rs.getString(1);
                long value = rs.getLong(2);
                stats.put(name.trim(), value);
            }
            for (MeasurementScheduleRequest req : reqs) {
                Long stat = stats.get(req.getName());
                if (stat != null) {
                    MeasurementDataNumeric value = new MeasurementDataNumeric(req, stat.doubleValue());
                    report.addData(value);
                } else {
                    log.warn("no stat " + req);
                }
            }
        } finally {
            DatabasePluginUtil.safeClose(con, ps, rs);
        }
    }

    @Override
    public boolean supportsConnectionPooling() {
        return true;
    }

    @Override
    public PooledConnectionProvider getPooledConnectionProvider() {
        return connectionProvider;
    }

}
