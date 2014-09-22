package com.apple.iad.rhq.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Parses and provides measurements from an HTTP body.
 */
public abstract class MeasurementProvider {

    /**
     * HTTP request body.
     */
    protected final String body;

    /**
     * Logging handle.
     */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Constructs a provider with a body.
     */
    public MeasurementProvider(String body) {
        this.body = body;
    }

    /**
     * Returns the measurement found or null if not found.
     * Depending on the format, may be a regular expression, JSon key-value key, XPath, etc.
     */
    public abstract Object extractValue(String metricPropertyName);

    /**
     * Return the body parsed by this provider.
     */
    public String getBody() {
        return body;
    }

}
