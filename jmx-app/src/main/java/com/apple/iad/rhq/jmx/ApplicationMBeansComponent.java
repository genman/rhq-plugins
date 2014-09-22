package com.apple.iad.rhq.jmx;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * For grouping JMX MBeans under one application.
 * Similar to the {@link ApplicationMBeansComponent} for JBoss 7.
 */
public class ApplicationMBeansComponent implements ResourceComponent<JMXComponent<?>>, JMXComponent<JMXComponent<?>> {

    private static final Log LOG = LogFactory.getLog(ApplicationMBeansComponent.class);

    /**
     * Parent component.
     */
    protected JMXComponent parent;

    /**
     * ObjectName query.
     */
    protected String beansQueryString;

    /**
     * This resource's context.
     */
    protected ResourceContext resourceContext;

    @Override
    public void start(ResourceContext<JMXComponent<?>> resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        this.parent = resourceContext.getParentResourceComponent();
        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        this.beansQueryString = new ApplicationMBeansDiscoveryComponent().getBeansQueryString(pluginConfig);
    }

    @Override
    public void stop() {
        parent = null;
    }

    @Override
    public EmsConnection getEmsConnection() {
        return parent.getEmsConnection();
    }

    @Override
    public AvailabilityType getAvailability() {
        EmsConnection connection = getEmsConnection();
        if (connection == null) {
            return DOWN;
        }
        if (!hasApplicationMBeans()) {
            LOG.warn("Found no MBeans with query '" + beansQueryString + "'");
            return DOWN;
        }
        return UP;
    }

    /**
     * Returns true if application MBeans are available.
     */
    protected boolean hasApplicationMBeans() {
        return !getEmsConnection().queryBeans(beansQueryString).isEmpty();
    }

}
