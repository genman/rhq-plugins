package com.apple.iad.rhq.tomcat;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jboss.on.plugins.tomcat.TomcatServerComponent;
import org.jboss.on.plugins.tomcat.helper.TomcatConfig;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.apple.iad.rhq.http.HttpComponent;

/**
 * Extracts the HTTP port value for a service running within Tomcat.
 */
public class TomcatHttpComponent extends HttpComponent<TomcatServerComponent<?>> {

    private static final String PORT_TOKEN = "@PORT@";
    private String port = null;
    private URL url;

    public TomcatHttpComponent() {
    }

    /**
     * Constructor used for discovery.
     */
    public TomcatHttpComponent(Configuration configuration, ResourceDiscoveryContext context) throws IOException {
        super(configuration, context);
        setPort(context.getParentResourceComponent());
        String urlStr = getConfiguration().getSimpleValue(PLUGINCONFIG_URL, "");
        this.url = createUrl(urlStr);
    }

    private void setPort(ResourceComponent parent) {
        log.debug("setPort " + parent);
        if (parent instanceof TomcatServerComponent) {
            TomcatServerComponent ts = (TomcatServerComponent) parent;
            File f = new File(ts.getCatalinaBase(), "conf" + File.separatorChar + "server.xml");
            if (!f.exists()) {
                log.warn("not found: " + f);
            }
            TomcatConfig config = TomcatConfig.getConfig(f);
            port = config.getPort();
        }
    }

    @Override
    public void start(ResourceContext context) throws IOException {
        super.start(context);
        setPort(context.getParentResourceComponent());
        String url = getConfiguration().getSimpleValue(PLUGINCONFIG_URL, null);
        this.url = createUrl(url);
    }

    /**
     * Return a resolved URL, replacing {@link #PORT_TOKEN}.
     */
    private URL createUrl(String url) throws IOException {
        log.debug("port " + port);
        // The port may be null, in which case use ""
        if (port == null)
            port = "";

        if (url != null) {
            url = url.replace(PORT_TOKEN, port);
            return new URL(url);
        } else {
            return null;
        }
    }

    /**
     * Returns the created URL.
     */
    @Override
    public URL getUrl() throws IOException {
        return url;
    }

}
