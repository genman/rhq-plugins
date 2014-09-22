package com.apple.iad.rhq.hadoop;

import static org.testng.AssertJUnit.assertEquals;

import java.io.InputStream;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

public class HadoopComponentTest extends ComponentTest {

    private final boolean remote = false;

    @Override
    protected void before() throws Exception {
        log.info("before");
        String p = System.getProperty("org.hyperic.sigar.path");
        if (p == null)
            throw new NullPointerException("sigar not found");
        super.before();
        log.info("descriptors " + descriptors);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (remote) {
            set(configuration, JMXDiscoveryComponent.CONNECTION_TYPE, J2SE5ConnectionTypeDescriptor.class.getName());
            set2(configuration, JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY, "hadoop.connectorAddress");
            set2(configuration, JMXComponent.PRINCIPAL_CONFIG_PROP, "hadoop.principal");
            set2(configuration, JMXComponent.CREDENTIALS_CONFIG_PROP, "hadoop.credentials");
        }
        if (resourceType.getName().equals("NameNode")) {
            configuration.getSimple("dfs.test.bytes").setStringValue("1000");
            configuration.getSimple("dfs.test.files").setStringValue("1");
        }
    }

    private void set2(Configuration config, String name, String value) {
        String s = System.getProperty(value);
        if (s == null)
            throw new NullPointerException(value + " not set");
        super.set(config, name, s);
    }

    @Test
    public void measurements() throws Exception {
        log.info("measurements");
        Pattern pattern = Pattern.compile("JVM|Hive|Html|JMX|JobTrackerQueue");
        for (Map.Entry<ResourceType, ResourceDescriptor> me: this.descriptors.entrySet()) {
            ResourceType type = me.getKey();
            String name = type.getName();
            if (pattern.matcher(name).find())
                continue;
            ResourceComponent<?> rc = getComponent(name);
            ResourceDescriptor rd = getResourceDescriptor(type);
            if (rc instanceof MeasurementFacet) {
                MeasurementReport report = getMeasurementReport(rc);
                log.info("testing " + name);
                try {
                    assertAll(report, rd);
                } catch (AssertionError e) {
                    log.error("cannot find", e);
                }
            }
        }
    }

    @Test
    public void zookeeper() throws Exception {
        log.info("zookeeper");

        String name = "Zookeeper";
        Resource r = getResource(name);
        AssertJUnit.assertNotNull(r.getVersion());

        name = "Standalone Server StandaloneServer_port-1";
        ResourceComponent<?> rc = getComponent(name);
        ResourceDescriptor rd = getResourceDescriptor("Zookeeper Server");
        MeasurementReport report = getMeasurementReport(rc);
        assertAll(report, rd);
        ConfigurationFacet cf = (ConfigurationFacet) rc;
        assertAll(cf, rd);

        name = "InMemoryDataTree StandaloneServer_port-1";
        rc = getComponent(name);
        rd = getResourceDescriptor("InMemoryDataTree");
        report = getMeasurementReport(rc);
        assertAll(report, rd);
    }

    @Test
    public void fsck() throws Exception {
        log.info("FSCK");
        InputStream is = getClass().getResourceAsStream("/fsck-open.txt");
        assert is != null;
        NameNodeComponent nnc = (NameNodeComponent) getComponent("NameNode");
        String name = "NameNode FSCK";
        ResourceType type = this.resourceTypes.get(name);
        Configuration configuration = new Configuration();
        configuration.setSimpleValue("path", "/");
        configuration.setSimpleValue("ignore.age", "5");
        NameNodeFsckComponent fsck = (NameNodeFsckComponent) manuallyAdd(type, configuration, nnc);

        Path tmp = new Path("/tmp/openfile");
        assertUp(nnc);
        FSDataOutputStream create = nnc.getFileSystem().create(tmp, true);
        create.write(new byte[1024]);
        create.flush();
        openCount(fsck, 0);
        Thread.sleep(1000 * 6);
        openCount(fsck, 1);
        create.close();
        Thread.sleep(1000);
        openCount(fsck, 0);

        log.info("count open");
        fsck.countOpen(is);
        log.info("FSCK DONE");
    }

    void openCount(NameNodeFsckComponent fsck, int count) throws Exception {
        MeasurementReport report;
        report = getMeasurementReport(fsck);
        assertEquals((double)count, map(report).get("OpenForWrite"));
    }

    @Test
    public void nna() throws Exception {
        log.info("nna");
        ResourceComponent rc = getComponent("NameNodeActivity");
        ResourceDescriptor rd = getResourceDescriptor("NameNodeActivity");
        MeasurementReport report = getMeasurementReport(rc);
        assertAll(report, rd);
    }

}
