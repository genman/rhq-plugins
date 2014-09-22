package com.apple.iad.rhq.jmx;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * Discovers applications based on the MBean name.
 */
public class ApplicationMBeansDiscoveryComponent implements ResourceDiscoveryComponent<JMXComponent<?>> {

    private static final String BEANS_QUERY_STRING = "beansQueryString";
    private static final Log LOG = LogFactory.getLog(ApplicationMBeansDiscoveryComponent.class);

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent<?>> context)
            throws Exception {

        JMXComponent<?> component = context.getParentResourceComponent();
        Configuration pluginConfig = context.getDefaultPluginConfiguration();
        EmsConnection emsConnection = component.getEmsConnection();
        if (!hasApplicationMBeans(pluginConfig, emsConnection)) {
            return Collections.emptySet();
        }

        return Collections.singleton(new DiscoveredResourceDetails(context.getResourceType(),
                getResourceKey(context, pluginConfig),
                getResourceName(context, pluginConfig),
                getResourceVersion(context, pluginConfig),
                getResourceDescription(context, pluginConfig), pluginConfig, null));
    }

    protected boolean hasApplicationMBeans(Configuration pluginConfig, EmsConnection emsConnection) {
        String beansQueryString = getBeansQueryString(pluginConfig);
        List<EmsBean> beans = emsConnection.queryBeans(beansQueryString);
        if (beans.isEmpty()) {
            LOG.debug("Found no MBeans with query '" + beansQueryString + "'");
            return false;
        } else {
            LOG.debug("Found MBeans " + beans);
        }
        return true;
    }

    /**
     * Returns a query string for config name {@link #BEANS_QUERY_STRING}.
     */
    protected String getBeansQueryString(Configuration pluginConfig) {
        String s = pluginConfig.getSimpleValue(BEANS_QUERY_STRING);
        if (s == null)
            throw new IllegalStateException(BEANS_QUERY_STRING + " not set");
        return s;
    }

    /**
     * Returns a resource key for config name 'resourceKey', default is 'key'.
     */
    protected String getResourceKey(ResourceDiscoveryContext<JMXComponent<?>> context, Configuration config) {
        return config.getSimpleValue("resourceKey", "key");
    }

    /**
     * Returns a resource name for config key 'resourceName', default is the resource type name.
     */
    protected String getResourceName(ResourceDiscoveryContext<JMXComponent<?>> context, Configuration config) {
        return config.getSimpleValue("resourceName", context.getResourceType().getName());
    }

    /**
     * Returns a resource description for config key 'resourceName', default is the resource type description.
     */
    protected String getResourceDescription(ResourceDiscoveryContext<JMXComponent<?>> context, Configuration config) {
        return config.getSimpleValue("resourceDescription", context.getResourceType().getDescription());
    }

    /**
     * Returns a resource version for config key 'resourceVersion', default is null.
     */
    protected String getResourceVersion(ResourceDiscoveryContext<JMXComponent<?>> context, Configuration config) {
        return config.getSimpleValue("resourceVersion", null);
    }

}
