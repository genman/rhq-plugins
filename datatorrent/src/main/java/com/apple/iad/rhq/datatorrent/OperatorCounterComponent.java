package com.apple.iad.rhq.datatorrent;

import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Sub-component of a physical operator. Represents one application counter.
 */
public class OperatorCounterComponent implements ResourceComponent<OperatorComponent>, MeasurementFacet {

    /**
     * Key for 'counters' in JSON tree.
     */
    public static final String COUNTERS = "counters";

    private OperatorComponent component;
    private String key;

    @Override
    public AvailabilityType getAvailability() {
        try {
            component.getDetailTree();
            return AvailabilityType.UP;
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
    }

    @Override
    public void start(ResourceContext<OperatorComponent> context) throws Exception {
        component = context.getParentResourceComponent();
        key = context.getResourceKey();
    }

    @Override
    public void stop() {
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> msr) throws Exception {
        Map<String, Object> details = component.getDetailTree();
        Map<String, String> counters = (Map<String, String>) details.get(COUNTERS);
        for (MeasurementScheduleRequest request : msr) {
            String name = request.getName();
            String value = counters.get(name);
            if (value != null) {
                mr.addData(new MeasurementDataNumeric(request, Double.parseDouble(value)));
            }
        }
    }

    @Override
    public String toString() {
        return "OperatorCounterComponent [key=" + key + "]";
    }

}