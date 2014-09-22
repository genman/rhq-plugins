package com.apple.iad.rhq.flume;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent.PROPERTY_OBJECT_NAME;

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * Queries groups of MBeans and sums their values.
 */
public class MBeanGroupComponent<T extends JMXComponent<?>>
    implements MeasurementFacet, ResourceComponent<T>
{

    /**
     * Subclasses are free to use this directly as a way to log messages.
     */
    private static Log log = LogFactory.getLog(MBeanGroupComponent.class);
    private JMXComponent parent;
    private String objectName;

    /**
     * Stores the context and loads the MBean.
     */
    public void start(ResourceContext<T> context) {
        objectName = context.getPluginConfiguration().getSimpleValue(
                PROPERTY_OBJECT_NAME, null);
        if (objectName == null)
            throw new NullPointerException(PROPERTY_OBJECT_NAME);
        parent = context.getParentResourceComponent();
    }

    public void stop() {
        parent = null;
    }

    /**
     * Returns UP if beans exist in this group.
     */
    public AvailabilityType getAvailability() {
        List<EmsBean> beans = parent.getEmsConnection().queryBeans(objectName);
        return (beans.isEmpty()) ? DOWN : UP;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        List<EmsBean> beans = parent.getEmsConnection().queryBeans(objectName);
        log.debug("found " + beans.size());
        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();
            double sum = 0;
            for (EmsBean bean : beans) {
                EmsAttribute attr = bean.getAttribute(name);
                if (attr == null || attr.getValue() == null) {
                    log.error(name + " not found");
                    continue;
                }
                Object value = attr.getValue();
                if (value instanceof Number) {
                    sum += ((Number)value).doubleValue();
                }
            }
            if (request.getDataType() == DataType.MEASUREMENT) {
                report.addData(new MeasurementDataNumeric(request, sum));
            }
        }
    }

}
