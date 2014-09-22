package com.apple.iad.rhq.memcached;

import static com.apple.iad.rhq.memcached.Stats.GET_HITS;
import static com.apple.iad.rhq.memcached.Stats.GET_MISSES;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
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

/**
 * Memcached monitoring component.
 */
public class MemcachedComponent implements ResourceComponent<MemcachedComponent>, MeasurementFacet {

    public static final String GET_RATIO = "get_ratio";

    public static final String PORT = "port";

    public static final String HOSTNAME = "hostname";

    private final Log log = LogFactory.getLog(getClass());

    private InetSocketAddress address;

    private Map<String, String> info = Collections.emptyMap();

    /**
     * Current sample misses.
     */
    private long get_misses;

    /**
     * Last sample misses.
     */
    private long get_misses2;

    /**
     * Current sample gets.
     */
    private long get_hits;

    /**
     * Last sample gets.
     */
    private long get_hits2;

    /**
     * Starts this instance.
     */
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        Configuration config = resourceContext.getPluginConfiguration();
        int port = Integer.parseInt(config.getSimpleValue(PORT, "11211"));
        String hostname = config.getSimpleValue(HOSTNAME, "localhost");
        address = new InetSocketAddress(hostname, port);
    }

    @Override
    public void stop() {
        info.clear();
    }

    public AvailabilityType getAvailability() {
        Socket socket = null;
        try {
            socket = MemcachedDiscovery.connect(address);
            info = new Stats(socket).info();
            return AvailabilityType.UP;
        } catch (IOException e) {
            log.warn("cannot connect", e);
            return AvailabilityType.DOWN;
        } finally {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
            }
        }
    }

    InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> msrs) throws Exception {
        getAvailability();
        for (MeasurementScheduleRequest msr : msrs) {
            String name = msr.getName();
            if (name.equals(GET_RATIO)) {
                String ms = info.get(GET_MISSES);
                String hs = info.get(GET_HITS);
                if (ms == null || hs == null)
                    continue;
                get_misses = Long.parseLong(ms);
                get_hits = Long.parseLong(hs);
                double hd = get_hits - get_hits2;
                double md = get_misses - get_misses2;
                get_misses2 = get_misses;
                get_hits2 = get_hits;
                if (md + hd == 0)
                    continue; // avoid NaN
                double d = hd / (md + hd);
                if (d < 0)
                    continue; // stats were reset?
                mr.addData(new MeasurementDataNumeric(msr, d));
                continue;
            }
            String s = info.get(name);
            if (s != null) {
                if (msr.getDataType() == DataType.TRAIT) {
                    mr.addData(new MeasurementDataTrait(msr, s));
                } else {
                    mr.addData(new MeasurementDataNumeric(msr, Double.parseDouble(s)));
                }
            } else {
                log.warn("stat not found " + name);
            }
        }
    }

}
