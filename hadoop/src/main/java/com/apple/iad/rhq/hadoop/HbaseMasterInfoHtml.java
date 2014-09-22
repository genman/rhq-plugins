package com.apple.iad.rhq.hadoop;

import java.io.IOException;
import java.io.InputStream;
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
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
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
public class HbaseMasterInfoHtml implements ResourceComponent<HbaseMasterInfoHtml>, MeasurementFacet,
ResourceDiscoveryComponent<HbaseMasterInfoHtml>, ManualAddFacet<HbaseMasterInfoHtml> {

    private final static Log log = LogFactory.getLog(HbaseMasterInfoHtml.class);

    private URL url;

    private Map<String, Object> map = Collections.emptyMap();

    private static final Pattern stats = Pattern.compile("<tr><td>([\\w\\s]+)</td><td>([\\d.]+)</td>");

    private static final Pattern total = Pattern.compile(
            "Total.*servers.*(\\d+).*requests.*(\\d+).*regions.*(\\d+)");

    private static final String URL = "url";

    public HbaseMasterInfoHtml() { }

    public HbaseMasterInfoHtml(URL url) {
        this.url = url;
    }

    Map<String, Object> parse(String html) {
        Map<String, Object> digest = new HashMap<String, Object>();

        Matcher matcher = stats.matcher(html);
        while (matcher.find()) {
            log.debug(matcher.group(1) + "," + matcher.group(2));
            digest.put(matcher.group(1), new Double(matcher.group(2)));
        }

        matcher = total.matcher(html);
        while (matcher.find()) {
            log.debug("found " + matcher.group());
            int servers = Integer.parseInt(matcher.group(1));
            double requests = Double.parseDouble(matcher.group(2));
            double regions = Double.parseDouble(matcher.group(3));
            digest.put("rserver", regions/servers);
            digest.put("servers", servers);
            digest.put("regions", regions);
            digest.put("requests", requests);
        }


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
    public void start(ResourceContext<HbaseMasterInfoHtml> context) throws InvalidPluginConfigurationException, Exception {
        Configuration c = context.getPluginConfiguration();
        String urls = c.getSimpleValue(URL, "http://localhost:60010/master.jsp");
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
            Object o = map.get(msr.getName());
            if (o != null) {
                if (msr.getDataType() == DataType.MEASUREMENT) {
                    mr.addData(new MeasurementDataNumeric(msr, (Double)o));
                } else {
                    mr.addData(new MeasurementDataTrait(msr, o.toString()));
                }
            }
        }
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration, ResourceDiscoveryContext<HbaseMasterInfoHtml> rdc) {
        String url = pluginConfiguration.getSimpleValue(URL, null);
        String key = "HbaseMasterInfo " + url;
        String name = key;
        String description = "Hbase master Info from HTML page at URL " + url;
        ResourceType resourceType = rdc.getResourceType();
        DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceType, key, name, null, description,
                pluginConfiguration, null);

        return details;
    }

    /**
     * Find the queues.
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<HbaseMasterInfoHtml> paramResourceDiscoveryContext) {
        return Collections.emptySet();
    }

}
