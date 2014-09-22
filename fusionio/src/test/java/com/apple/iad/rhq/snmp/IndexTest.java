package com.apple.iad.rhq.snmp;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Variable;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

@Test
public class IndexTest {

    private static final String THERMAL = "fusionIoDimmInfoThermalGoverningLevel";
    private Log log = LogFactory.getLog(getClass());

    public void testThis() throws IOException {
        String mibFile = "fioIoDimm.mib";
        MibIndexCache.load(mibFile);
        MibIndex index = MibIndexCache.getIndex();
        Map<Integer, String> mapping = index.getMapping(THERMAL);
        log.info(mapping);
        SnmpComponent snmp = new SnmpComponent();
        MibComponent mibComponent = new MibComponent(snmp);
        MeasurementReport report = new MeasurementReport();
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(0, THERMAL, 0L, true, DataType.TRAIT);
        Variable variable = new Integer32(2);
        mibComponent.add(report, request, variable);
        assertEquals("moderate", report.getTraitData().iterator().next().getValue());

    }

}
