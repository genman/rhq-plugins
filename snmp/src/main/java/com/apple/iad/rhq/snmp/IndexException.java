package com.apple.iad.rhq.snmp;

/**
 * Thrown indicating a name or OID wasn't found in the index,
 * or some other indexing issue happened.
 */
public class IndexException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception.
     */
    public IndexException() {
    }

    /**
     * Constructs a new exception.
     */
    public IndexException(String paramString) {
        super(paramString);
    }

    /**
     * Constructs a new exception.
     */
    public IndexException(Throwable paramThrowable) {
        super(paramThrowable);
    }

    /**
     * Constructs a new exception.
     */
    public IndexException(String paramString, Throwable paramThrowable) {
        super(paramString, paramThrowable);
    }

}
