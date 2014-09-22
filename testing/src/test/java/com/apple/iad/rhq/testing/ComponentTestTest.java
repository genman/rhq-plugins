package com.apple.iad.rhq.testing;

import static org.testng.AssertJUnit.assertEquals;

import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.TestPluginLifecycleListener.State;

/**
 * Validates some simple things about {@link ComponentTest}
 */
@Test
public class ComponentTestTest extends ComponentTest {

    @Override
    protected void before() throws Exception {
        TestPluginLifecycleListener.state = State.NEW;
        super.before();
    }

    @Override
    protected void afterClass() throws Exception {
        super.afterClass();
        assertEquals(State.SHUTDOWN, TestPluginLifecycleListener.state);
    }

    public void test() throws Exception {
        TestComponent component = (TestComponent) getComponent(TestComponentDiscovery.name);
        assertUp(component);
        assert getResource(component) != null;

        log.debug("push event");
        TestEventContext eventContext = getEventContext(component);
        assertEquals(0, eventContext.getEvents().size());
        component.pushEvent();
        assertEquals(1, eventContext.getEvents().size());

        log.debug("measurements");
        MeasurementReport report = getMeasurementReport(component);
        ResourceDescriptor rd = getResourceDescriptor("Test Name");
        assertAll(report, rd);
        String simpleValue = component.getResourceContext().getPluginConfiguration().getSimpleValue("simple", null);
        assert "value".equals(simpleValue);
        component.avail = AvailabilityType.DOWN;
        assertDown(component);
    }

    @Test(expectedExceptions = { AssertionError.class })
    public void noDescriptor() {
        getResourceDescriptor("Invalid");
    }

    @Test(expectedExceptions = { AssertionError.class })
    public void noComponent() {
        getComponent("Invalid");
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        set(configuration, "simple", "value");
    }

}
