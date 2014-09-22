package com.apple.iad.rhq.port;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Queries groups of MBeans and sums their values.
 */
public class PortComponent
    implements MeasurementFacet, ResourceComponent
{

    /**
     * Subclasses are free to use this directly as a way to log messages.
     */
    private static Log log = LogFactory.getLog(PortComponent.class);

    private InetSocketAddress isa;

    private volatile int elapsed;

    /**
     * Stores the context and loads the MBean.
     */
    public void start(ResourceContext context) throws Exception {
        String key = context.getResourceKey();
        try {
            isa = PortDiscovery.parse(key);
        } catch (Exception e) {
            throw new IllegalStateException("could not parse key " + key, e);
        }
    }

    public void stop() {
        isa = null;
    }

    /**
     * Returns UP if beans exist in this group.
     */
    public AvailabilityType getAvailability() {
        try {
            long start = System.nanoTime();
            PortDiscovery.probe(isa);
            long end = System.nanoTime();
            elapsed = (int) (end - start);
            return UP;
        } catch (IOException e) {
            log.debug("check avail " + isa + " failed: " + e);
            return DOWN;
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();
            if (name.equals("connectTime")) {
                report.addData(new MeasurementDataNumeric(request, (double) elapsed));
            }
        }
    }

}
