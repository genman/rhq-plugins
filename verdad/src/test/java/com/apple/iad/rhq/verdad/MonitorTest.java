package com.apple.iad.rhq.verdad;

import static com.apple.iad.rhq.verdad.MonitorComponent.ELAPSED;
import static com.apple.iad.rhq.verdad.MonitorComponent.ENABLED;
import static com.apple.iad.rhq.verdad.MonitorComponent.GROUP;
import static com.apple.iad.rhq.verdad.MonitorComponent.OUTPUT;
import static java.util.Collections.singletonList;
import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

/**
 * Runs tests for Verdad and monitor components.
 */
public class MonitorTest extends ComponentTest {

    private final String name = "Verdad";
    private final File vdFile = new File(getClass().getResource("/vd.sh").getFile());
    private final File check_log = new File(getClass().getResource("/check_log").getFile());
    {
        vdFile.setExecutable(true);
        check_log.setExecutable(true);
    }

    @Test
    public void testAll() throws Exception {
        log.info("available");

        Map<String, Object> map;

        VerdadComponent vd = (VerdadComponent)getComponent(name);
        assertUp(vd);
        assertEquals("b53c509", getResource(name).getVersion());

        map = map(getMeasurementReport(vd));
        double elapsed = (Double)map.get(ELAPSED);
        assertTrue("elapsed " + elapsed, elapsed >= 0);

        assertEquals(6, vd.observerCount());

        MonitorComponent mc = new MonitorComponent(vd);
        Map<String, Map> monitor = mc.monitorNode(vd.getVerdad());
        Map<String, List> map2 = monitor.get("env");
        map2.put("PATH", singletonList("/bin:/usr/bin:" + check_log.getParent()));
        vd.refreshChildren();

        mc = (MonitorComponent) getComponent("abstruth");
        assertEquals(UP, mc.getAvailabilityCurrent());
        mc.toString();
        MeasurementReport mr = getMeasurementReport(mc);
        map = map(mr);
        assertEquals(null, map.get(OUTPUT));
        elapsed = (Double)map.get(ELAPSED);
        assertTrue("elapsed " + elapsed, elapsed >= 0);
        assertEquals("p", map.get(GROUP));
        noEvents(mc);

        MonitorComponent truth = (MonitorComponent) getComponent("truth");
        assertEquals("1.0", getResource(truth).getVersion());
        assertEquals(UP, truth.getAvailabilityCurrent());
        map = map(getMeasurementReport(truth));
        assertEquals("p", map.get(GROUP));

        mc = (MonitorComponent) getComponent("untrue");
        assertEquals(UP, mc.getAvailabilityCurrent());
        assertEquals(UP, mc.getAvailabilityCurrent());
        Event event = event(mc); // only one event
        assertEquals("exit code 1", event.getDetail());
        assertEquals(EventSeverity.WARN, event.getSeverity());

        log.info("check log return success");
        mc = (MonitorComponent) getComponent("check_log_null");
        assertEquals(UP, mc.getAvailabilityCurrent());
        map = map(getMeasurementReport(mc));
        assertEquals(null, map.get(OUTPUT));

        Configuration parameters = new Configuration();
        try {
            mc.invokeOperation("x", parameters);
            fail("unsupported");
        } catch (UnsupportedOperationException e) {}
        OperationResult or = mc.invokeOperation(MonitorComponent.INVOKE, parameters);
        assertEquals("0", or.getComplexResults().getSimpleValue("exitCode"));
        String out = or.getComplexResults().getSimpleValue("output");
        assertEquals(true, out.contains("okay"));

        log.info("check log return error");
        mc = (MonitorComponent) getComponent("check_log_bad");
        assertEquals(DOWN, mc.getAvailabilityCurrent());
        event = event(mc);
        assertEquals(EventSeverity.ERROR, event.getSeverity());
        assertEquals("event " + event, true, event.getDetail().contains("error"));

        log.debug("missing script");
        mc = (MonitorComponent) getComponent("check_mem");
        assertEquals("Checks", getResource(mc).getDescription());
        assertEquals(DOWN, mc.getAvailabilityCurrent());

        Map<String, List> e = MonitorComponent.serviceNode(monitor, "check_mem");
        assert e != null;
        log.debug("explictly disabled");
        e.put(ENABLED, singletonList("false"));
        vd.refreshChildren();
        assertEquals(UP, mc.getAvailabilityCurrent());
        map = map(getMeasurementReport(mc));
        assertEquals("false", map.get(ENABLED));

        log.debug("remove enabled element, can't tell, assume up");
        assert e.remove(ENABLED) != null;
        vd.refreshChildren();
        assertEquals(DOWN, mc.getAvailabilityCurrent());

        log.debug("removes the XML");
        vd.getVerdad().clear();
        vd.refreshChildren();
        assertEquals(UP, truth.getAvailability());
        assertEquals(UP, truth.getAvailabilityCurrent());
        log.debug("should see enable = false");
        map = map(getMeasurementReport(truth));
        assertEquals("false", map.get(ENABLED));

        log.debug("restart component no XML");
        truth.stop();
        truth.start(getContext(truth));
        assertEquals(UP, truth.getAvailability());
        map = map(getMeasurementReport(truth));
        assertEquals("false", map.get(ENABLED));

        log.debug("restart truth with no verdad");
        truth.stop();
        assertEquals(5, vd.observerCount());
        truth.start(getContext(truth));
        assertEquals("false", map.get(ENABLED));
        try {
            mc.invokeOperation(MonitorComponent.INVOKE, parameters);
            fail("invoke");
        } catch (MissingSettingsException ex) {}

        restart(vd);
        assertUp(vd);
    }

    private void noEvents(MonitorComponent mc) {
        assertEquals("no events", 0, getEventContext(mc).getEvents().size());
    }

    private Event event(MonitorComponent mc) {
        Collection<Event> events = getEventContext(mc).getEvents();
        assertEquals("has one event", 1, events.size());
        return events.iterator().next();
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(name)) {
            ConfigurationDefinition def = resourceType.getPluginConfigurationDefinition();
            Configuration conf = def.getDefaultTemplate().getConfiguration();
            conf.getSimple(VerdadComponent.EXECUTABLE).setStringValue(vdFile.toString());
            log.debug("" + conf.getAllProperties());
        }
    }

}
