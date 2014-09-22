package com.apple.iad.rhq.memcached;

import net.spy.memcached.MemcachedClient;

import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.testng.annotations.Test;

import com.apple.iad.rhq.memcached.MemcachedComponent;
import com.apple.iad.rhq.testing.ComponentTest;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Runs test of memcached plugin.
 * <p/>
 * Test depends on memcached already running. Start with no options or port
 * 11211.
 */
@Test
public class MemcachedTest extends ComponentTest {

    private static final String name = "memcached 11211";
    private MemcachedComponent component;

    @Override
    protected void before() throws Exception {
        super.before();
        component = (MemcachedComponent) getComponent(name);
    }

    @Override
    protected void setConfiguration(Configuration configuration,
            ResourceType resourceType) {
    }

    public void test() throws Exception {
        MeasurementReport measurementReport = getMeasurementReport(component);
        ResourceDescriptor resourceDescriptor = getResourceDescriptor("Memcached");
        log.info("measurements " + measurementReport.getNumericData());
        log.info("measurements " + measurementReport.getTraitData());
        measurementReport = getMeasurementReport(component);
        // ratio not set yet
        // assertAll(measurementReport, resourceDescriptor);

        MemcachedClient c = new MemcachedClient(component.getAddress());
        String key = "foo";
        c.set(key, 0, "value").get();
        c.get(key);
        c.get(key);
        c.get(key + "_not");

        measurementReport = getMeasurementReport(component);
        assertAll(measurementReport, resourceDescriptor);
        Double d = (Double) map(measurementReport).get(
                MemcachedComponent.GET_RATIO);
        assertEquals(.66, d.doubleValue(), .1);
        measurementReport = getMeasurementReport(component);

        c.shutdown();
    }

    //manually adding config to test against lab host vp25q03ad-app012.iad.apple.com 
    public void testManual() throws Exception {
        Configuration config = new Configuration();
        config.setSimpleValue(MemcachedComponent.HOSTNAME, "17.176.211.32");
        config.setSimpleValue(MemcachedComponent.PORT, "11212");
        ResourceType type = getResourceType("Memcached");
        manuallyAdd(type, config);
        MeasurementReport mr = getMeasurementReport(component);
        log.info("metrics " + map(mr));
    }

}
