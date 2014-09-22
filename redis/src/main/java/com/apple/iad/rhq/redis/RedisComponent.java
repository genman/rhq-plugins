package com.apple.iad.rhq.redis;

import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

import redis.clients.jedis.exceptions.JedisException;

/**
 * Redis component.
 *
 * @author Elias Ross
 */
public class RedisComponent implements ResourceComponent<RedisComponent>, MeasurementFacet {

    public static final String PORT = "port";

    private final Log log = LogFactory.getLog(getClass());

    private Properties info = new Properties();

    private int port;

    /**
     * Starts this instance.
     */
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        Configuration config = resourceContext.getPluginConfiguration();
        port = Integer.parseInt(config.getSimpleValue(PORT, "6379"));
    }

    @Override
    public void stop() {
        info.clear();
    }

    public AvailabilityType getAvailability() {
        Client2 client = new Client2("localhost", port);
        try {
            info = client.infoAll();
            return AvailabilityType.UP;
        } catch (JedisException e) {
            log.warn("cannot connect", e);
            return AvailabilityType.DOWN;
        } finally {
            client.disconnect();
        }
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> msrs) throws Exception {
        getAvailability();
        for (MeasurementScheduleRequest msr : msrs) {
            String s = info.getProperty(msr.getName());
            if (s != null) {
                if (msr.getDataType() == DataType.TRAIT) {
                    mr.addData(new MeasurementDataTrait(msr, s));
                } else {
                    mr.addData(new MeasurementDataNumeric(msr, Double.parseDouble(s)));
                }
            } else {
                log.warn("not found " + msr.getName());
            }
        }
    }

}
