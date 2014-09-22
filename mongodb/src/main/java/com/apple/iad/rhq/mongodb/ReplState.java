package com.apple.iad.rhq.mongodb;

/**
 * Replication states.
 */
public enum ReplState {
    STARTUP, // 0
    PRIMARY,
    SECONDARY,
    RECOVERING,
    FATAL,
    STARTUP2, // 5
    UNKNOWN,
    ARBITER,
    DOWN,
    ROLLBACK,
    SHUNNED, // 10
}
