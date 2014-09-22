package com.apple.iad.rhq.snmp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.percederberg.mibble.Mib;
import net.percederberg.mibble.MibLoader;
import net.percederberg.mibble.MibLoaderException;
import net.percederberg.mibble.MibSymbol;
import net.percederberg.mibble.MibType;
import net.percederberg.mibble.MibTypeSymbol;
import net.percederberg.mibble.MibValue;
import net.percederberg.mibble.MibValueSymbol;
import net.percederberg.mibble.snmp.SnmpAccess;
import net.percederberg.mibble.snmp.SnmpIndex;
import net.percederberg.mibble.snmp.SnmpNotificationType;
import net.percederberg.mibble.snmp.SnmpObjectType;
import net.percederberg.mibble.snmp.SnmpStatus;
import net.percederberg.mibble.snmp.SnmpTrapType;
import net.percederberg.mibble.snmp.SnmpType;
import net.percederberg.mibble.type.ElementType;
import net.percederberg.mibble.type.SequenceOfType;
import net.percederberg.mibble.type.SequenceType;
import net.percederberg.mibble.value.ObjectIdentifierValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TableEvent;

/**
 * Provides mappings of the SNMP name and OID; tracks interesting data from the MIB to use at runtime.
 */
public class MibIndex {

    public static final Pattern NAME_OID = Pattern.compile("(\\p{Alpha}+)(\\.\\d+)*");
    public static final String MIB_DIR = "mibs";

    private static final Log log = LogFactory.getLog(MibIndex.class);
    private static final boolean debug = log.isDebugEnabled();

    final NavigableMap<OID, String> oids = new TreeMap<OID, String>();
    final Map<String, NameRecord> names = new LinkedHashMap<String, NameRecord>();
    private final Map<String, TableRecord> tables = new LinkedHashMap<String, TableRecord>();
    private final MibLoader loader;
    private boolean extra;

    /**
     * Constructs a new instance.
     */
    public MibIndex() {
        loader = new MibLoader();
        loader.addResourceDir(MIB_DIR);
    }

    /**
     * Set to 'true' to store extra information from the MIB.
     * Currently this only includes the description.
     */
    public void setExtra(boolean extra) {
        this.extra = extra;
    }

