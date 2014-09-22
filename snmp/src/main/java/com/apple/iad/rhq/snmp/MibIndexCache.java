package com.apple.iad.rhq.snmp;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Caches instances of a single {@link MibIndex} instance containing all the loaded
 * MIBs so far.
 * By default, 'SNMPv2-MIB' is loaded.
 */
public class MibIndexCache {

    private static MibIndex index = new MibIndex();
    private static Set<String> files = new HashSet<String>();
    static {
        try {
            load("SNMPv2-MIB");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads a MIB file.
     */
    public static synchronized void load(String mibFile) throws IOException {
        if (files.add(mibFile))
            index.load(mibFile);
    }

    /**
     * Return the cached index.
     * Note: Should not call 'load' when index is being accessed.
     */
    public static synchronized MibIndex getIndex() {
        return index;
    }

}
