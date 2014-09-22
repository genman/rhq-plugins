package com.apple.iad.rhq.datatorrent;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceContext;

import com.apple.iad.rhq.http.HttpComponent;
import com.apple.iad.rhq.http.JSONTreeProvider;
import com.apple.iad.rhq.http.MeasurementProvider;
import com.apple.iad.rhq.http.UrlSource;

/**
 * Represents a data torrent application.
 */
public class AppComponent extends HttpComponent<GWComponent> {

    private static final String ID = "id";

    /**
     * Application is running.
     */
    private static final String RUNNING = "RUNNING";

    private static final String CONTAINER_URL =
            "/ws/v1/applications/@ID@/physicalPlan/containers?states=ACTIVE";

    private static final String OPERATOR_URL =
            "/ws/v1/applications/@ID@/physicalPlan/operators";

    /**
     * Parent component.
     */
    private GWComponent parent;

    /**
     * Resource key; which is the application name.
     */
    private String name;

    private volatile MeasurementProvider containerMP;
    private volatile MeasurementProvider operatorMP;
    private HttpComponent<?> containerData;
    private HttpComponent<?> operatorData;

    /**
     * Set through {@link #findId()}.
     * May be accessed through sub components, use volatile.
     */
    private volatile String id = "";

    /**
     * Constructs a new component.
     */
    public AppComponent() {
    }

    AppComponent(Configuration configuration, UrlSource source, String id) throws IOException {
        super(configuration, source);
        this.id = id;
    }

    @Override
    public void start(ResourceContext<GWComponent> context) throws IOException {
        super.start(context);
        parent = context.getParentResourceComponent();
        name = context.getResourceKey();
        findId();
    }

    private void findId() throws IOException {
        boolean match = false;
        try {
            for (Map<String, Object> app : AppDiscovery.getApps(parent)) {
                if (name.equals(app.get("name"))) {
                    id = (String) app.get(ID);
                    assert id != null;
                    match = true;
                }
            }
            if (!match) {
                log.warn("unable to find matching ID for " + name);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }

        Configuration config = new Configuration();
        String curl = CONTAINER_URL.replace("@ID@", id);
        config.setSimpleValue(PLUGINCONFIG_URL, curl);
        config.setSimpleValue(PLUGINCONFIG_FORMAT, Format.jsonTree.name());
        containerData = new HttpComponent(config, parent);

        config = config.deepCopy();
        String ourl = OPERATOR_URL.replace("@ID@", id);
        config.setSimpleValue(PLUGINCONFIG_URL, ourl);
        operatorData = new HttpComponent(config, parent);
    }

    static long windowId(Object o) {
        long l = Long.valueOf(o.toString());
        return l & 0xFFFF;
    }

    @Override
    protected MeasurementProvider getMeasurementProvider(String body) throws Exception {
        final MeasurementProvider mp = super.getMeasurementProvider(body);
        return new MeasurementProvider(body) {
            @Override
            public Object extractValue(String name) {
                Object o = mp.extractValue(name);
                if (name.endsWith("WindowId")) {
                    return windowId(o);
                } else {
                    return o;
                }
            }
        };
    }

    /**
     * Returns the application ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns UP if app is listed and in RUNNING state.
     */
    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType at = super.getAvailability();
        if (at == AvailabilityType.UP) {
            try {
                String state = (String) getMeasurementProvider().extractValue("state");
                if (!RUNNING.equals(state))
                    at = AvailabilityType.DOWN;
            } catch (Exception e) {
                log.warn("cannot obtain state " + getBody(), e);
                at = AvailabilityType.DOWN;
            }
        }
        if (at == AvailabilityType.DOWN) {
            log.debug("attempting to find correct ID");
            try {
                findId();
            } catch (IOException e) {
                log.debug("", e);
            }
            at = super.getAvailability();
        }
        return at;
    }

    /**
     * Tests this URL, also obtains container and operator data.
     */
    @Override
    protected boolean testUrl(URL url) throws IOException {
        boolean parent = super.testUrl(url);
        try {
            if (containerData != null)
                containerMP = containerData.getMeasurementProvider();
            if (operatorData != null)
                operatorMP = operatorData.getMeasurementProvider();
        } catch (Exception e) {
            throw new IOException(e);
        }
        return parent;
    }

    /**
     * Returns the list of containers for this app.
     */
    public List<Map<String, Object>> getContainerData() throws Exception {
        if (containerMP == null) {
            getAvailability();
        }
        if (containerMP == null) {
            log.debug("no container data");
            return emptyList();
        }
        JSONTreeProvider jtp = (JSONTreeProvider) containerMP;
        Map tree = (Map)jtp.getTree();
        return (List<Map<String,Object>>)tree.get("containers");
    }

    /**
     * Returns the list of operators (logical plan) for this app.
     * Operators are sorted by ID, descending.
     */
    public List<Map<String, Object>> getOperatorData() {
        if (operatorMP == null) {
            getAvailability();
        }
        if (operatorMP == null) {
            log.debug("no operator data");
            return emptyList();
        }
        JSONTreeProvider jtp = (JSONTreeProvider) operatorMP;
        Map tree = (Map)jtp.getTree();
        List<Map<String, Object>> l = (List<Map<String,Object>>)tree.get("operators");
        Collections.sort(l, new Comparator<Map<String, Object>>() {

            @Override
            public int compare(Map<String, Object> m1, Map<String, Object> m2) {
                String id1 = m1.get(ID).toString();
                String id2 = m2.get(ID).toString();
                return id1.compareTo(id2);
            }

        });
        return l;
    }

    @Override
    public URL getUrl() throws IOException {
        return resolveUrl("");
    }

    @Override
    protected URL resolveUrl(String urlStr) throws IOException {
        String url = "/ws/v1/applications/" + id;
        return super.resolveUrl(url);
    }

}
