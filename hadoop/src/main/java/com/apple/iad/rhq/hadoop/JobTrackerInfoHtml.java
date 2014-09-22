package com.apple.iad.rhq.hadoop;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Parses an HTML page to get job tracker information.
 */
public class JobTrackerInfoHtml implements ResourceComponent<JobTrackerInfoHtml>, MeasurementFacet,
    ResourceDiscoveryComponent<JobTrackerInfoHtml>, ManualAddFacet<JobTrackerInfoHtml> {

    private final static Log log = LogFactory.getLog(JobTrackerInfoHtml.class);

    private URL url;

    private Map<String, Double> map = Collections.emptyMap();

    static final Pattern stats = Pattern.compile("<table[^>]+>\\s*<tr>(.*)</tr>\\s*<tr>(.*)</tr>\\s*</table>");
    static final Pattern tag = Pattern.compile("(\\s*</?\\w+[^>]*>\\s*)+");

    private static final String URL = "url";

    public JobTrackerInfoHtml() { }

    public JobTrackerInfoHtml(URL url) {
        this.url = url;
    }

    Map<String, Double> parse(String html) {
        Matcher matcher = stats.matcher(html);
        if (!matcher.find())
            throw new IllegalStateException("pattern not found " + stats.pattern());

        Map<String, Double> digest = new HashMap<String, Double>();
        String heads = matcher.group(1);
        String row = matcher.group(2);
        String[] values = tag.split(row);
        int c = 0;
        for (String head : tag.split(heads)) {
            String val = values[c++];
            if (val.isEmpty())
                continue;
            digest.put(head, new Double(val));
        }
        if (log.isDebugEnabled())
            log.debug("digest=" + digest);
        return digest;
    }

    @Override
    public AvailabilityType getAvailability() {
        InputStream is;
        try {
            is = url.openStream();
            try {
                this.map = parse(IOUtils.toString(is));
            } finally {
                is.close();
            }
        } catch (Exception e) {
            log.warn("cannot parse " + url, e);
            return AvailabilityType.DOWN;
        }
        return AvailabilityType.UP;
    }

    @Override
    public void start(ResourceContext<JobTrackerInfoHtml> context) throws InvalidPluginConfigurationException, Exception {
        Configuration c = context.getPluginConfiguration();
        String urls = c.getSimpleValue(URL, "http://localhost:50030/jobtracker.jsp");
        try {
            this.url = new URL(urls);
        } catch (IOException e) {
            throw new InvalidPluginConfigurationException(e);
        }
    }

    @Override
    public void stop() {
        map.clear();
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> msrs) throws Exception {
        if (map.isEmpty())
            getAvailability();
        for (Iterator<MeasurementScheduleRequest> i = msrs.iterator(); i.hasNext(); ) {
            MeasurementScheduleRequest msr = i.next();
            Double d = map.get(msr.getName());
            if (d != null) {
                mr.addData(new MeasurementDataNumeric(msr, d));
            }
        }
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration, ResourceDiscoveryContext<JobTrackerInfoHtml> rdc) {
        String url = pluginConfiguration.getSimpleValue(URL, null);
        String key = "Job Tracker Info " + url;
        String name = key;
        String description = "Job Tracker Info from HTML page at URL " + url;
        ResourceType resourceType = rdc.getResourceType();
        DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceType, key, name, null, description,
            pluginConfiguration, null);

        return details;
    }

    /**
     * Find the queues.
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JobTrackerInfoHtml> paramResourceDiscoveryContext) {
        return Collections.emptySet();
    }

    URL forQueue(String queue) throws MalformedURLException {
        return new URL(url, "/jobqueue_details.jsp?queueName=" + queue);
    }

    public URL getUrl() {
        return url;
    }

}
