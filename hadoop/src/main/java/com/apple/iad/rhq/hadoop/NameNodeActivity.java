package com.apple.iad.rhq.hadoop;

import java.util.Map;

/**
 * Job tracker information.
 */
public class NameNodeActivity extends HadoopMBean {
    
    final int GB = 1024 * 1024 * 1024;
    
    /*
     <metric property="fsImageLoadTime" displayName="fsImageLoadTime" units="milliseconds"/>
     */
    @Override
    protected void digest(Map<String, Double> values) throws Exception {
        NameNodeComponent nnc = (NameNodeComponent)getResourceContext().getParentResourceComponent();
        JSONProvider provider = nnc.getProvider();
        if (provider == null)
            return;
        Map<String, Double> json = provider.getDocument();
        String attr[] = {
            "FilesCreated", "fsImageLoadTime"
        };
        for (String s : attr)
            values.put(s, json.get(s));
        values.put("BlocksCorrupted", json.get("CorruptBlocks"));
        values.put("SyncsAvgTime",    json.get("Syncs_avg_time"));
        values.put("TransactionsAvgTime", json.get("Transactions_avg_time"));
        values.put("TransactionsNumOps", json.get("Transactions_num_ops"));
        values.put("blockReportAvgTime", json.get("blockReport_avg_time"));
    }

}
