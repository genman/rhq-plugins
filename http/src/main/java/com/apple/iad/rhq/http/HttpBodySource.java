package com.apple.iad.rhq.http;

import java.io.BufferedReader;

/**
 * Implemented by components that read the body of an HTTP page.
 */
public interface HttpBodySource {

    /**
     * Returns a reader for reading the body.
     * The body may be potentially streamed, please close it.
     * Returns an empty string upon the resource not being available.
     * This method should be called sparingly.
     */
    BufferedReader getBodyReader();

    /**
     * Returns the body as a string.
     * Returns an empty string upon the resource not being available.
     * The body may be optionally cached (using a timer or otherwise) to reduce service load.
     */
    String getBody();

    /**
     * Returns the measurement provider for this HTTP body, which is refreshed
     * every time the body itself is refreshed.
     *
     * @throws Exception if the body could not be parsed
     */
    MeasurementProvider getMeasurementProvider() throws Exception;
}
