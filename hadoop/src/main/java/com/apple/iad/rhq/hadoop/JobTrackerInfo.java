package com.apple.iad.rhq.hadoop;

import java.util.Map;

/**
 * Job tracker information.
 */
public class JobTrackerInfo extends HadoopMBean {
    
    @Override
    protected void digest(Map<String, Double> values) {
        digest(values, getEmsBean().getAttribute("SummaryJson"));
    }

}
