package com.apple.iad.rhq.snmp;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.testng.annotations.Test;

@Test
public class MibTableComponentExtTest {

    protected final Log log = LogFactory.getLog(getClass());

    final OID oid1 = new OID("1.2.3");
    final OID oid2 = new OID("3.2.1");
    final long lo = 42;
    final long hi = 4;
    final MibComponent mibComponent = new MockMibComponent();

    MibTableComponentExt ex = new MibTableComponentExt(mibComponent, null) {

        @Override
        public OID oid(String name) {
            if (name.equals("abc"))
                return oid1;
            if (name.equals("xyz"))
                return oid2;
            throw new IllegalStateException("name " + name);
        }

    };

    public void testComposite() throws Exception {
        MeasurementReport report = new MeasurementReport();
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(0, "abc^xyz", 0L, true, DataType.TRAIT);
        ex.getValues(report, Collections.singleton(request));
        MeasurementDataNumeric n = report.getNumericData().iterator().next();
        long expect = lo + (hi << 32);
        log.info(expect);
        assertEquals(expect, n.getValue().longValue());
    }

    class MockMibComponent extends MibComponent {

        @Override
        public SnmpComponent getSnmpComponent() {
            return new MockSnmpComponent();
        }

    }

    class MockSnmpComponent extends SnmpComponent {

        @Override
        public Map<OID, Variable> get(Collection<OID> oids) throws IOException {
            Map<OID, Variable> map = new LinkedHashMap<OID, Variable>();
            if (oids.size() == 2) {
                Iterator<OID> i = oids.iterator();
                map.put(i.next(), new Counter32(lo));
                map.put(i.next(), new Counter32(hi));
            }
            return map;
        }

    }

}
