package com.apple.iad.rhq.verdad;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovers a monitor service from Verdad side.
 */
public class MonitorDiscovery implements ResourceDiscoveryComponent<VerdadComponent> {

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Verdad setting 'version'.
     */
    public static final String VERSION = "version";

    /**
     * Verdad setting 'description'
     */
    private static final String DESCRIPTION = "description";

    /**
     * Locates all the iad.monitor.service.* elements in the Verdad tree.
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<VerdadComponent> context) {
        Set<DiscoveredResourceDetails> drds = new HashSet<DiscoveredResourceDetails>();
        VerdadComponent vc = context.getParentResourceComponent();
        Map d = vc.getVerdad();
        if (d == null) {
            log.debug("unable to discover; no verdad info");
            return drds;
        }
        MonitorComponent mc = new MonitorComponent(vc);
        Map<String, Map> mon = mc.monitorNode(d);
        if (mon == null) {
            log.debug("no monitoring element indicated for this host");
            return drds;
        }
        Map<String, List> service = mon.get("service");
        if (service == null) {
            log.debug("no monitoring service element for this host");
            return drds;
        }
        for (Entry<String, List> e : service.entrySet()) {
            String name = e.getKey();
            String desc;
            String ver;
            try {
                MonitorComponent sc = new MonitorComponent(vc, name);
                log.info(name + " discovered with " + sc.getSettings());
                desc = sc.setting(DESCRIPTION, null);
                ver = sc.setting(VERSION, null);
                if (!sc.settingTrue(MonitorComponent.ENABLED)) {
                    log.debug("disabled " + name);
                    continue;
                }
            } catch (MissingSettingsException ex) {
                throw new IllegalStateException(ex);
            }
            // We could capture the args, paths, and other stuff, in a plugin config
            // Then we wouldn't depend on verdad at startup.
            // Small risk I think...
            DiscoveredResourceDetails drd = new DiscoveredResourceDetails(context.getResourceType(),
                    name, // key
                    name, // name
                    ver, desc, context.getDefaultPluginConfiguration(),
                    null);
            drds.add(drd);
        }
        return drds;
    }

}
