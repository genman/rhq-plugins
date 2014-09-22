package com.apple.iad.rhq.verdad;

/**
 * Thrown when expected verdad settings are missing.
 */
public class MissingSettingsException extends Exception {

    private static final long serialVersionUID = 1L;

    public MissingSettingsException(String string) {
        super(string);
    }

}
