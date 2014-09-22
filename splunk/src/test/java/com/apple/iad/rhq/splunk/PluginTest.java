package com.apple.iad.rhq.splunk;

import static org.testng.AssertJUnit.assertNotNull;

import java.net.URL;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

/**
 * To test, run an instance of Splunk or use SSH port forwarding.
 * Splunk runs on port 8089.
 */
public class PluginTest extends ComponentTest {

    @Test
    public void measurements() throws Exception {
        log.info("available");
        ResourceComponent component;
        if (hasComponent("Splunk")) {
            component = getComponent("Splunk");
        } else {
            ResourceType rt = getResourceType("Splunk");
            Configuration conf = getConfiguration(rt);
            URL url = getClass().getResource("/metrics.log");
            conf.setSimpleValue("home", url.getPath());
            conf.setSimpleValue("metricsLog", "");
            component = manuallyAdd(rt, conf);
        }
        assertUp(component);
        MeasurementReport mr = getMeasurementReport(component);
        Map<String, Object> map = map(mr);
        log.debug(map);
        assertNotNull(map.get(MetricsTail.RECENCY));
        double d = (Double) map.get(MetricsTail.RECENCY);
        assert d > 0;
        log.info(map);
        String version = getResource(component).getVersion();
        log.info(version);
    }

}
