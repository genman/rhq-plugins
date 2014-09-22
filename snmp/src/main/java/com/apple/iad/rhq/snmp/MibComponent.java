package com.apple.iad.rhq.snmp;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;

/**
 * Represents an SNMP component that obtains metrics using a (compiled) MIB definition.
 * Performs an SNMP GET using the parent component.
 */
public class MibComponent implements MeasurementFacet, SnmpComponentHolder {

    private static final Log log = LogFactory.getLog(MibComponent.class);
    private SnmpComponent snmp;
    private MibIndex index;
    private Set<MeasurementDefinition> definitions;

    /**
     * Constructs a new instance.
     */
    public MibComponent() {
    }

    /**
     * Constructs a new instance with a connector.
     */
    public MibComponent(SnmpComponent snmp) {
        this.snmp = snmp;
        this.index = MibIndexCache.getIndex();
    }

    /**
     * Returns UP if able to obtain one of the defined measurement mib.
     */
    @Override
    public AvailabilityType getAvailability() {
        Set<MeasurementDefinition> mds = this.definitions;
        if (!mds.isEmpty()) {
            MeasurementDefinition md = mds.iterator().next();
            OID oid = index.getNameRecord(md.getName()).getOid0();
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
    public void start(ResourceContext<SnmpComponentHolder> context) throws InvalidPluginConfigurationException, Exception {
        snmp = context.getParentResourceComponent().getSnmpComponent();
        Configuration conf = context.getPluginConfiguration();
        definitions = context.getResourceType().getMetricDefinitions();
        index = index(conf);
    }

    /**
     * Creates a MIB index based on the 'mibs' property.
     */
    static MibIndex index(Configuration conf) throws IOException {
        String regex = "[,\\s]+";
        String mibs = conf.getSimpleValue("mibs", "").trim();
        if (!mibs.isEmpty()) {
            for (String mib : mibs.split(regex)) {
                log.debug("index " + mib);
                MibIndexCache.load(mib);
            }
        }
        return MibIndexCache.getIndex();
    }

    /**
     * Stops this component.
     */
    @Override
    public void stop() {
        index = null;
    }

    /**
     * Converts a measurement name into an OID object suffixed with 0.
     */
    protected OID oid(String name) {
        return index.getNameRecord(name).getOid0();
    }

    /**
     * Converts an SNMP variable into an RHQ measurement value.
     * This includes mapping integer values into string constants,
     * converting strings to doubles, etc.
     *
     * @param request measurement request
     * @param variable SNMP variable
     * @return data as a trait or numeric (or complex)
     */
    private MeasurementData asMeasurement(MeasurementScheduleRequest request, Variable variable) {
        if (variable == null)
            throw new NullPointerException("variable");
        if (request == null)
            throw new NullPointerException("request");
        DataType dataType = request.getDataType();
        String name = request.getName();
        if (variable instanceof Integer32) {
            int i = ((Integer32)variable).toInt();
            Map<Integer, String> mapping = index.getMapping(name);
            if (mapping != null && !mapping.isEmpty()) {
                return new MeasurementDataTrait(request, mapping.get(i));
            }
        }
        if (dataType == DataType.TRAIT) {
            return new MeasurementDataTrait(request, variable.toString());
        }
        if (variable instanceof OctetString && dataType == DataType.MEASUREMENT) {
            // interpret as String
            return new MeasurementDataNumeric(request, new Double(variable.toString()));
        }
        if (variable instanceof TimeTicks) {
            long ms = ((TimeTicks)variable).toMilliseconds();
            return new MeasurementDataNumeric(request, (double)ms);
        }
        return new MeasurementDataNumeric(request, (double)variable.toLong());
    }

    /**
     * Returns the values requested in the measurement report using a bulk GET.
     */
    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        List<OID> oids = new ArrayList<OID>();
        for (MeasurementScheduleRequest request : requests) {
            OID oid = oid(request.getName());
            oids.add(oid);
        }
        Map<OID, Variable> map = snmp.get(oids);
        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();
            OID oid = oid(name);
            Variable variable = map.get(oid);
            if (variable == null) {
                log.error("cannot map oid to variable for name=" + name + " oid=" + oid + " map=" + map);
                continue;
            }
            if (variable.isException()) {
                log.warn("exception obtaining name=" + name + " exception=" + variable);
            }
            add(report, request, variable);
        }
    }

    /**
     * Adds to a measurement report data from an SNMP variable.
     *
     * @param report measurement report
     * @param request measurement request corresponding to this variable:w
     * @param variable SNMP variable
     */
    void add(MeasurementReport report, MeasurementScheduleRequest request, Variable variable) {
        MeasurementData md = asMeasurement(request, variable);
        if (md == null) {
            log.warn("unable to obtain " + request.getName());
            return;
        }
        if (md instanceof MeasurementDataNumeric)
            report.addData((MeasurementDataNumeric)md);
        else if (md instanceof MeasurementDataTrait)
            report.addData((MeasurementDataTrait)md);
        else
            throw new IllegalStateException("unknown " + variable);
    }

    /**
     * Returns the parent SNMP component.
     */
    public SnmpComponent getSnmpComponent() {
        return snmp;
    }

    /**
     * MIB index.
     */
    public MibIndex getIndex() {
        return index;
    }

    /**
     * Debug string.
     */
    @Override
    public String toString() {
        return "MibComponent [snmp=" + snmp + "]";
    }

}
