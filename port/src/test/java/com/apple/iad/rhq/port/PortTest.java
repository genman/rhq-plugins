package com.apple.iad.rhq.port;

import static org.testng.AssertJUnit.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.noderunner.http.servlet.ServletServer;

import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.port.PortDiscovery;
import com.apple.iad.rhq.testing.ComponentTest;

public class PortTest extends ComponentTest {

    private static final String PORT = "Port";
    private ServletServer ss;

    @SuppressWarnings("serial")
    class Servlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException { }
    }

    @Override
    protected void before() throws Exception {
        log.info("before");
        ss = new ServletServer(new Servlet());
        ss.start();
        super.before();
    }

    protected void afterClass() throws Exception {
        ResourceComponent<?> rc = getComponent(PORT);
        ss.close();
        assertDown(rc);
        super.afterClass();
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(PORT)) {
            // configuration.setSimpleValue(PortDiscovery.ADDRESS, "localhost:" + ss.getPort());
            ConfigurationDefinition configurationDefinition = resourceType.getPluginConfigurationDefinition();
            configurationDefinition.getDefaultTemplate().getConfiguration().
                getSimple(PortDiscovery.ADDRESS).
                setStringValue("localhost:" + ss.getPort());
            log.debug("" + configuration.getAllProperties());
        }
    }

    @Test
    public void addressConf() throws Exception {
        ResourceComponent<?> rc = getComponent(PORT);
        assertUp(rc);
        ResourceDescriptor rd = getResourceDescriptor(PORT);
        MeasurementReport report = getMeasurementReport(rc);
        log.info(report.getNumericData());
        try {
            assertAll(report, rd);
        } catch (AssertionError e) {
            log.error("cannot find", e);
        }
    }

    @Test
    public void fileParse() throws Exception {
        File temp = File.createTempFile("porttest", ".foo");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.newLine();
        out.write("connect.to = " + ss.getPort());
        out.newLine();
        out.close();

        int port = ss.getPort();
        Resource resource = getResource(PORT);
        assertEquals("Port " + port, resource.getName());
        assertEquals("" + port, resource.getResourceKey());
        ResourceType resourceType = resource.getResourceType();
        Configuration configuration = new Configuration();
        configuration.setSimpleValue(PortDiscovery.SOURCE, temp.toString());
        ResourceComponent rc = manuallyAdd(resourceType, configuration);
        assertUp(rc);
    }

}
