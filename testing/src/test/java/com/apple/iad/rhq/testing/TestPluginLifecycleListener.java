package com.apple.iad.rhq.testing;

import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;

public class TestPluginLifecycleListener implements PluginLifecycleListener {

    enum State { NEW, INIT, SHUTDOWN };
    static State state = State.NEW;

    @Override
    public void initialize(PluginContext pc) throws Exception {
        assert state == State.NEW;
        assert pc != null;
        state = State.INIT;
    }

    @Override
    public void shutdown() {
        assert state == State.INIT;
        state = State.SHUTDOWN;
    }

}
