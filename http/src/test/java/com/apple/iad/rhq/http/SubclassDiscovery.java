package com.apple.iad.rhq.http;

import java.net.URL;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.apple.iad.rhq.http.HttpDiscovery;

public class SubclassDiscovery extends HttpDiscovery {

    public static final String VER = "v1";
    public static final String KEY = "key";
    public static final String NAME = "name";

    @Override
    protected int getDiscoveryTimeout() {
        return 100;
    }

    @Override
    protected String getVersion(ResourceDiscoveryContext context, String ver, URL url) {
        return VER;
    }

    @Override
    protected String getResourceKey(ResourceDiscoveryContext context, URL url) {
        return KEY;
    }

    @Override
    protected String getResourceName(ResourceDiscoveryContext context, URL url) {
        return NAME;
    }

}
