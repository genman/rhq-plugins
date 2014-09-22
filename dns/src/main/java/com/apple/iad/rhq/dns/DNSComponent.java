package com.apple.iad.rhq.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
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
import org.xbill.DNS.Cache;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;

/**
 * DNS monitoring component.
 */
public class DNSComponent implements ResourceComponent<DNSComponent>, MeasurementFacet {

    private final Log log = LogFactory.getLog(getClass());

    private String[] hosts = new String[0];

    public static final String HOST = "host";
    
    /**
     * Number of total address records for all hosts.
     */
    private volatile int addresses = 0;

    private long time;

    /**
     * Current DNS server being used. Round-robin is used.
     */
    private String server;
    
    /**
     * Starts this instance.
     */
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        server = resourceContext.getResourceKey();
        Configuration config = resourceContext.getPluginConfiguration();
        String s = config.getSimpleValue(HOST, "");
        if (s.trim().isEmpty()) {
            s = InetAddress.getLocalHost().getHostName();
        }
        hosts = s.split("[\\s,]+");
    }

    @Override
    public void stop() {
        hosts = null;
    }

    public AvailabilityType getAvailability() {
        if (log.isDebugEnabled()) {
            log.debug("lookup " + Arrays.toString(hosts) + " server=" + server);
        }
        try {
            SimpleResolver resolver = new SimpleResolver(server);
            long start = System.nanoTime();
            int a = 0;
            for (String host : hosts) {
                Lookup lookup = new Lookup(host);
                lookup.setResolver(resolver);
                lookup.setCache(new Cache());
                Record[] addr = lookup.run();
                if (addr == null) {
                    log.warn("no answers " + lookup);
                    return AvailabilityType.DOWN;
                }
                a += addr.length;
            }
            addresses = a;
            time = System.nanoTime() - start;
            if (log.isDebugEnabled()) {
                log.debug("lookup done in " + time + "ns");
            }
            return AvailabilityType.UP;
        } catch (UnknownHostException e) {
            log.warn("failed to find host " + e);
            return AvailabilityType.DOWN;
        } catch (TextParseException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> msrs) throws Exception {
        getAvailability();
        for (MeasurementScheduleRequest msr : msrs) {
            String name = msr.getName();
            if (name.equals("queryTime")) {
                mr.addData(new MeasurementDataNumeric(msr, (double) this.time));
            }
            if (name.equals("ipCount")) {
                mr.addData(new MeasurementDataNumeric(msr, (double) this.addresses));
            }
        }
    }

}
