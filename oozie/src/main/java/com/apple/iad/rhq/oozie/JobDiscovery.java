package com.apple.iad.rhq.oozie;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.apple.iad.rhq.http.JSONTreeProvider;

/**
 * Discovers individual jobs.
 */
public class JobDiscovery implements ResourceDiscoveryComponent {

    private static final Log log = LogFactory.getLog(JobComponent.class);

        /*
         * JSON is roughly:
{
   "total":158,
   "workflows":[
      {
         "appPath":null,

         */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
        JobsComponent jobs = (JobsComponent) context.getParentResourceComponent();

        JSONTreeProvider tp = (JSONTreeProvider) jobs.getMeasurementProvider();
        List<Map> workflows = (List)((Map)tp.getTree()).get("workflows");
        for (Map m : workflows) {
            String appName = (String) m.get("appName");
            String user = (String) m.get("user");
            Configuration conf = context.getDefaultPluginConfiguration();
            DiscoveredResourceDetails drd = new DiscoveredResourceDetails(context.getResourceType(),
                    appName, "Job " + appName, "",
                    "Oozie job for appName " + appName + " user " + user,
                    conf, null);
            details.add(drd);
        }
        jobs.clearState();
        return details;
    }

}
