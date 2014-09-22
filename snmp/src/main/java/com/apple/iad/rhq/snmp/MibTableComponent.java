package com.apple.iad.rhq.snmp;

import static com.apple.iad.rhq.snmp.MibTableDiscovery.INDEX;
import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;


/**
 * Represents an SNMP table row, where the row is indicated by
 * its {@link MibTableDiscovery#INDEX} configuration value.
 */
public class MibTableComponent implements ResourceComponent<MibComponent>, MeasurementFacet, OperationFacet {

    private static final Log log = LogFactory.getLog(MibComponent.class);

    private MibComponent mibComponent;
    private OID rowIndex;

    private Set<MeasurementDefinition> definitions;

    private SnmpComponent snmp;

    /**
     * Construct a new instance.
     */
    public MibTableComponent() {
    }

    /**
     * Construct with necessary fields.
     */
    public MibTableComponent(MibComponent mibComponent, OID rowIndex) {
        this.mibComponent = mibComponent;
        this.snmp = mibComponent.getSnmpComponent();
        this.rowIndex = rowIndex;
    }

    /**
     * Returns DOWN if the table row no longer exists or can be read.
     */
    @Override
    public AvailabilityType getAvailability() {
        Set<MeasurementDefinition> mds = this.definitions;
        if (!mds.isEmpty()) {
            MeasurementDefinition md = mds.iterator().next();
            OID oid = oid(md.getName());
            try {
                Variable variable = snmp.get(oid);
                if (variable.isException())
                    return DOWN;
            } catch (IOException e) {
                return DOWN;
            }
        }
        return UP;
    }

    @Override
    public void start(ResourceContext<MibComponent> rc) throws Exception {
        mibComponent = rc.getParentResourceComponent();
        Configuration conf = rc.getPluginConfiguration();
        String index = conf.getSimpleValue(INDEX, "");
        if (index.isEmpty())
            throw new IllegalStateException(INDEX + " not defined");
        this.rowIndex = new OID(index);
        log.debug("start " + rc.getResourceType().getName() + " " + rc.getResourceKey());
        definitions = rc.getResourceType().getMetricDefinitions();
        snmp = mibComponent.getSnmpComponent();
    }

    @Override
    public void stop() {
        mibComponent = null;
        rowIndex = null;
    }

    /**
     * Returns the parent {@link MibComponent}.
     */
    protected MibComponent getMibComponent() {
        return mibComponent;
    }

    /**
     * Returns a concatenated column + row OID to obtain SNMP data.
     * @param name name of the column
     */
    protected OID oid(String name) {
        MibIndex mibIndex = mibComponent.getIndex();
        OID column = mibIndex.getNameRecord(name).getOid();
        return new OID(column.toIntArray(), rowIndex.toIntArray());
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> msrs) throws Exception {
        Map<OID, MeasurementScheduleRequest> oids = new HashMap<OID, MeasurementScheduleRequest>();
        for (MeasurementScheduleRequest msr : msrs) {
            String name = msr.getName();
            oids.put(oid(name), msr);
        }
        Map<OID, Variable> map = snmp.get(oids.keySet());
        if (log.isDebugEnabled())
            log.debug("getValues() " + map);
        for (Map.Entry<OID, Variable> me : map.entrySet()) {
            OID oid = me.getKey();
            MeasurementScheduleRequest request = oids.get(oid);
            if (request == null) {
                log.debug(oid + " oid found in response, not in request");
                continue;
            }
            Variable variable = me.getValue();
            if (variable.isException()) {
                log.debug(oid + " has exception " + variable);
                continue;
            }
            mibComponent.add(report, request, variable);
        }
    }

    @Override
    public String toString() {
        return "MibTableComponent [mibComponent=" + mibComponent + ", rowIndex="
                + rowIndex + "]";
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (name.equals("set")) {
            return new SetOperation(mibComponent.getSnmpComponent(), rowIndex).set(parameters);
        }
        throw new Exception("unknown operation " + name);
    }

}
