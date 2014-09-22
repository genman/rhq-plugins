package com.apple.iad.rhq.snmp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.percederberg.mibble.MibSymbol;
import net.percederberg.mibble.MibType;
import net.percederberg.mibble.MibTypeSymbol;
import net.percederberg.mibble.MibValueSymbol;
import net.percederberg.mibble.snmp.SnmpAccess;
import net.percederberg.mibble.type.IntegerType;
import net.percederberg.mibble.value.NumberValue;

import org.snmp4j.smi.OID;

/**
 * MIB index name record.
 * Immutable.
 */
public class NameRecord {

    private final OID oid;
    private final OID oid0;
    private final MibType syntax;
    private final SnmpAccess access;
    private final List<OID> index;
    private final String desc;
    private final Map<Integer, String> mapping;

    /**
     * Constructs a name record.
     */
    public NameRecord(OID oid, MibType syntax, SnmpAccess snmpAccess, MibTypeSymbol typeSymbol, List<OID> index, String desc) {
        this.oid = oid;
        this.oid0 = new OID(oid).append(0);
        this.syntax = syntax;
        this.access = snmpAccess;
        this.index = index;
        this.desc = desc;

        if (typeSymbol != null && typeSymbol.getType() instanceof IntegerType) {
            MibType type = typeSymbol.getType();
            mapping = new HashMap<Integer, String>();
            IntegerType it = (IntegerType) type;
            for (MibValueSymbol mvs : it.getAllSymbols()) {
                NumberValue value = (NumberValue)mvs.getValue();
                Number n = (Number) value.toObject();
                String name = mvs.getName().intern();
                mapping.put(n.intValue(), name);
            }
        } else if (syntax instanceof IntegerType && ((IntegerType) syntax).hasSymbols()) {
            mapping = new HashMap<Integer, String>();
            for (MibSymbol s : ((IntegerType) syntax).getAllSymbols()) {
                MibValueSymbol vs = (MibValueSymbol)s;
                String name = vs.getName().intern();
                Number n = (Number) vs.getValue().toObject();
                mapping.put(n.intValue(), name);
            }
        } else {
            mapping = null;
        }
    }

    /**
     * Returns the OID for this.
     */
    public OID getOid() {
        return oid;
    }

    /**
     * Returns the syntax type symbol for this record.
     */
    public MibType getSyntax() {
        return syntax;
    }

    /**
     * Returns a list of OIDs for table index columns; null if not a table.
     */
    public List<OID> getIndex() {
        return index;
    }

    /**
     * Returns the description of this entry.
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Returns the SNMP access, which may be null.
     */
    public SnmpAccess getAccess() {
        return access;
    }

    /**
     * Return OID with 0 appended to it; for scalar variables.
     */
    public OID getOid0() {
        return oid0;
    }

    /**
     * Return the integer mapping for this name record.
     * Note: May be indexed by type symbol instead.
     */
    public Map<Integer, String> getMapping() {
        return mapping;
    }

    /**
     * Returns a debug string.
     */
    @Override
    public String toString() {
        return "NameRecord [oid=" + oid + ", syntax=" + syntax + ", desc=" + desc + "]";
    }

}