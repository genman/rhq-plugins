package com.apple.iad.rhq.snmp;

import static org.snmp4j.mp.SnmpConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.snmp4j.smi.OID;

/**
 * Mapping of error OIDs to String defined errors.
 */
public class Errors {

    private static final Map<OID, String> map = new HashMap<OID, String>();

    private Errors() { }

    static {
        add(usmStatsUnsupportedSecLevels, "usmStatsUnsupportedSecLevels");
        add(usmStatsNotInTimeWindows, "usmStatsNotInTimeWindows");
        add(usmStatsUnknownUserNames, "usmStatsUnknownUserNames");
        add(usmStatsUnknownEngineIDs, "usmStatsUnknownEngineIDs");
        add(usmStatsWrongDigests, "usmStatsWrongDigests");
        add(usmStatsDecryptionErrors, "usmStatsDecryptionErrors");
        add(snmpUnknownSecurityModels, "snmpUnknownSecurityModels");
        add(snmpInvalidMsgs, "snmpInvalidMsgs");
        add(snmpUnknownPDUHandlers, "snmpUnknownPDUHandlers");
    }

    /**
     * Convert an error OID into a string.
     */
    public static String get(OID oid) {
        return map.get(oid);
    }

    private static void add(OID oid, String string) {
        map.put(oid, string);
    }

}
