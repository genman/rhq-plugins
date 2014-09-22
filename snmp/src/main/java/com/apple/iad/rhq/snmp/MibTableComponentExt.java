package com.apple.iad.rhq.snmp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.Variable;

/**
 * Extends the parent, allowing a property name with ^, for combining the values
 * of two 32 BIT OID values into a double value.
 * This is probably useless outside of Fusion IO, since everyone else uses
 * Counter64 instead...
 */
public class MibTableComponentExt extends MibTableComponent {

    private static final Pattern ext = Pattern.compile("(.*)\\^(.*)");

    /**
     * Constructs a new instance.
     */
    public MibTableComponentExt() {
    }

    MibTableComponentExt(MibComponent mibComponent, OID rowIndex) {
        super(mibComponent, rowIndex);
    }

    class ComposedRequest {
        final MeasurementScheduleRequest req;
        final OID oid1;
        final OID oid2;
        public ComposedRequest(MeasurementScheduleRequest req, Matcher matcher) {
            this.req = req;
            oid1 = oid(matcher.group(1));
            oid2 = oid(matcher.group(2));
        }
    }

    /**
     * Bug in RHQ classloader: parent method is protected and can't be accessed (?)
     */
    @Override
    public OID oid(String name) {
        return super.oid(name);
    }

    /**
     * If the property name contains a ^, then a request is made for each separate OID.
     * The OID values are then combined into one value, returned as a double.
     * Potentially could be used to, say, multiple or add or something.
     */
    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        requests = new HashSet<MeasurementScheduleRequest>(requests); // copy for safety
        List<ComposedRequest> creq = new ArrayList<ComposedRequest>();
        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();
            Matcher matcher = ext.matcher(name);
            if (matcher.matches()) {
                creq.add(new ComposedRequest(request, matcher));
            }
        }
        List<OID> oids = new ArrayList<OID>();
        for (ComposedRequest request : creq) {
            oids.add(request.oid1);
            oids.add(request.oid2);
        }
        Map<OID, Variable> map = getMibComponent().getSnmpComponent().get(oids);
        for (ComposedRequest request : creq) {
            Variable v1 = map.get(request.oid1);
            Variable v2 = map.get(request.oid2);
            if (v1 instanceof UnsignedInteger32 && v2 instanceof UnsignedInteger32) {
                long a = ((UnsignedInteger32)v1).getValue();
                long b = ((UnsignedInteger32)v2).getValue();
                long v = a + (b << 32);
                report.addData(new MeasurementDataNumeric(request.req, (double)v));
            }
            requests.remove(request.req);
        }
        super.getValues(report, requests);
    }

}