    /**
     * Load a MIB file, then build an index based on the SNMP object symbols.
     * Builds a name to OID and name to type reference.
     */
    public void load(String mibFile) throws IOException {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            log.debug("load mib file " + mibFile + " class loader" + cl);
            Mib mib;
            File file = new File(mibFile);
            URL url = cl.getResource(MIB_DIR + "/" + mibFile);
            if (file.canRead()) {
                mib = loader.load(file);
            } else if (url != null) {
                log.debug("found using URL");
                mib = loader.load(url);
            } else {
                try {
                    url = new URL(mibFile);
                    log.debug("loading using URL");
                    mib = loader.load(url);
                } catch (MalformedURLException e) {
                    log.debug("not found using URL; load using loader class");
                    mib = loader.load(mibFile);
                }
            }
            Collection<MibSymbol> symbols = mib.getAllSymbols();
            for (MibSymbol s : symbols) {
                if (s instanceof MibValueSymbol) {
                    MibValueSymbol v = (MibValueSymbol) s;
                    MibType t = v.getType();
                    if (t instanceof SnmpType) {
                        SnmpType ot = (SnmpType) t;
                        symbol(v, ot);
                    }
                }
            }
            initTables();
        } catch (IOException e) {
            throw e;
        } catch (MibLoaderException e) {
            StringWriter sw = new StringWriter();
            e.getLog().printTo(new PrintWriter(sw));
            throw new IOException(sw.toString());
        }
    }

    private void symbol(MibValueSymbol v, SnmpType ot) {
        if (obsolete(ot)) {
            return;
        }
        String name = v.getName();
        MibTypeSymbol mts = null;
        MibType type = v.getType();
        MibType syntax = null;
        List<OID> index = Collections.emptyList();
        debug("symbol", name, type.getClass(), v.getValue());
        if (type instanceof SnmpObjectType) {
            SnmpObjectType sot = (SnmpObjectType) type;
            syntax = sot.getSyntax();
            if (syntax instanceof SequenceOfType) {
                table(name, (SequenceOfType) syntax);
            } else if (syntax instanceof SequenceType) {
                if (!sot.getIndex().isEmpty()) {
                    index = new ArrayList<OID>();
                    for (SnmpIndex i : (List<SnmpIndex>)sot.getIndex()) {
                        ObjectIdentifierValue oiv = (ObjectIdentifierValue) i.getValue();
                        index.add(new OID(oiv.toString()));
                    }
                }
            }
            mts = syntax.getReferenceSymbol();
        }
        debug("mts", mts, "type", type);
        String desc = extra ? ot.getDescription() : null;
        debug("name", name);
        SnmpAccess access = null;
        if (ot instanceof SnmpObjectType) {
            access = ((SnmpObjectType) ot).getAccess();
        }
        OID oid;
        if (ot instanceof SnmpTrapType) {
            MibValue mv = ((SnmpTrapType) ot).getEnterprise();
            debug("mv", mv);
            oid = new OID(mv.toString());
        } else {
            oid = new OID(v.getValue().toString());
        }
        names.put(name, new NameRecord(oid, syntax, access, mts, index, desc));
        oids.put(oid, name);
    }

    /**
     * Tables are 'sequence of type' containing entry instances which are 'sequence type'.
     */
    private void table(String name, SequenceOfType sot) {
        debug("table", name, sot);
        MibType et = sot.getElementType();
        if (et instanceof SequenceType) {
            SequenceType st = (SequenceType) et;
            // syntax name, not entry name; rely on entry OID being table OID + 1
            // String entry = st.getReferenceSymbol().getName();
            List<String> cols = new ArrayList<String>();
            for (ElementType et2 : st.getAllElements()) {
                String cname = et2.getName();
                cols.add(cname);
            }
            tables.put(name, new TableRecord(this, cols));
        }
    }

    private boolean obsolete(SnmpType type) {
        if (type instanceof SnmpNotificationType)
            return ((SnmpNotificationType)type).getStatus() == SnmpStatus.OBSOLETE;
        if (type instanceof SnmpObjectType)
            return ((SnmpObjectType)type).getStatus() == SnmpStatus.OBSOLETE;
        return false;
    }

    /**
     * Returns a name record.
     * @throws IndexException if name is not found
     */
    public NameRecord getNameRecord(String name) {
        if (name == null)
            throw new NullPointerException("name");
        NameRecord nr = names.get(name);
        if (nr == null)
            throw new IndexException("name " + name + " not found");
        return nr;
    }

    /**
     * Returns a name record by exact OID.
     * @throws IndexException if OID is not found
     * @see #toName(OID) to convert to name plus suffix
     */
    public String getName(OID oid) {
        String name = oids.get(oid);
        if (name == null)
            throw new IndexException("oid " + oid + " not found");
        return name;
    }

    /**
     * Returns true if {@link #getName(OID)} will return a value.
     */
    public boolean hasName(OID oid) {
        return oids.containsKey(oid);
    }

    /**
     * Returns a map of integers to value by name.
     * Returns null if no mapping exists.
     */
    public Map<Integer, String> getMapping(String name) {
        NameRecord nameRecord = names.get(name);
        if (nameRecord == null)
            return null;
        return nameRecord.getMapping();
    }

    /**
     * Remove unknown columns from tables, set up OID columns, indexes.
     */
    private void initTables() {
        for (Entry<String, TableRecord> me : tables.entrySet()) {
            TableRecord tr = me.getValue();
            tr.init(me.getKey());
        }
    }

    /**
     * Returns the OID columns for this table.
     * @throws IndexException if table is unknown
     */
    public TableRecord getTableRecord(String tableName) {
        TableRecord tableRecord = tables.get(tableName);
        if (tableRecord == null)
            throw new IndexException("unknown table " + tableName + " have " + tables.keySet());
        return tableRecord;
    }

    /**
     * Decode a table event for a table, assuming the columns are as ordered in the
     * call to {@link #getTable(String)}.
     *
     * @param tableName
     * @param te
     * @return
     */
    public Map<String, Variable> decode(String tableName, TableEvent te) {
        HashMap<String, Variable> hm = new HashMap<String, Variable>();
        log.debug(te);
        for (VariableBinding vb : te.getColumns()) {
            String key = oids.get(vb.getOid());
            Variable var = vb.getVariable();
            hm.put(key, var);
        }
        return hm;
    }

    private void debug(Object... objs) {
        if (debug)
            log.debug(Arrays.toString(objs));
    }

    /**
     * Returns a mapping of names to name records.
     */
    public Map<String, NameRecord> getNames() {
        return Collections.unmodifiableMap(names);
    }

    /**
     * Returns the OID for the given string value.
     * Note the name can be an OID, or a textual name, include suffixes.
     * Examples:
     * <ul>
     * <li>someName   - will return the OID of 'someName'
     * <li>someName.2 - will return the OID of 'someName' suffixed with '2'
     * <li>1.3.6.1.6.3.1.1.5 - will parse the OID
     * </ul>
     * @throws IndexException if name cannot be resolved
     */
    public OID toOid(String value) {
        if (value == null)
            throw new NullPointerException("value");
        Matcher m = NAME_OID.matcher(value);
        if (m.matches()) {
            String name = m.group(1);
            String remain = m.group(2);
            if (remain == null) {
                return getNameRecord(name).getOid();
            } else {
                remain = remain.substring(1);
                return new OID(getNameRecord(name).getOid()).append(new OID(remain));
            }
        }
        return new OID(value);
    }

    /**
     * Inverse operation of {@link #toOid(String)}.
     * Returns the exact matching name part suffixed with the remainder OID.
     */
    public String toName(OID oid) {
        if (oid == null)
            throw new NullPointerException("value");
        String name = this.oids.get(oid);
        if (name != null)
            return name;
        Entry<OID, String> lowerEntry = this.oids.lowerEntry(oid);
        if (lowerEntry != null && oid.startsWith(lowerEntry.getKey())) {
            OID parent = lowerEntry.getKey();
            OID suffix = new OID(oid.getValue(), parent.size(), oid.size() - parent.size());
            return lowerEntry.getValue() + "." + suffix;
        }
        return oid.toString();
    }

    /**
     * Returns a mapping of names to table records.
     */
    public Map<String, TableRecord> getTables() {
        return Collections.unmodifiableMap(tables);
    }

    /**
     * Returns a debug string.
     */
    @Override
    public String toString() {
        return "MibIndex [loader=" + loader +
                ",\n names=" + names +
                ",\n tables=" + tables +
                "]";
    }

}
