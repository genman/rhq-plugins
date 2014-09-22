package com.apple.iad.rhq.snmp;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.snmp4j.smi.OID;
import org.snmp4j.util.TableEvent;

/**
 * Discovers a table defined in a MIB file, using the resource name or property
 * 'table' as table name. Translates the table name to an OID, then looks up
 * the column values, etc.
 */
public class MibTableDiscovery implements ResourceDiscoveryComponent<MibComponent> {

    /**
     * Index OID as a string, for example '1.2.3.4'.
     * See {@link OID#OID(String)} for how this is parsed.
     */
    public static final String INDEX = "index";

    /**
     * Table name.
     */
    public static final String TABLE = "table";

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Discover table rows by querying the table name, which is either the resource name
     * or 'table' configuration value.
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<MibComponent> rdc) throws Exception {

        Set<DiscoveredResourceDetails> drd = new HashSet<DiscoveredResourceDetails>();
        Configuration conf = rdc.getDefaultPluginConfiguration();

        MibComponent mibComponent = rdc.getParentResourceComponent();
        String tableName = rdc.getResourceType().getName();
        tableName = conf.getSimpleValue(TABLE, tableName);

        log.debug("discover table " + tableName);
        MibIndex index = mibComponent.getIndex();
        TableRecord tableRecord = index.getTableRecord(tableName);
        OID oids[] = { tableRecord.getOids()[0] };
        List<TableEvent> events = mibComponent.getSnmpComponent().getTable(oids);

        for (TableEvent event : events) {
            conf = conf.deepCopy();
            log.debug("events " + event);
            if (event.getIndex() == null) {
                log.debug("empty index");
                continue;
            }
            log.debug("event index " + event.getIndex());
            String name = toString(tableRecord.index(event));
            log.debug("decode " + name);
            String version = null;

            String key = event.getIndex().toString();
            conf.put(new PropertySimple(INDEX, key));
            conf.put(new PropertySimple(TABLE, tableName));

            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                    rdc.getResourceType(), // ResourceType
                    key,    // key
                    name, // help resource name
                    version, // Version
                    "SNMP table " + tableName + ", row index " + name, // description
                    conf,
                    null // process information
            );
            drd.add(detail);
        }

        return drd;
    }

    private static String toString(Map<String, Object> index) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, Object> e : index.entrySet()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(e);
        }
        return sb.toString();
    }


}
