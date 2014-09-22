package com.apple.iad.rhq.oozie;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.apple.iad.rhq.http.JSONTreeProvider;

/**
 * Discovers individual coordinator jobs.
 */
public class CoordinatorDiscovery implements ResourceDiscoveryComponent {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
        JobsComponent jobs = (JobsComponent) context.getParentResourceComponent();

        JSONTreeProvider tp = (JSONTreeProvider) jobs.getMeasurementProvider();
        List<Map> workflows = (List)((Map)tp.getTree()).get("coordinatorjobs");
        if (workflows == null) {
            throw new InvalidPluginConfigurationException("missing coordinatorjobs " + jobs);
        }
        for (Map m : workflows) {
            String jobName = (String) m.get("coordJobName");
            String user = (String) m.get("user");
            Configuration conf = context.getDefaultPluginConfiguration();
            DiscoveredResourceDetails drd = new DiscoveredResourceDetails(context.getResourceType(),
                    jobName, "Job " + jobName, "",
                    "Oozie job for job " + jobName + " user " + user,
                    conf, null);
            details.add(drd);
        }
        jobs.clearState();
        return details;
    }

}
