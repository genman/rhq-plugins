package com.apple.iad.rhq.testing;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Test component.
 */
public class TestComponent implements ResourceComponent, MeasurementFacet {

    private final Log log = LogFactory.getLog(getClass());

    protected AvailabilityType avail = AvailabilityType.UP;

    private String trait = "trait";

    private ResourceContext resourceContext;

    /**
     * Starts this instance.
     */
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        log.info("start");
        this.resourceContext = resourceContext;
    }

    @Override
    public void stop() {
        log.info("stop");
    }

    public AvailabilityType getAvailability() {
        log.info("avail");
        return avail;
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> msrs) throws Exception {
        log.info("getValues");
        for (MeasurementScheduleRequest msr : msrs) {
            mr.addData(new MeasurementDataTrait(msr, trait));
        }
    }

    public ResourceContext getResourceContext() {
        return resourceContext;
    }

    public void pushEvent() {
        EventSeverity severity = EventSeverity.ERROR;
        Event event = new Event("test", "loc", System.currentTimeMillis(), severity, "detail");
        resourceContext.getEventContext().publishEvent(event);
    }

}
