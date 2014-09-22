package com.apple.iad.rhq.memcached;

import static org.testng.AssertJUnit.assertEquals;

import java.net.InetSocketAddress;

import org.testng.annotations.Test;

import com.apple.iad.rhq.memcached.MemcachedDiscovery;

/**
 * Tests discovery stuff.
 */
@Test
public class DiscoveryTest {

    public void test() {
        MemcachedDiscovery md = new MemcachedDiscovery();
        String arg = "/usr/bin/memcached -d -m 1024 -c 3072 -l 127.0.0.1 -u api";
        InetSocketAddress address = md.address(arg.split(" "));
        assertEquals(MemcachedDiscovery.DEFAULT_PORT, address.getPort());
        assertEquals("127.0.0.1", address.getAddress().getHostAddress());

        arg = "-l 127.0.0.1:9999";
        address = md.address(arg.split(" "));
        assertEquals(9999, address.getPort());

        arg = "";
        address = md.address(arg.split(" "));
        assertEquals(MemcachedDiscovery.DEFAULT_PORT, address.getPort());

        arg = "-p 7777";
        address = md.address(arg.split(" "));
        assertEquals(7777, address.getPort());
    }

}
