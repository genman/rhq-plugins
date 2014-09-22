package com.apple.iad.rhq.datatorrent;

import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;

import com.apple.iad.rhq.http.HttpComponent;
import com.apple.iad.rhq.http.JSONTreeProvider;
import com.apple.iad.rhq.http.MeasurementProvider;
import com.apple.iad.rhq.http.HttpComponent.Format;

/**
 * Represents a DataTorrent application operator.
 * Note that the resource is keyed off of name,
 */
public class OperatorComponent extends AppSubComponent {

    /**
     * Operator in active state.
     */
    private static final String ACTIVE = "ACTIVE";

    private static final String OPERATOR_PATH =
            "/ws/v1/applications/%s/physicalPlan/operators/%s";

    private String name;

    private int instance;

    private String opId = null;

    /**
     * Returns UP if container is found, and in active state.
     */
    public AvailabilityType getAvailability() {
        boolean found = false;
        try {
            List<Map<String, Object>> l = app.getOperatorData();
            int count = 0;
            for (Map<String, Object> op: l) {
                String name  = (String) op.get("name");
                opId = (String) op.get("id");
                if (name.equals(this.name) && instance == count++) {
                    this.stats = op;
                    String status = (String) stats.get("status");
                    if (!ACTIVE.equals(status)) {
                        return AvailabilityType.DOWN;
                    }
                    found = true;
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("operator " + this, e);
            return AvailabilityType.DOWN;
        }
        return found ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    /**
     * Returns JSON tree for details.
     * @throws Exception if JSON could not be obtained
     */
    public Map<String, Object> getDetailTree() throws Exception {
        if (opId == null) {
            getAvailability();
        }
        String path = String.format(OPERATOR_PATH, getAppId(), opId);
        Configuration config = new Configuration();
        config.setSimpleValue(HttpComponent.PLUGINCONFIG_URL, path);
        config.setSimpleValue(HttpComponent.PLUGINCONFIG_FORMAT, Format.jsonTree.name());
        HttpComponent detail = new HttpComponent(config, app);
        MeasurementProvider mp = detail.getMeasurementProvider();
        JSONTreeProvider jtp = (JSONTreeProvider) mp;
        return (Map<String, Object>)jtp.getTree();
    }

    /**
     * Return the name.
     */
    public String getName() {
        getAvailability();
        return (String) this.stats.get("name");
    }

    @Override
    public void start(ResourceContext<AppComponent> ac) throws InvalidPluginConfigurationException, Exception {
        super.start(ac);
        String[] split = key.split("\\|");
        if (split.length != 2)
            throw new InvalidPluginConfigurationException(key);
        name = split[0];
        instance = Integer.parseInt(split[1]);
    }


}
