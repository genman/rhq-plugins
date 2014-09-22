package com.apple.iad.rhq.snmp;

import java.io.IOException;

/**
 * Thrown when no response received from SNMP server.
 * Could be caused when the response times out or invalid credentials were used.
 */
public class NoResponseException extends IOException {

    private static final long serialVersionUID = 1L;

    /**
     * No message or cause.
     */
    public NoResponseException() {
        super();
    }

    /**
     * Message and cause.
     */
    public NoResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Message.
     */
    public NoResponseException(String message) {
        super(message);
    }

    /**
     * Cause.
     */
    public NoResponseException(Throwable cause) {
        super(cause);
    }

}
