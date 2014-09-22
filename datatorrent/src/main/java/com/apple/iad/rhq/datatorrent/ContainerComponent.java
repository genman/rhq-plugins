package com.apple.iad.rhq.datatorrent;

import java.util.Map;
import java.util.List;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

import com.apple.iad.rhq.http.HttpComponent;
import com.apple.iad.rhq.http.HttpComponent.Method;

/**
 * Represents a DataTorrent application container.
 */
public class ContainerComponent extends AppSubComponent implements OperationFacet {

    private static String CONTAINER_KILL =
            "/ws/v1/applications/%s/physicalPlan/containers/%s/kill";

    /**
     * Container in active state.
     */
    private static final String ACTIVE = "ACTIVE";

    /**
     * Returns UP if container is found, and in active state.
     */
    @Override
    public AvailabilityType getAvailability() {
        boolean found = false;
        try {
            List<Map<String, Object>> l = app.getContainerData();
            String fullKey = getFullKey();
            for (Map<String, Object> con: l) {
                String id = (String) con.get("id");
                if (id.equals(fullKey)) {
                    this.stats = con;
                    String state = (String) stats.get("state");
                    if (!ACTIVE.equals(state)) {
                        return AvailabilityType.DOWN;
                    }
                    found = true;
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("container " + this, e);
            return AvailabilityType.DOWN;
        }
        return found ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    String getFullKey() {
        // remove 'application'
        return "container" + app.getId().substring(11) + "_" + key;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration arg1) throws Exception {
        if (name.equals("kill")) {
            String path = String.format(CONTAINER_KILL, getFullKey());
            Configuration conf = new Configuration();
            conf.setSimpleValue(HttpComponent.PLUGINCONFIG_URL, path);
            conf.setSimpleValue(HttpComponent.PLUGINCONFIG_METHOD, Method.POST.name());
            HttpComponent hc = new HttpComponent<ResourceComponent<?>>(conf, app);
            String at = hc.getBody();
            return new OperationResult("result " + at);
        }
        throw new UnsupportedOperationException();
    }

}
