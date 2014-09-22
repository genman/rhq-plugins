package com.apple.iad.rhq.datatorrent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.apple.iad.rhq.http.HttpComponent;
import com.apple.iad.rhq.http.JSONTreeProvider;
import com.apple.iad.rhq.http.MeasurementProvider;

/**
 * Discovery for {@link AppComponent}.
 */
public class AppDiscovery implements ResourceDiscoveryComponent<GWComponent> {

    private static final String URL = "url";

    private static final String APPS_LIST = "/ws/v1/applications?state=RUNNING";

    static Pattern verp = Pattern.compile("(\\S+).*rev: (\\w+).*");

    static List<Map<String, Object>> getApps(GWComponent gw) throws Exception {
        Configuration c = new Configuration();
        c.setSimpleValue(URL, APPS_LIST);
        c.setSimpleValue("format", HttpComponent.Format.jsonTree.name());
        HttpComponent hc = new HttpComponent(c, gw);
        JSONTreeProvider jtp = (JSONTreeProvider) hc.getMeasurementProvider();
        Map m = (Map)jtp.getTree();
        return (List<Map<String, Object>>)m.get("apps");
    }

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<GWComponent> rdc)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();
        List<Map<String, Object>> l = getApps(rdc.getParentResourceComponent());
        if (l == null) {
            throw new Exception("no apps found");
        }
        for (Map<String, Object> app : l) {
            String name = (String) app.get("name");
            String id = (String) app.get("id");
            String cname = name.replace(".class", "").replace('/', '.');
            Configuration config = rdc.getDefaultPluginConfiguration();

            HttpComponent hc = new AppComponent(config, rdc.getParentResourceComponent(), id);
            MeasurementProvider mp = hc.getMeasurementProvider();
            String ver = (String) mp.extractValue("version");
            ver = getVersion(ver);
            DiscoveredResourceDetails drd = new DiscoveredResourceDetails(
                    rdc.getResourceType(), name, cname, ver, "DataTorrent application " + cname,
                    config, null);
            set.add(drd);
        }
        return set;

    }

    /**
     * Extracts short version from version stat.
     */
    static String getVersion(String ver) {
        Matcher matcher = verp.matcher(ver);
        if (matcher.matches()) {
            return matcher.group(1) + " " + matcher.group(2);
        } else {
            return ver;
        }
    }

}
