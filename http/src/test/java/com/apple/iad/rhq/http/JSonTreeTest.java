package com.apple.iad.rhq.http;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.apple.iad.rhq.http.JSONTreeProvider;

@Test
public class JSonTreeTest {

    public void test() throws Exception {
        String body = "{ \"body\": { \"title\": 69 } }";
        JSONTreeProvider p = new JSONTreeProvider(body);
        Object x = p.extractValue("/body/title");
        assert x != null;
        assertEquals(new Long(69), x);
    }

    public void testList() throws Exception {
        String body = "{ \"body\": [ 42, \"aaa\", [ 1, 55 ] ] }";
        JSONTreeProvider p = new JSONTreeProvider(body);
        Object x = p.extractValue("/body/0");
        assert x != null;
        assertEquals(new Long(42), x);
        x = p.extractValue("/body/1");
        assert x != null;
        assertEquals("aaa", x);
        x = p.extractValue("/body/2/1");
        assertEquals(new Long(55), x);
    }

}
