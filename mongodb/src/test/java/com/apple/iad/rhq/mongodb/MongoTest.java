package com.apple.iad.rhq.mongodb;

import static org.testng.AssertJUnit.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.clientapi.descriptor.plugin.MetricDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

/**
 * Runs test of MongoDB plugin.
 * <p/>
 * Test depends on mongodb already running on the default part.
 */
@Test
public class MongoTest extends ComponentTest {

    private static final String main = "MongoDBServer";
    private static final String name = "mongodb local";

    @Override
    protected void before() throws Exception {
        super.before();
        if (!hasComponent(main))
            manuallyAdd(main);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
    }

    public void testMain() throws Exception {
        ResourceComponent component = getComponent(main);
        MeasurementReport report = getMeasurementReport(component);
        log.info("got " + map(report));
    }

    public void testDB() throws Exception {
        ResourceComponent component = getComponent(name);
        assertUp(component);
        MeasurementReport report = getMeasurementReport(component);
        ResourceDescriptor resourceDescriptor = getResourceDescriptor("MongoDB");
        log.info("measurements " + report.getNumericData());
        log.info("measurements " + report.getTraitData());
        String ignore[] = {
                "indexSize",
                "nsSizeMB",
        };
        HashMap<String, MetricDescriptor> map = new HashMap<String, MetricDescriptor>();
        for (MetricDescriptor md : resourceDescriptor.getMetric()) {
            map.put(md.getProperty(), md);
        }
        for (MeasurementDataNumeric n : report.getNumericData()) {
            map.remove(n.getName());
        }
        for (MeasurementDataTrait n : report.getTraitData()) {
            map.remove(n.getName());
        }
        for (String s : ignore) {
            map.remove(s);
        }
        assertTrue("Measurements not found " + map.keySet(), map.isEmpty());

        log.info("test get");
        for (ResourceComponent c : components.keySet()) {
            Set<MeasurementScheduleRequest> metrics = new HashSet<MeasurementScheduleRequest>();
            ((MeasurementFacet)c).getValues(report, metrics);
        }
    }

    /*
    public void testComponent() throws Exception {
        ResourceComponent component = getComponent(name2);
        assertUp(component);
        MeasurementReport report = getMeasurementReport(component);
        log.info("got " + map(report));
    }
    */

}
