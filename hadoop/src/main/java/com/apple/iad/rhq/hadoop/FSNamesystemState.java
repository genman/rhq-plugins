package com.apple.iad.rhq.hadoop;

import java.util.Map;

/**
 * Job tracker information.
 */
public class FSNamesystemState extends HadoopMBean {

    final int GB = 1024 * 1024 * 1024;

    /*
     <metric property="BlocksTotal" displayName="BlocksTotal" units="none"/>
            <metric property="CapacityRemaining" displayName="CapacityRemaining" units="bytes" description="DFS remaining"/>
            <metric property="CapacityTotal" displayName="CapacityTotal" units="bytes" description="DFS Configured capacitiy"/>
            <metric property="CapacityUsed" displayName="CapacityUsed" units="bytes" description="DFS used" displayType="summary"/>
            <metric property="FSState" displayName="FSState" units="none" dataType="trait"/>
            <metric property="FilesTotal" displayName="FilesTotal" units="none"/>
            <metric property="PendingReplicationBlocks" displayName="PendingReplicationBlocks" units="none"/>
            <metric property="ScheduledReplicationBlocks" displayName="ScheduledReplicationBlocks" units="none"/>
            <metric property="TotalLoad" displayName="TotalLoad" units="none"/>
            <metric property="UnderReplicatedBlocks" displayName="UnderReplicatedBlocks" units="none"/>
     */
    @Override
    protected void digest(Map<String, Double> values) throws Exception {
        NameNodeComponent nnc = (NameNodeComponent)getResourceContext().getParentResourceComponent();
        JSONProvider provider = nnc.getProvider();
        if (provider == null)
            return;
        Map<String, Double> json = provider.getDocument();
        String attr[] = {
            "BlocksTotal", "FilesTotal", "PendingReplicationBlocks", "ScheduledReplicationBlocks",
            "TotalLoad", "UnderReplicatedBlocks"
        };
        for (String s : attr)
            values.put(s, json.get(s));
        try {
            values.put("CapacityRemaining", json.get("CapacityRemainingGB") * GB);
            values.put("CapacityTotal",     json.get("CapacityTotalGB") * GB);
            values.put("CapacityUsed",      json.get("CapacityUsedGB") * GB);
        } catch (NullPointerException e) {
            log.debug("failed to get key", e);
        }
    }

}
