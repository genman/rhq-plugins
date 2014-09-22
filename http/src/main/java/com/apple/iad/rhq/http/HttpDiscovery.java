package com.apple.iad.rhq.http;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * This discovery component can be used to find HTTP services in the current host.
 *
 * @author Elias Ross
 */
public class HttpDiscovery<T extends ResourceComponent<?>> implements ResourceDiscoveryComponent<T>, ManualAddFacet<T> {

    private final Log log = LogFactory.getLog(HttpDiscovery.class);

    /**
     * Default timeout for checking if a URL is available or not.
     */
    private static final int TIMEOUT = 500;

    /**
     * Returns the discovery (automatic, not manual) timeout for this URL.
     */
    protected int getDiscoveryTimeout() {
        return TIMEOUT;
    }

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> context)
        throws Exception {

        log.debug("Processing discovered http resources");

        Set<DiscoveredResourceDetails> discoveredServers = new HashSet<DiscoveredResourceDetails>();
        URL url = null;
        try {
            Configuration configuration = context.getDefaultPluginConfiguration();
            HttpComponent httpComponent = createHttpComponent(configuration, context);
            httpComponent.setTimeout(getDiscoveryTimeout());
            url = httpComponent.getUrl();
            // don't add unless 'true'
            if (httpComponent.testUrl(url)) {
                String ver = httpComponent.getVersion();

                DiscoveredResourceDetails server = new DiscoveredResourceDetails(
                        context.getResourceType(),
                        getResourceKey(context, url),
                        getResourceName(context, url),
                        getVersion(context, ver, url),
                        getResourceDescription(context, url),
                        configuration, null);
                discoveredServers.add(server);
            }
        } catch (ConnectException e) {
            log.debug("did not discover " + url + " " + e);
        } catch (IOException e) {
            log.info("did not discover " + url + " " + e);
            log.debug(e, e);
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException(e);
        }

        return discoveredServers;
    }

    /**
     * Obtains the version of the HTTP service; returns the HTTP header 'server' for now.
     *
     * @param context discovery context
     * @param ver HTTP server header
     * @param url discovery URL
     */
    protected String getVersion(ResourceDiscoveryContext<T> context, String ver, URL url) {
        return ver;
    }

    @Override
    public DiscoveredResourceDetails discoverResource(
            Configuration configuration, ResourceDiscoveryContext<T> context)
            throws InvalidPluginConfigurationException {
        try {
            HttpComponent httpComponent = createHttpComponent(configuration, context);
            URL url = httpComponent.getUrl();
            if (!httpComponent.testUrl(url)) {
                String r = httpComponent.getResponseCode() + " " + httpComponent.getResponseMessage();
                throw new InvalidPluginConfigurationException("Failed to access " + url + ": " + r);
            }
            String ver = httpComponent.getVersion();

            DiscoveredResourceDetails server = new DiscoveredResourceDetails(
                    context.getResourceType(),
                    getResourceKey(context, url), // key
                    getResourceName(context, url), // name
                    getVersion(context, ver, url),
                    context.getResourceType().getDescription(),
                    configuration, null);
            return server;
        } catch (IOException e) {
            throw new InvalidPluginConfigurationException(e);
        }
    }

    /**
     * Returns the resource key of the resource; by default converts the URL to a string.
     * Subclasses may want to truncate dynamic query parameters or otherwise.
     */
    protected String getResourceKey(ResourceDiscoveryContext<T> context, URL url) {
        return url.toString();
    }

    /**
     * Returns the name of the resource; by default converts the URL to a string.
     * Subclasses may want to specify a shorter or different name.
     */
    protected String getResourceName(ResourceDiscoveryContext<T> context, URL url) {
        return context.getResourceType().getName() + " " + url;
    }

    /**
     * Return the HTTP component used for discovery.
     * By default, returns {@link HttpComponent} initialized with this context.
     *
     * @param configuration plugin configuration
     * @param context discovery context
     */
    protected HttpComponent createHttpComponent(Configuration configuration, ResourceDiscoveryContext<T> context) throws IOException {
        return new HttpComponent(configuration, context);
    }

    /**
     * Provide the resource description for this url; by default returns the resource type
     * description.
     * Subclasses may want to specify a shorter or different name.
     */
    protected String getResourceDescription(ResourceDiscoveryContext<T> context, URL url) {
        return context.getResourceType().getDescription();
    }

}
