package com.apple.iad.rhq.flume;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * Discovers a group of MBeans where the metrics should be added together.
 * The key to group by should be called {@link key}
 */
public class MBeanGroupDiscovery extends MBeanResourceDiscoveryComponent {

    /**
     * Key to group by.
     * This would appear in the pattern as:
     * <pre>
     * org.apache.flume.channel:type=%key%
     * </pre>
     */
    public static final String key = "key";

    /**
     * Regex identifying the parts to strip from the key.
     * Note: Maybe attempt to configure this?
     */
    private static final Pattern pattern = Pattern.compile("(.*?)(-[\\d\\-]+)");

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Groups object names by a pattern seen in key.
     * Parent class does the work of finding the object names; we reduce this list
     * by stripping out the common pattern.
     */
    @Override
    public Set discoverResources(ResourceDiscoveryContext context) {
        Set<DiscoveredResourceDetails> drds = super.discoverResources(context);
        Set<DiscoveredResourceDetails> drds2 = new HashSet<DiscoveredResourceDetails>();
        Set<String> seen = new HashSet<String>();
        for (DiscoveredResourceDetails drd : drds) {
            Configuration conf = drd.getPluginConfiguration();
            String value = conf.getSimpleValue(key, null);
            if (value == null)
                throw new IllegalStateException(conf + " missing " + key);
            Matcher matcher = pattern.matcher(value);
            if (!matcher.matches()) {
                log.debug(value + " not matched " + pattern);
                continue;
            }
            String count = matcher.group(2);
            String objectName = conf.getSimpleValue(PROPERTY_OBJECT_NAME, null);
            objectName = objectName.replace(count, "*");
            log.debug("found name " + objectName);
            if (seen.add(objectName)) {
                conf.getSimple(PROPERTY_OBJECT_NAME).setValue(objectName);
                // clean up name and description
                drd.setResourceName(drd.getResourceName().replace(count, ""));
                drd.setResourceDescription(drd.getResourceDescription().replace(count, ""));
                drd.setResourceKey(objectName);
                log.debug("add " + drd);
                drds2.add(drd);
            } else {
                log.debug("ignore another " + objectName);
            }
        }
        return drds2;
    }

}