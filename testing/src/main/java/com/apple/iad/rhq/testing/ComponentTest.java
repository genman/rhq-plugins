package com.apple.iad.rhq.testing;

import static java.util.Arrays.asList;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.util.ValidationEventCollector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.configuration.ConfigurationProperty;
import org.rhq.core.clientapi.descriptor.plugin.MetricDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor.Depends;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServiceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.availability.AvailabilityContextImpl;
import org.rhq.core.pc.content.ContentContextImpl;
import org.rhq.core.pc.inventory.InventoryContextImpl;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.operation.OperationContextImpl;
import org.rhq.core.pluginapi.availability.AvailabilityContext;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.system.pquery.ProcessInfoQuery;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Base class for RHQ Plugin Testing.
 * Initializes a plugin container and by default loads all descriptors in the classpath,
 * running discovery on each resource type (service/server) found.
 *
 * Methods to override:
 *
 * {@link #setConfiguration(Configuration, ResourceType)}
 */
public abstract class ComponentTest {

    /**
     * Logging component.
     */
    protected final Log log = LogFactory.getLog(getClass());

    private static File temp = new File(System.getProperty("java.io.tmpdir"));

    /**
     * Associates a resource component with a resource.
     */
    protected Map<ResourceComponent, Resource> components = new LinkedHashMap<ResourceComponent, Resource>();

    /**
     * Associates a resource type with a resource descriptor.
     */
    protected final Map<ResourceType, ResourceDescriptor> descriptors = new LinkedHashMap<ResourceType, ResourceDescriptor>();

    /**
     * Associates a name of a resource with a type.
     * This is useful for manually adding resources.
     *
     * @see #manuallyAdd(ResourceType, Configuration)
     * @see #getResourceType(String)
     */
    protected Map<String, ResourceType> resourceTypes = new LinkedHashMap<String, ResourceType>();

    private final Map<ResourceComponent, ResourceContext> resourceContext = new HashMap<ResourceComponent, ResourceContext>();

    /**
     * Plugins associated with descriptors.
     */
    private final Map<String, PluginDescriptor> plugins = new HashMap<String, PluginDescriptor>();
    private final Set<String> pluginsDone = new HashSet<String>();

    /**
     * Plugins with lifecycle listeners.
     */
    private final Map<String, PluginLifecycleListener> lifecycles = new LinkedHashMap<String, PluginLifecycleListener>();

    private final PluginContainer pluginContainer = PluginContainer.getInstance();

    private final SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

    /**
     * Scan all processes before starting; false will disable this feature.
     * Disabling is suggested for running tests against a remote instance.
     */
    private boolean processScan = true;

    // TODO

    private final PluginMetadataManager pmm = new PluginMetadataManager();
    private final File temporaryDirectory = temp;
    private final File dataDirectory = temp;
    private final String pluginContainerName = "rhq";
    private final OperationContext operationContext = new OperationContextImpl(0);
    private final ContentContext contentContext = new ContentContextImpl(0);
    private final PluginContainerDeployment pluginContainerDeployment = null;
    private Resource platform;
    private ResourceContainer platformContainer;
    private ResourceComponent platformComponent;
    private List<ProcessInfo> processInfo = Collections.emptyList();
    private PluginDescriptor pluginDescriptor;

    private int schedule;

    /**
     * Constructs a new component test.
     */
    protected ComponentTest() {
    }

    /**
     * Initializes the plugin container.
     * This calls the method {@link #before()}.
     */
    @BeforeClass
    protected void beforeClass() throws Exception {
        log.debug("beforeClass");
        // Speed up propagation of events by adjusting delay/period to 1 second
        PluginContainerConfiguration pcc = new PluginContainerConfiguration();
        pcc.setEventSenderInitialDelay(1);
        pcc.setEventSenderPeriod(1);
        pluginContainer.setConfiguration(pcc);
        pluginContainer.initialize();
        platform = pluginContainer.getInventoryManager().getPlatform();
        platformContainer = pluginContainer.getInventoryManager().getResourceContainer(platform);
        platformComponent = platformContainer.getResourceComponent();
        if (platformComponent == null) {
            throw new IllegalStateException("no platform component");
        }
        if (platformContainer == null) {
            platformContainer = new ResourceContainer(platform, getClass().getClassLoader());
        }
        resourceContext.put(platformComponent, platformContainer.getResourceContext());
        before();
    }

    /**
     * Initializes all plugins defined in the system classpath;
     * using auto-discovery where possible.
     * This is run once per test class.
     * Override this method to start components for discovery manually.
     */
    protected void before() throws Exception {
        log.debug("before");
        if (processScan) {
            processInfo = getProcessInfos();
            if (processInfo == null)
                processInfo = Collections.emptyList();
            log.debug("Process Info " + processInfo);
            for (ProcessInfo i : processInfo) {
                log.debug(i.getBaseName() + " " + Arrays.toString(i.getCommandLine()));
            }
        }

        Enumeration<URL> e = getClass().getClassLoader().getResources("META-INF/rhq-plugin.xml");
        List<URL> l = Collections.list(e);
        for (URL url : l) {
            InputStream is = url.openStream();
            log.debug("parse " + url);
            PluginDescriptor pd = loadDescriptor(is);
            plugins.put(pd.getName(), pd);
        }
        for (Map.Entry<String, PluginDescriptor> me : plugins.entrySet()) {
            process(me.getValue());
        }
    }

    /**
     * Process a plugin descriptor.
     * Processing happens in dependency order.
     * Algorithm is not the most efficient, but whatever.
     */
    private void process(PluginDescriptor pd) throws Exception {
        for (Depends d : pd.getDepends()) {
            PluginDescriptor pd2 = plugins.get(d.getPlugin());
            if (pd2 == null) {
                log.error("required plugin " + d + " not found by " + pd.getName());
                continue;
            }
            process(pd2);
        }
        if (pluginsDone.add(pd.getName())) {
            processPluginDescriptor(pd);
            buildDesc(pd.getServers());
            buildDesc(pd.getServices());
        }
    }

    /**
     * Parses and loads a particular plugin descriptor by stream, typically
     * obtained by calling {@link Class#getResourceAsStream(String)}.
     */
    protected PluginDescriptor loadDescriptor(InputStream is) throws Exception {
        ValidationEventCollector vec = new ValidationEventCollector();
        PluginDescriptor pd = AgentPluginDescriptorUtil.parsePluginDescriptor(is, vec);
        if (vec.hasEvents())
            log.warn("Validation failed " + asList(vec.getEvents()));
        return pd;
    }

    /**
     * Empty method provided to correspond with {@link #before()}.
     * Called by {@link #afterClass()}.
     */
    protected void after() throws Exception {
    }

    /**
     * Process a plugin descriptor.
     */
    private void processPluginDescriptor(PluginDescriptor pd) throws Exception {
        this.pluginDescriptor = pd;
        Set<ResourceType> types = pmm.loadPlugin(pd);
        log.debug("Plugin names " + pmm.getPluginNames());
        if (types == null) {
            throw new IllegalStateException("no types for descriptor " + pd.getName());
        }
        String cl = pd.getPluginLifecycleListener();
        if (cl != null && !cl.isEmpty()) {
            if (!cl.contains(".")) {
                cl = pd.getPackage() + "." + cl;
            }
            Class<PluginLifecycleListener> c = (Class<PluginLifecycleListener>) Class.forName(cl);
            PluginLifecycleListener pll = c.newInstance();
            String name = pd.getName();
            PluginContext pc = new PluginContext(name, systemInfo, temporaryDirectory, dataDirectory, pluginContainerName);
            pll.initialize(pc);
            lifecycles.put(name, pll);
        }
        mapResourceTypeNames(types);
        log.info("Resource types: " + resourceTypes);
        resources(types, platform, platformComponent);
        log.info("ResourceComponent map: " + components);
    }

    private void mapResourceTypeNames(Set<ResourceType> types) {
        for (ResourceType type : types) {

            // validate display name uniqueness
            Set<String> names = new HashSet<String>();
            for (MeasurementDefinition md : type.getMetricDefinitions()) {
                assert names.add(md.getDisplayName()) : type + " has duplicate display name " + md;
            }

            this.resourceTypes.put(type.getName(), type);
            mapResourceTypeNames(type.getChildResourceTypes());
        }
    }

    /**
     * Manually create a component by name.
     * @param name which is the resource type name; note that this can be ambiguous
     */
    public ResourceComponent manuallyAdd(String name) throws Exception {
        ResourceType resourceType = resourceTypes.get(name);
        if (resourceType == null)
            throw new IllegalStateException("no type " + name);
        Configuration configuration = resourceType.getPluginConfigurationDefinition().getDefaultTemplate().createConfiguration();
        setConfiguration(configuration, resourceType);
        return manuallyAdd(resourceType, configuration);
    }

    /**
     * Manually create a component by resource type.
     */
    public ResourceComponent manuallyAdd(ResourceType type, Configuration configuration) throws Exception {
        return manuallyAdd(type, configuration, platformContainer.getResourceComponent());
    }

    /**
     * Manually create a component by resource type, configuration, parent.
     */
    public ResourceComponent manuallyAdd(ResourceType type, Configuration configuration, ResourceComponent parent) throws Exception {
        String s = pmm.getDiscoveryClass(type);
        ResourceDiscoveryComponent rdc = (ResourceDiscoveryComponent) Class.forName(s).newInstance();
        ManualAddFacet maf = (ManualAddFacet)rdc;
        ResourceContext parentResourceContext = platformContainer.getResourceContext();
        ResourceDiscoveryContext resourceDiscoveryContext = new ResourceDiscoveryContext(type, parent,
                parentResourceContext, systemInfo,
                performProcessScans(type), Collections.emptyList(),
                pluginContainerName, pluginContainerDeployment);
        DiscoveredResourceDetails drd = maf.discoverResource(configuration, resourceDiscoveryContext);
        return createChild(drd, platform, configuration, parent, rdc);
    }

    private ResourceComponent createChild(DiscoveredResourceDetails drd,
            Resource resource,
            Configuration configuration,
            ResourceComponent parentComponent,
            ResourceDiscoveryComponent rdc) throws Exception
    {
        ResourceType type = pmm.getType(drd.getResourceType());

        Resource cresource = new Resource();
        cresource.setResourceType(type);
        cresource.setPluginConfiguration(configuration);
        cresource.setResourceKey(drd.getResourceKey());
        cresource.setParentResource(resource);
        cresource.setName(drd.getResourceName());
        cresource.setVersion(drd.getResourceVersion());
        cresource.setDescription(drd.getResourceDescription());
        cresource.setUuid(uuid());

        String rclassname = pmm.getComponentClass(type);
        ResourceComponent component = (ResourceComponent) Class.forName(rclassname).newInstance();

        EventContext eventContext = new TestEventContext();
        ResourceContext parentContext = resourceContext.get(parentComponent);
        InventoryManager inventoryManager = pluginContainer.getInventoryManager();
        AvailabilityContext availContext = new AvailabilityContextImpl(cresource, inventoryManager);
        ResourceContext context = new ResourceContext(cresource, parentComponent,
                parentContext, rdc, systemInfo, temporaryDirectory, dataDirectory,
                pluginContainerName, eventContext, operationContext, contentContext,
                availContext, new InventoryContextImpl(cresource),
                pluginContainerDeployment);

        try {
            component.start(context);
        } catch (Exception e) {
            // TODO shouldn't this exception be thrown?
            log.error("failed to start component " + component, e);
        }
        components.put(component, cresource);
        resourceContext.put(component, context);
        resources(type.getChildResourceTypes(), cresource, component);
        return component;
    }

    /**
     * Returns the test event context for this context.
     */
    public TestEventContext getEventContext(ResourceComponent<?> component) {
        return (TestEventContext)getContext(component).getEventContext();
    }

    /**
     * Returns the resource context for this component.
     * Returns null if not found.
     */
    public ResourceContext getContext(ResourceComponent<?> component) {
        return resourceContext.get(component);
    }

    /**
     * Restarts the resource component.
     * This is useful for 'rebooting' a component for testing.
     */
    public void restart(ResourceComponent<?> component) throws Exception {
        component.stop();
        component.start(getContext(component));
    }

    private String uuid() {
        return UUID.randomUUID().toString();
    }

    private void resources(Set<ResourceType> types, Resource parent, ResourceComponent component) throws Exception {
        if (component == null) {
            throw new NullPointerException("component");
        }
        for (ResourceType type : types) {
            discover(type, parent, component);
        }

    }

    /**
     * Run discovery scan for this resource type and parent component.
     */
    protected void discover(ResourceType type, ResourceComponent parent) throws Exception {
        discover(type, components.get(parent), parent);
    }

    /**
     * Run discovery scan for this resource type under the platform component.
     */
    protected void discover(ResourceType type) throws Exception {
        discover(type, platformComponent);
    }

    private void discover(ResourceType type, Resource parent, ResourceComponent component) throws Exception {
        Set<ResourceType> supported = type.getParentResourceTypes();
        if (!supported.isEmpty() && !supported.contains(parent.getResourceType())) {
            log.debug("not discovering " + type + " inside " + parent.getResourceType() + "; supported is " + supported);
            return;
        }
        String s = pmm.getDiscoveryClass(type);
        if (s == null) {
            throw new NullPointerException("no discovery " + type);
        }
        ResourceDiscoveryComponent rdc = (ResourceDiscoveryComponent) Class.forName(s).newInstance();
        log.debug("rdc=" + rdc);
        ResourceContext context = resourceContext.get(component);
        if (context == null) {
            throw new NullPointerException("no context " + component);
        }
        ResourceDiscoveryContext resourceDiscoveryContext = new ResourceDiscoveryContext(type, component,
                context, systemInfo,
                performProcessScans(type), Collections.emptyList(),
                pluginContainerName, pluginContainerDeployment);
        ConfigurationDefinition def = type.getPluginConfigurationDefinition();
        Configuration config;
        if (def == null) {
            config = new Configuration();
        } else {
            ConfigurationTemplate template = def.getDefaultTemplate();
            if (template == null) {
                config = new Configuration();
            } else {
                config = template.getConfiguration();
            }
        }
        setConfiguration(config, type);
        Set<DiscoveredResourceDetails> drds;
        try {
            drds = rdc.discoverResources(resourceDiscoveryContext);
        } catch (Exception e) {
            log.warn("failed discovery " + type, e);
            return;
        }
        Map<Configuration, Boolean> confs = new IdentityHashMap<Configuration, Boolean>();
        for (DiscoveredResourceDetails drd : drds) {
            log.debug("discovered " + drd);
            ResourceType resourceType = drd.getResourceType();
            Configuration c = drd.getPluginConfiguration();
            if (confs.put(c, true) != null)
                throw new IllegalStateException("returned multiple resources that point to the same plugin configuration object");
            setConfiguration(drd.getPluginConfiguration(), resourceType);
            ResourceComponent child = createChild(drd, parent, drd.getPluginConfiguration(), component, rdc);
        }
        if (drds.isEmpty()) {
            log.warn("not discovered " + type);
            context.getPluginConfiguration();
        }
    }

    /***
     * Returns a default discovery context for the given resource type.
     */
    protected ResourceDiscoveryContext platformDiscoveryContext(ResourceType type) {
        return new ResourceDiscoveryContext(type,
                platformContainer.getResourceComponent(),
                platformContainer.getResourceContext(), systemInfo,
                performProcessScans(type), Collections.emptyList(),
                pluginContainerName, pluginContainerDeployment);
    }

    /**
     * Called before the configuration is processed; override to set specific plugin parameters.
     */
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
    }

    /**
     * Stops all components, stops the plugin container.
     */
    @AfterClass
    protected void afterClass() throws Exception {
        for (ResourceComponent c : components.keySet()) {
            c.stop();
            getEventContext(c).close();
        }
        components.clear();
        resourceContext.clear();
        for (Entry<String, PluginLifecycleListener> entry : lifecycles.entrySet()) {
            String plugin = entry.getKey();
            log.debug("shutdown " + plugin);
            entry.getValue().shutdown();
        }
        PluginContainer.getInstance().shutdown();
        after();
    }

    /**
     * Returns a measurement report for the given resource component.
     * The resource must implement {@link MeasurementFacet}.
     */
    public MeasurementReport getMeasurementReport(ResourceComponent component) throws Exception {
        Resource resource = this.components.get(component);
        return getMeasurementReport(resource, component);
    }

    private static MeasurementScheduleRequest msr(MeasurementData md) {
        return new MeasurementScheduleRequest(md.getScheduleId(), md.getName(), 0, true, null);
    }

    /**
     * Returns a measurement report for the given resource and component.
     * The resource must implement {@link MeasurementFacet}.
     */
    public MeasurementReport getMeasurementReport(Resource resource, ResourceComponent component) throws Exception {
        MeasurementReport report = new MeasurementReport();
        ResourceType type = resource.getResourceType();
        Set<MeasurementScheduleRequest> s = new LinkedHashSet<MeasurementScheduleRequest>();
        for (MeasurementDefinition md : type.getMetricDefinitions()) {
            MeasurementSchedule ms = new MeasurementSchedule(md, resource);
            ms.setId(schedule++);
            s.add(new MeasurementScheduleRequest(ms));
        }
        ((MeasurementFacet)component).getValues(report, s);
        for (MeasurementDataNumeric n : report.getNumericData()) {
            boolean remove = s.remove(msr(n));
            if (!remove)
                log.error("metric not requested but found " + n);
        }
        for (MeasurementDataTrait n : report.getTraitData()) {
            boolean remove = s.remove(msr(n));
            if (!remove)
                log.error("metric not requested but found " + n);
        }
        return report;
    }

    /**
     * Returns true if the component exists or not.
     */
    public boolean hasComponent(String name) {
        ResourceComponent c = getComponent0(name);
        return c != null;
    }

    /**
     * Returns the first resource component by resource name, then looks by matching resource type name,
     * then by resource key, then by resource name, then asserts failure if not found.
     * Note that this name can be ambiguous if it is shared by multiple plugins.
     */
    public ResourceComponent getComponent(String name) {
        ResourceComponent c = getComponent0(name);
        if (c == null)
            fail("component not found " + name + " in " + components.entrySet());
        return c;
    }

    private ResourceComponent getComponent0(String name) {
        for (Map.Entry<ResourceComponent, Resource> c : components.entrySet())
            if (c.getValue().getName().equals(name))
                return c.getKey();
        for (Map.Entry<ResourceComponent, Resource> c : components.entrySet())
            if (c.getValue().getResourceType().getName().equals(name))
                return c.getKey();
        for (Map.Entry<ResourceComponent, Resource> c : components.entrySet())
            if (c.getValue().getResourceKey().equals(name))
                return c.getKey();
        for (Map.Entry<ResourceComponent, Resource> c : components.entrySet())
            if (c.getValue().getName().equals(name))
                return c.getKey();
        return null;
    }

    /**
     * Returns a resource matching this component.
     */
    public Resource getResource(ResourceComponent rc) {
        Resource r = components.get(rc);
        if (r == null)
            throw new IllegalStateException("not found");
        return r;
    }

    /**
     * Returns a resource matching this name.
     * This may be ambiguous if multiple resources share the same name.
     */
    public Resource getResource(String name) {
        return getResource(getComponent(name));
    }

    /**
     * Builds a new configuration for a resource type.
     */
    public Configuration getConfiguration(ResourceType resourceType) {
        if (resourceType == null)
            throw new NullPointerException("ResourceType");
        Configuration configuration = resourceType.getPluginConfigurationDefinition().getDefaultTemplate().createConfiguration();
        setConfiguration(configuration, resourceType);
        return configuration;
    }

    // ASSERT METHOD

    /**
     * From a measurement report, returns a measurement value, or asserts failure if no such value exists.
     * @param name name of the measurement
     */
    public static Double getValue(MeasurementReport report, String name) {
        for (MeasurementDataNumeric m: report.getNumericData()) {
            if (m.getName().equals(name)) {
                return m.getValue();
            }
        }
        fail("report does not incude " + name + " report " + report.getNumericData());
        return null;
    }

    /**
     * Asserts the resource component is available.
     */
    public static void assertUp(ResourceComponent component) {
        assertEquals("up " + component, AvailabilityType.UP, component.getAvailability());
    }

    /**
     * Asserts the resource component is unavailable.
     */
    public static void assertDown(ResourceComponent component) {
        assertEquals("down " + component, AvailabilityType.DOWN, component.getAvailability());
    }

    /**
     * Sets a configuration option.
     */
    public static void set(Configuration config, String name, String value) {
        PropertySimple s = config.getSimple(name);
        if (s == null) {
            s = new PropertySimple(name, value);
            config.put(s);
        } else {
            s.setStringValue(value);
        }
    }

    private List<ProcessScanResult> performProcessScans(ResourceType serverType) {
        List<ProcessScanResult> scanResults = new ArrayList<ProcessScanResult>();
        Set<ProcessScan> processScans = serverType.getProcessScans();
        log.debug("Executing process scans for server type " + serverType + "...");
        ProcessInfoQuery piq = new ProcessInfoQuery(processInfo);
        for (ProcessScan processScan : processScans) {
            List<ProcessInfo> queryResults = piq.query(processScan.getQuery());
            for (ProcessInfo autoDiscoveredProcess : queryResults) {
                scanResults.add(new ProcessScanResult(processScan, autoDiscoveredProcess));
                log.info("Process scan auto-detected new server resource: scan=[" + processScan
                        + "], discovered-process=[" + autoDiscoveredProcess + "]");
            }
        }
        return scanResults;
    }

    /**
     * AutoDiscoveryExecutor method.
     */
    private List<ProcessInfo> getProcessInfos() {
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        log.debug("Retrieving process table...");
        long startTime = System.currentTimeMillis();
        List<ProcessInfo> processInfos = null;
        try {
            processInfos = systemInfo.getAllProcesses();
        } catch (UnsupportedOperationException uoe) {
            log.debug("Cannot perform process scan - not supported on this platform. (" + systemInfo.getClass() + ")");
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("Retrieval of process table took " + elapsedTime + " ms.");
        return processInfos;
    }

    /**
     * Returns the plugin descriptor.
     */
    public PluginDescriptor getPluginDescriptor() {
        return pluginDescriptor;
    }

    /**
     * Returns a resource type by name, or throws an exception if not found.
     */
    public ResourceType getResourceType(String name) {
        ResourceType resourceType = resourceTypes.get(name);
        if (resourceType == null)
            fail("resource type not found " + name + " in " + resourceTypes.keySet());
        return resourceType;
    }

    /**
     * Returns the plugin descriptor by looking up by resource type name.
     * Note this may be ambiguous (resolve incorrectly) if multiple plugins share
     * the same resource type name.
     * @throws IllegalStateException if not found
     */
    public ResourceDescriptor getResourceDescriptor(String name) {
        ResourceType resourceType = getResourceType(name);
        return getResourceDescriptor(resourceType);
    }

    /**
     * Returns the plugin descriptor by looking up by resource type.
     * @throws IllegalStateException if not found
     */
    public ResourceDescriptor getResourceDescriptor(ResourceType type) {
        ResourceDescriptor rd = descriptors.get(type);
        if (rd == null)
            throw new IllegalStateException("no descriptor " + type + " in " + descriptors.keySet());
        return rd;
    }

    private void buildDesc(List<? extends ResourceDescriptor> l) {
        for (ResourceDescriptor rd : l) {
            // resourceTypes holds the correct mapping
            ResourceType resourceType = resourceTypes.get(rd.getName());
            descriptors.put(resourceType, rd);
            if (rd instanceof ServerDescriptor) {
                buildDesc(((ServerDescriptor)rd).getServers());
                buildDesc(((ServerDescriptor)rd).getServices());
            }
            if (rd instanceof ServiceDescriptor) {
                buildDesc(((ServiceDescriptor)rd).getServices());
                buildDesc(((ServiceDescriptor)rd).getServices());
            }
        }
    }

    /**
     * Asserts that all measurements in the report are present
     * according to the resource descriptor.
     *
     * @see #getResourceDescriptor(String) for obtaining this.
     * @param report
     */
    public static void assertAll(MeasurementReport report, ResourceDescriptor l) {
        HashMap<String, MetricDescriptor> map = new HashMap<String, MetricDescriptor>();
        for (MetricDescriptor md : l.getMetric()) {
            map.put(md.getProperty(), md);
        }
        for (MeasurementDataNumeric n : report.getNumericData()) {
            map.remove(n.getName());
        }
        for (MeasurementDataTrait n : report.getTraitData()) {
            map.remove(n.getName());
        }
        map.remove(MeasurementDefinition.AVAILABILITY_NAME);
        assertTrue("Measurements not found " + map.keySet(), map.isEmpty());
    }

    /**
     * Converts a report into a map, where the measurement names are
     * mapped to either a Double for measurements or String for traits.
     */
    public static Map<String, Object> map(MeasurementReport report) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        for (MeasurementDataNumeric n : report.getNumericData()) {
            map.put(n.getName(), n.getValue());
        }
        for (MeasurementDataTrait n : report.getTraitData()) {
            map.put(n.getName(), n.getValue());
        }
        return map;
    }

    /**
     * Set to false to avoid scanning local machine processes to speed up testing.
     */
    public void setProcessScan(boolean processScan) {
        this.processScan = processScan;
    }

    /**
     * Asserts all values defined in the resource descriptor do not return null
     * for a resource implementing configuration facet.
     */
    public void assertAll(ConfigurationFacet cf, ResourceDescriptor rd) throws Exception {
        Configuration config = cf.loadResourceConfiguration();
        List<JAXBElement<? extends ConfigurationProperty>> templates = rd.getResourceConfiguration().getConfigurationProperty();
        for (JAXBElement<? extends ConfigurationProperty> template : templates) {
            String name = template.getValue().getName();
            // Property property = config.get(name);
            assertNotNull("config contains " + name, config.get(name));
            Object value = config.getSimpleValue(name, null);
            assertNotNull("value for " + name, value);
            log.debug("config found " + name + " value " + value );
        }
    }

    /**
     * Returns a component by name or resource key and also by component class.
     */
    public <T> T getComponent(String name, Class<T> clazz) {
        for (Map.Entry<ResourceComponent, Resource> c : components.entrySet()) {
            ResourceComponent<?> rc = c.getKey();
            if (!rc.getClass().isAssignableFrom(clazz)) {
                continue;
            }
            Resource r = c.getValue();
            if (r.getName().equals(name))
                return (T)rc;
            if (r.getResourceKey().equals(name))
                return (T)rc;
        }
        fail("component not found " + name + " in " + components.entrySet());
        return null;
    }

}
