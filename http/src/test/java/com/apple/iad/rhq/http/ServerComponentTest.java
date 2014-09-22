package com.apple.iad.rhq.http;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.noderunner.http.servlet.ServletServer;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

/**
 * Tests {@link ServerComponent}.
 */
public class ServerComponentTest extends ComponentTest {

    private static final String HTTP_SERVER = "HTTP Server";
    private ServletServer ss;
    private long sleep;
    private int code = HttpServletResponse.SC_OK;
    private String body;
    private final String regexApple = "apple: (\\d+)";
    private final String regexBanana = "banana: (\\d+)";
    private String url;
    private HttpComponent component;
    private final String xpath = "//t/text()";
    private final String title = "title";
    private final String body_title = "/body/title";
    private File temp;
    private Servlet servlet;

    @SuppressWarnings("serial")
    class Servlet extends HttpServlet {

        String posted;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            log.info("doGet");
            if (code != HttpServletResponse.SC_OK) {
                resp.sendError(code, "epic fail");
            }
            resp.addHeader("x-head", "13");
            resp.getWriter().print(body);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            log.info("doPost");
            posted = req.getReader().readLine();
            try {
                resp.addHeader("content-type", "text/plain");
                PrintWriter writer = resp.getWriter();
                writer.println("hello");
                writer.close();
            } catch (Exception e) {
                log.error(e, e);
            }
            log.info("done");
        }
    }

    public ServerComponentTest() {
        setProcessScan(false);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(HTTP_SERVER)) {
            log.info("setConfiguration " + configuration.getProperties());
            this.url = "http://hostname:" + ss.getPort() + "/";
            configuration.getSimple(HttpComponent.PLUGINCONFIG_URL).setStringValue(url);
            configuration.getSimple("timeout").setIntegerValue(1);
            // for discovery below:
            ConfigurationDefinition configurationDefinition = resourceType.getPluginConfigurationDefinition();
            configurationDefinition.getDefaultTemplate().getConfiguration().
                getSimple(HttpComponent.PLUGINCONFIG_URL).setStringValue(url);
            // TODO probe multiple ports
            // String ports = ss.getPort() + ", " + (ss.getPort() + 1);
            // configuration.getSimple("ports").setStringValue(ports);
            addMeasurementDefinition(resourceType, regexApple);
            addMeasurementDefinition(resourceType, regexBanana);
            addMeasurementDefinition(resourceType, xpath);
            addMeasurementDefinition(resourceType, title);
            addMeasurementDefinition(resourceType, body_title);
            addMeasurementDefinition(resourceType, "#x-head");
            addMeasurementDefinition(resourceType, "#x-fail");
        }
        if (resourceType.getName().equals("post")) {
            String url = "http://localhost:" + ss.getPort() + "/post";
            ConfigurationDefinition configurationDefinition = resourceType.getPluginConfigurationDefinition();
            configurationDefinition.getDefaultTemplate().getConfiguration().
                getSimple(HttpComponent.PLUGINCONFIG_URL).setStringValue(url);
        }
        if (resourceType.getName().equals("file")) {
            try {
                temp = File.createTempFile("rhq-plugin", ".txt");
                FileWriter w = new FileWriter(temp);
                w.write("count 42\r\n");
                w.close();
                temp.deleteOnExit();
                configuration.getSimple(HttpComponent.PLUGINCONFIG_URL).setStringValue(temp.toURI().toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @BeforeMethod
    public void reset() {
        code = HttpServletResponse.SC_OK;
        body = "apple: 42  banana: 49";
        sleep = 0;
        component.getConfiguration().setSimpleValue("regex", null);
    }

    @Override
    protected void before() throws Exception {
        log.info("before");
        servlet = new Servlet();
        ss = new ServletServer(servlet, 8889);
        ss.start();
        super.before();
        component = (HttpComponent) getComponent(HTTP_SERVER);
    }

    @Override
    protected void afterClass() throws Exception {
        super.afterClass();
        ss.close();
    }

    private void addMeasurementDefinition(ResourceType type, String s) {
        DisplayType displayType = DisplayType.SUMMARY;
        MeasurementDefinition md = new MeasurementDefinition(s, MeasurementCategory.PERFORMANCE,
                MeasurementUnits.BYTES, DataType.MEASUREMENT, NumericType.DYNAMIC, true, 0, displayType);
        type.getMetricDefinitions().add(md);
    }

    @Test
    public void up() throws Exception {
        log.info("testUp");
        sleep = 150;
        assertEquals(AvailabilityType.UP, component.getAvailability());
        MeasurementReport report = getMeasurementReport(component);
        Double rt = getValue(report, "responseTime");
        assertNotNull(rt);
        assertTrue(rt > 100 && rt < 200);
        assertEquals((double)body.length(), getValue(report, "responseSize"));

        assertEquals(42.0, getValue(report, regexApple));
        assertEquals(49.0, getValue(report, regexBanana));
        assertEquals(13.0, ComponentTest.map(report).get("#x-head"));

        // TODO figure out how to test this
        assertEquals("ServletServer_Servlet", component.getVersion());
        component.clearState();
        assertEquals(null, component.getVersion());
        assertEquals(AvailabilityType.UP, component.getAvailability());
    }

    @Test(dependsOnMethods={"up"})
    public void xml() throws Exception {
        log.info("xml; test xpath");
        body = "<i><t>999</t></i>";
        component.getAvailability();
        component.setFormat(HttpComponent.Format.xml);
        MeasurementReport report = getMeasurementReport(component);
        assertEquals(999.0, getValue(report, xpath));
    }

    @Test(dependsOnMethods={"up"})
    public void xpathOperation() throws Exception {
        log.info("xml; xpath operation");
        body = "<i><t>999</t></i>";
        component.setFormat(HttpComponent.Format.xml);
        Configuration parameters = new Configuration();
        parameters.setSimpleValue("extract", xpath);
        OperationResult result = component.invokeOperation("test", parameters);
        Configuration conf = result.getComplexResults();
        log.debug("RESULT " + conf.getMap());
        assertEquals("true", conf.getSimpleValue("success"));
        assertEquals("999", conf.getSimpleValue("response"));
        assertEquals("", conf.getSimpleValue("request"));
        assertEquals("200", conf.getSimpleValue("responseCode"));

        parameters.setSimpleValue("extract", null);
        result = component.invokeOperation("test", parameters);
        conf = result.getComplexResults();
        assertEquals(body, conf.getSimpleValue("response"));
    }

    @Test(dependsOnMethods={"up"})
    public void json() throws Exception {
        log.info("json");
        body = "{ \"body\": { \"title\": 66 } }";
        component.getAvailability();
        component.setFormat(HttpComponent.Format.json);
        MeasurementReport report = getMeasurementReport(component);
        assertEquals(66.0, getValue(report, title));
    }

    @Test(dependsOnMethods={"up"})
    public void jsonTree() throws Exception {
        log.info("jsonTree");
        body = "{ \"body\": { \"title\": 69 } }";
        component.getAvailability();
        component.setFormat(HttpComponent.Format.jsonTree);
        MeasurementReport report = getMeasurementReport(component);
        assertEquals(69.0, getValue(report, body_title));
    }

    @Test(dependsOnMethods={"up"})
    public void down() throws Exception {
        log.info("down");
        this.code = 500;
        assertEquals(AvailabilityType.DOWN, component.getAvailability());
        component.getConfiguration().setSimpleValue(HttpComponent.PLUGINCONFIG_STATUS, null);
        assertEquals(AvailabilityType.UP, component.getAvailability());
    }

    @Test(dependsOnMethods={"up"})
    public void regEx() throws Exception {
        log.info("testRegEx");
        Configuration configuration = component.getConfiguration();
        log.debug("reg config " + configuration.getProperties());
        configuration.setSimpleValue("regex", "banana");
        assertEquals(AvailabilityType.UP, component.getAvailability());
        log.info("zucchini");
        configuration.setSimpleValue("regex", "zucchini");
        assertEquals(AvailabilityType.DOWN, component.getAvailability());
    }

    @Test
    public void discovery() throws Exception {
        HttpDiscovery discovery = new HttpDiscovery();
        Resource resource = getResource(HTTP_SERVER);
        ResourceType resourceType = resource.getResourceType();
        ResourceDiscoveryContext resourceDiscoveryContext = platformDiscoveryContext(resourceType);
        Set<DiscoveredResourceDetails> details = discovery.discoverResources(resourceDiscoveryContext);
        log.info("details " + details);
        boolean found = false;
        String url2 = url.replaceAll("hostname", InetAddress.getLocalHost().getHostName());
        for (DiscoveredResourceDetails d : details) {
            if (d.getResourceKey().equals(url2))
                found = true;
        }
        assertTrue("resource " + url2 + " not in " + details, found);
        code = 404;
        details = discovery.discoverResources(resourceDiscoveryContext);
        assertEquals(0, details.size());
    }

    @Test
    public void file() throws Exception {
        ResourceComponent c = getComponent("file");
        assertUp(c);
        MeasurementReport report = getMeasurementReport(c);
        Map<String, Object> next = ComponentTest.map(report);
        assertEquals(42.0d, next.get("count (\\d+)"));

        Configuration conf = new Configuration();
        conf.setSimpleValue(HttpComponent.PLUGINCONFIG_URL, temp.toURI().toURL().toString());
        HttpComponent hc = new HttpComponent(conf, (HttpComponent)c);
        assertUp(hc);

        temp.delete();
        assertDown(c);
        assertDown(hc);

        assert resourceTypes.containsKey("not here");
        for (Map.Entry<ResourceComponent, Resource> c2 : components.entrySet())
            if (c2.getValue().getResourceType().getName().equals("not here"))
                assert false;
    }

    @Test
    public void post() {
        log.info("post");
        ResourceComponent c = getComponent("post");
        assertUp(c);
        assertEquals("posties", this.servlet.posted);
    }

    @Test
    public void directory() throws Exception {
        ResourceType type = getResourceType(HTTP_SERVER);
        Configuration configuration = new Configuration();
        configuration.setSimpleValue("url", "file:/tmp/");
        ResourceComponent component = manuallyAdd(type, configuration);
        assertUp(component);
        MeasurementReport report = getMeasurementReport(component);
        log.info("root directory " + map(report));

        /* TODO mkdir /tmp/bbb and run
        Configuration conf2 = new Configuration();
        conf2.setSimpleValue("url", "bbb");
        HttpComponent component2 = (HttpComponent) manuallyAdd(type, conf2, component);
        assertEquals("file:/tmp/bbb", component2.getUrl());
        log.info("tmp directory " + map(report));
        */
    }

    @Test
    public void subclass() throws Exception {
        log.info("subclass");
        SubclassComponent c = (SubclassComponent) getComponent(SubclassDiscovery.NAME);
        assertUp(c);
        Resource r = getResource(c);
        assertEquals("posties", this.servlet.posted);
        assertEquals(SubclassDiscovery.KEY, r.getResourceKey());
        assertEquals(SubclassDiscovery.NAME, r.getName());
        assertEquals(SubclassDiscovery.VER, r.getVersion());
        assertEquals(SubclassComponent.BODY, c.getBody());
        assertEquals("3", c.getMeasurementProvider().extractValue("foo").toString());
    }

}
