package com.apple.iad.rhq.hadoop;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.simple.parser.ParseException;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Extends MBean information with data from JSON attributes, optionally.
 */
public class HadoopMBean extends MBeanResourceComponent<HadoopComponent> {

    public HadoopMBean() {
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> msrs) {
        Map<String, Double> digest = new HashMap<String, Double>();
        try {
            digest(digest);
        } catch (Exception e) {
            log.warn("failed to digest additional content", e);
        }
        msrs = new HashSet(msrs); // need to copy - RHQ Bug 821058
        for (Iterator<MeasurementScheduleRequest> i = msrs.iterator(); i.hasNext(); ) {
            MeasurementScheduleRequest msr = i.next();
            Double d = digest.get(msr.getName());
            if (d != null) {
                mr.addData(new MeasurementDataNumeric(msr, d));
                i.remove();
            }
        }
        if (!msrs.isEmpty()) // JIRA issue HADOOP-8389
            super.getValues(mr, msrs);
    }

    /**
     * Optional method to 'digest' (parse) additional metrics outside the MBean.
     * Passed in is an empty map to populate.
     */
    protected void digest(Map<String, Double> values) throws Exception {
    }

    protected void digest(Map<String, Double> values, EmsAttribute attribute) {
        try {
            JSONProvider.digest(values, (String) attribute.getValue());
        } catch (ParseException e) {
            log.info("cannot parse " + attribute, e);
        }
    }
}
