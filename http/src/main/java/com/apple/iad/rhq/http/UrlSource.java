package com.apple.iad.rhq.http;

import java.io.IOException;
import java.net.URL;

/**
 * Implemented by components that return a URL used for monitoring this resource.
 */
public interface UrlSource {

    /**
     * Returns a URL.
     *
     * @throws IOException if the URL cannot be created
     */
    URL getUrl() throws IOException;

}
