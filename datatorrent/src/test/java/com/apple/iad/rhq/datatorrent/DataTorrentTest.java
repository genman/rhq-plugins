package com.apple.iad.rhq.datatorrent;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileWriter;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

@Test
public class DataTorrentTest extends ComponentTest {

    private final Log log = LogFactory.getLog(getClass());
    private String host = "vp25q03ad-app061.iad.apple.com";
    private ResourceComponent gw;
    private static final String name = "dtgateway";

    @Override
    protected void before() throws Exception {
        super.before();

        ResourceType type = getResourceType(name);
        Configuration configuration = getConfiguration(type);
        String url = configuration.getSimpleValue("url").replaceAll("hostname", host);
        configuration.setSimpleValue("url", url);

        gw = manuallyAdd(type, configuration);
    }

    public void testParse() throws Exception {
        for (Entry<ResourceComponent, Resource> me : this.components.entrySet()) {
            ResourceComponent rc = me.getKey();
            log.info("validating " + rc);
            assertUp(rc);
            if (rc instanceof MeasurementFacet) {
                MeasurementReport report = getMeasurementReport(rc);
                log.info(map(report));
                ResourceDescriptor rd = getResourceDescriptor(me.getValue().getResourceType());
                assertAll(report, rd);
            }
        }
    }

    @Test(enabled=false)
    public void testArg() throws Exception {
        ResourceType rt = getResourceType(name);
        Configuration c = new Configuration();
        c.setSimpleValue("url", "http://" + host + ":9090/ws/v1/about");
        GWComponent gw = (GWComponent) manuallyAdd(rt, c);
        components.remove(gw); // affects other tests

        gw.discovered(new String[0]);
        assertEquals(host, gw.getUrl().getHost());

        File file = File.createTempFile("dttest", ".txt");
        file.deleteOnExit();
        FileWriter fw = new FileWriter(file);
        gw.discovered(new String[] { "blah", "-findport", file.getAbsolutePath(), "bbb" });
        assertEquals(host, gw.getUrl().getHost());

        fw.write("xyz:123\n");
        fw.close();
        gw.discovered(new String[] { "blah", "-findport", file.getAbsolutePath(), "bbb" });

        assertEquals("xyz", gw.getUrl().getHost());
        assertEquals(123, gw.getUrl().getPort());
    }

    @Test(enabled=false)
    public void testKill() throws Exception {
        OperationFacet c = (OperationFacet)getComponent("container");
        OperationResult or = c.invokeOperation("kill", new Configuration());
        assertNotNull(or);
    }

    public void testUp() {
        assertUp(gw);
    }

}
