package com.apple.iad.rhq.oozie;

import org.rhq.core.domain.measurement.AvailabilityType;

import com.apple.iad.rhq.http.HttpComponent;
import com.apple.iad.rhq.http.JSONTreeProvider;
import com.apple.iad.rhq.http.MeasurementProvider;

/**
 * Holds all job information.
 */
public class JobsComponent extends HttpComponent {

    /**
     * For this, we simply avoid fetching all the jobs, as
     * it can cause timeouts.
     */
    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    @Override
    protected MeasurementProvider getMeasurementProvider(String body) throws Exception {
        return new JSONTreeProvider(body);
    }

}
