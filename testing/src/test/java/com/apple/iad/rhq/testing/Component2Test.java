package com.apple.iad.rhq.testing;

import org.testng.annotations.Test;

/**
 * This test ensures that the test container can be re-used.
 * A bug occurred when @BeforeTest was used instead of @BeforeClass.
 */
@Test
public class Component2Test extends ComponentTest {

    public void test() throws Exception {
        assert hasComponent(TestComponentDiscovery.name);
        assert !hasComponent("not here");
        TestComponent component = (TestComponent) getComponent(TestComponentDiscovery.name);
        assertUp(component);
    }

}
