package com.apple.iad.rhq.datatorrent;

import static java.util.Collections.emptyMap;

import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

abstract class AppSubComponent implements ResourceComponent<AppComponent>, MeasurementFacet {

    protected final Log log = LogFactory.getLog(getClass());
    protected AppComponent app;
    protected String key;
    protected Map<String, Object> stats = emptyMap();

    public AppSubComponent() {
        super();
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> msr) throws Exception {
        getAvailability(); // loads stats
        for (MeasurementScheduleRequest r : msr) {
            String name = r.getName();
            Object val = stats.get(name);
            if (val == null) {
                continue;
            }
            if (r.getDataType() == DataType.MEASUREMENT) {
                Number n;
                if (name.endsWith("WindowId")) {
                    n = AppComponent.windowId(val);
                } else {
                    n = Double.parseDouble(val.toString());
                }
                mr.addData(new MeasurementDataNumeric(r, n.doubleValue()));
            } else {
                mr.addData(new MeasurementDataTrait(r, val.toString()));
            }
        }
    }

    @Override
    public void start(ResourceContext<AppComponent> ac) throws InvalidPluginConfigurationException, Exception {
        app = ac.getParentResourceComponent();
        key = ac.getResourceKey();
    }

    @Override
    public void stop() {
        stats = emptyMap();
    }

    /**
     * Returns the application ID.
     */
    protected String getAppId() {
        return app.getId();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +" [key=" + key + ", stats=" + stats + "]";
    }

}