package com.apple.iad.rhq.hadoop;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

public class NameNodeInfoComponentTest {

    @Test
    public void testCount() {
        NameNodeInfoComponent c = new NameNodeInfoComponent();
        assert c.count("{}") == 0;
        assertEquals(2, c.count("{\"foo\":{}, \"bar\":{}}"));
    }
}
