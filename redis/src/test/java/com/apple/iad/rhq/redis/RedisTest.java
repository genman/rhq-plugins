package com.apple.iad.rhq.redis;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.rhq.core.clientapi.descriptor.plugin.MetricDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

import redis.clients.jedis.Protocol;

/**
 * Runs test of redis plugin.
 * <p/>
 * Test depends on redis already running. Start with no options.
 */
@Test
public class RedisTest extends ComponentTest {

    private static final String name = "redis 6379";
    private ResourceComponent component;

    @Override
    protected void before() throws Exception {
        super.before();
        component = getComponent(name);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
    }

    public void testPort() throws IOException {
        File f = new File(getClass().getResource("/redis.conf").getFile());
        assertEquals(666, RedisDiscovery.port(f));
        f = new File(getClass().getResource("/log4j.xml").getFile());
        assertEquals(Protocol.DEFAULT_PORT, RedisDiscovery.port(f));
    }

    public void test() throws Exception {
        MeasurementReport report = getMeasurementReport(component);
        ResourceDescriptor resourceDescriptor = getResourceDescriptor("Redis");
        log.info("measurements " + report.getNumericData());
        log.info("measurements " + report.getTraitData());
        String ignore[] = {
                "aof_base_size",
                "aof_buffer_length",
                "aof_current_size",
                "aof_delayed_fsync",
                "aof_pending_bio_fsync",
                "aof_pending_rewrite",
                "aof_rewrite_buffer_length",
                "master_host",
                "master_last_io_seconds_ago",
                "master_link_status",
                "master_port",
                "master_sync_in_progress",
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
    }


}
