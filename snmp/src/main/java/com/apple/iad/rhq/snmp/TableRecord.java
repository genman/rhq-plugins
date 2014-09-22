package com.apple.iad.rhq.snmp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.percederberg.mibble.MibTypeTag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.asn1.BER;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TableEvent;


/**
 * Table record stores columns and entry information.
 */
public class TableRecord {

    private static final Log log = LogFactory.getLog(TableRecord.class);

    private final MibIndex mibIndex;
    private final List<String> columns;
    private OID[] oids = new OID[0];
    private NameRecord entry;

    /**
     * Constructs a new TableRecord.
     */
    public TableRecord(MibIndex mibIndex, List<String> columns) {
        this.mibIndex = mibIndex;
        this.columns = columns;
    }

    /**
     * Cache/calculate several things.
     * Remove unused columns; store column OIDs; find NameRecord for entry (row).
     *
     * @param name name of the table
     */
    void init(String name) {
        for (Iterator<String> i = columns.iterator(); i.hasNext(); ) {
            String col = i.next();
            if (!mibIndex.names.containsKey(col))
                i.remove();
        }
        this.oids = new OID[columns.size()];
        for (int i = 0; i < columns.size(); i++)
            oids[i] = mibIndex.getNameRecord(columns.get(i)).getOid();
        OID toid = mibIndex.getNameRecord(name).getOid();
        toid = new OID(toid).append(1);
        String entryName = mibIndex.oids.get(toid);
        if (entryName == null)
            throw new IllegalStateException("entry name " + toid + " not found; init table " + name);
        entry = mibIndex.getNameRecord(entryName);
    }

    /**
     * Based on the table index type, returns the columns of the index with their value.
     * The values are either String, Number, or ???
     *
     * @throws IOException if decode fails
     */
    public Map<String, Object> index(TableEvent te) throws IOException {
        return index(te.getIndex());
    }

    /**
     * Desconstructs an OID into name-values pairs.
     */
    public Map<String, Object> index(OID oid) throws IOException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        ByteBuffer buffer = ByteBuffer.wrap(oid.toByteArray());
        BERInputStream inputStream = new BERInputStream(buffer);
        debug("index", oid, " decoding using ", this);
        for (OID index : entry.getIndex()) {
            String name = mibIndex.getName(index);
            NameRecord nameRecord = mibIndex.getNameRecord(name);
            MibTypeTag tag = nameRecord.getSyntax().getTag();
            debug("COLUMN", name, "TYPE", tag);
            Object value = null;
            if (tag == MibTypeTag.OCTET_STRING) {
                int len = BER.decodeLength(inputStream);
                byte b[] = new byte[len];
                inputStream.read(b);
                value = new String(b);
            } else if (tag == MibTypeTag.INTEGER) {
                // TODO determine how to handle multiple bytes for a number
                value = inputStream.read();
            } else if (tag.equals(MibTypeTags.IpAddress)) {
                value = toString(inputStream, 4);
            } else {
                log.warn("UNKNOWN " + tag + " col " + name + " remain " + inputStream.available());
                value = toString(inputStream, inputStream.available());
            }
            map.put(name, value);
        }
        return map;
    }

    /**
     * Reads from an input stream 'count' bytes, converting it into a dotted OID
     */
    public String toString(InputStream inputStream, int count) throws IOException {
        int array[] = new int[count];
        for (int j = 0; j < count; j++)
            array[j] = inputStream.read();
        return new OID(array).toString();
    }

    /**
     * Decodes a row into name-value pairs; assuming the columns are supplied in
     * order of table OID.
     *
     * @throws IllegalStateException if OIDs don't correspond
     */
    public Map<String, Variable> row(TableEvent te) {
        Map<String, Variable> map = new HashMap<String, Variable>();
        for (int i = 0; i < te.getColumns().length; i++) {
            String colName = columns.get(i);
            VariableBinding vb = te.getColumns()[i];
            if (!vb.getOid().startsWith(oids[i]))
                throw new IllegalStateException(vb + " does not start with " + oids[i]);
            map.put(colName, vb.getVariable());
        }
        return map;
    }

    /**
     * Return a list of table columns.
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * Return the OIDs corresponding to table columns.
     */
    public OID[] getOids() {
        return oids;
    }

    private void debug(Object... objs) {
        log.debug(Arrays.toString(objs));
    }

    /**
     * Return a debug string.
     */
    @Override
    public String toString() {
        return "TableRecord [columns=" + columns
                + ", oids=" + Arrays.toString(oids) + ", entry=" + entry + "]";
    }


}
