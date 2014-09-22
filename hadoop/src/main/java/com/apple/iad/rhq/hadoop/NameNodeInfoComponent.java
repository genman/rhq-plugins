package com.apple.iad.rhq.hadoop;

import java.util.Map;
import java.util.Set;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.jmx.MBeanResourceComponent;

public class NameNodeInfoComponent extends MBeanResourceComponent<HadoopComponent> implements MeasurementFacet {

    private static final String suffix = "Count";

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> msrs) {
        for (MeasurementScheduleRequest msr : msrs) {
            String name = msr.getName();
            if (name.endsWith(suffix)) {
                name = name.substring(0, name.length() - suffix.length());
                EmsAttribute attr = getEmsBean().getAttribute(name);
                String val = (String) attr.refresh();
                double d = count(val);
                report.addData(new MeasurementDataNumeric(msr, d));
            }
        }
        // TODO Auto-generated method stub
        super.getValues(report, msrs);
    }

    int count(String attr) {
        JSONParser parser = new JSONParser();
        try {
            Map l = (Map)parser.parse(attr);
            return l.size();
        } catch (ParseException e) {
            log.warn("cannot parse " + attr, e);
            return -1;
        }
    }

}
