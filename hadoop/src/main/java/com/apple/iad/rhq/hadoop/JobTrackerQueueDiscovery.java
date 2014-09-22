package com.apple.iad.rhq.hadoop;

import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovers a list of Hadoop Hive tables matching a query and regular expression.
 * Uses (MySQL) JDBC to access the metadata, not the Hive JDBC driver.
 */
public class JobTrackerQueueDiscovery implements ResourceDiscoveryComponent {

    static final Pattern jobqueue = Pattern.compile("\"jobqueue_details.jsp\\?queueName=(.*)\"");
    
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext context)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        // TODO use JSon interface
        JobTrackerInfoHtml server = (JobTrackerInfoHtml) context.getParentResourceComponent();
        Set<String> queues = getQueues(server.getUrl());
        for (String queue : queues) {
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                    context.getResourceType(),
                    queue, // database key
                    "Queue " + queue, // UI name
                    null, // Version
                    "Job Tracker Queue " + queue,
                    context.getDefaultPluginConfiguration(),
                    null); // process info
            details.add(detail);
        }

        return details;
    }

    /**
     * Return queues.
     */
    Set<String> getQueues(URL url) throws Exception {
        InputStream is = url.openStream();
        try {
            String s = IOUtils.toString(is);
            Matcher matcher = jobqueue.matcher(s);
            Set<String> queues = new HashSet<String>();
            while (matcher.find()) {
                String q = matcher.group(1);
                queues.add(q);
            }
            return queues;
        } finally {
            is.close();
        }
    }

}
