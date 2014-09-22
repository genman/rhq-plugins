package com.apple.iad.rhq.oozie;

import static org.testng.AssertJUnit.assertEquals;

import java.net.URL;
import java.util.Map.Entry;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

/**
 * Tests {@link ServerComponent}.
 */
public class OozieTest extends ComponentTest {

    private static String OOZIE = "Oozie Server";
    private static String JOB = "Oozie Job";
    private static String INST = "Oozie Instrumentation";

    public OozieTest() {
        setProcessScan(false);
    }

    @Test
    public void testCommandLine() throws Exception {
        String cdh4 = "java -Djava.util.logging.config.file=/opt/cloudera/parcels/CDH-4.3.0-1.cdh4.3.0.p0.22/lib/oozie/oozie-server-0.20/conf/logging.properties -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager " +
                "-Xmx1073741824 -Doozie.home.dir=/opt/cloudera/parcels/CDH-4.3.0-1.cdh4.3.0.p0.22/lib/oozie -Doozie.config.dir=/var/run/cloudera-scm-agent/process/33477-oozie-OOZIE_SERVER " +
                "-Doozie.log.dir=/var/log/oozie -Doozie.log.file=oozie-cmf-oozie1-OOZIE_SERVER-.out -Doozie.config.file=oozie-site.xml -Doozie.log4j.file=log4j.properties -Doozie.log4j.reload=10 -Doozie.http.hostname=hosty -Doozie.http.port=11004 -Djava.net.preferIPv4Stack=true -Doozie.admin.port=11001 -Dderby.stream.error.file=/var/log/oozie/derby.log -Doozie.https.keystore.file= -Doozie.https.keystore.pass= -Doozie.https.port= -Djava.endorsed.dirs=/opt/cloudera/parcels/CDH-4.3.0-1.cdh4.3.0.p0.22/lib/bigtop-tomcat/endorsed -classpath /opt/cloudera/parcels/CDH-4.3.0-1.cdh4.3.0.p0.22/lib/bigtop-tomcat/bin/bootstrap.jar -Dcatalina.base=/opt/cloudera/parcels/CDH-4.3.0-1.cdh4.3.0.p0.22/lib/oozie/oozie-server-0.20 -Dcatalina.home=/opt/cloudera/parcels/CDH-4.3.0-1.cdh4.3.0.p0.22/lib/bigtop-tomcat -Djava.io.tmpdir=/var/run/cloudera-scm-agent/process/33477-oozie-OOZIE_SERVER/temp org.apache.catalina.startup.Bootstrap start";
        OozieDiscovery discovery = new OozieDiscovery();
        URL url = discovery.getUrl(cdh4.split(" "));
        assertEquals("http://hosty:11004/oozie", url.toString());

        url = discovery.getUrl("java foo".split(" "));
        assertEquals("http://localhost:11000/oozie", url.toString());

        url = discovery.getUrl("java -Doozie.base.url=http://example.net".split(" "));
        assertEquals("http://example.net", url.toString());
    }

    @Test
    public void up() throws Exception {
        log.info("testUp");
        Configuration c = new Configuration();
        c.setSimpleValue("url", "http://vp25q03ad-hadoopfeeder006.iad.apple.com:11000/oozie"); // http://vp25q03ad-hadoop087.iad.apple.com:11000/oozie");
        // getResourceType(OOZIE);
        ResourceComponent component = manuallyAdd(getResourceType(OOZIE), c);
        log.info("assertUp");
        assertUp(component);
        ResourceComponent inst = getComponent(INST);
        log.info("inst");
        assertUp(inst);
        MeasurementReport mr = getMeasurementReport(inst);
        log.info(INST + " " + map(mr));

        for (Entry<ResourceComponent, Resource> e : components.entrySet()) {
            ResourceComponent rc = e.getKey();
            log.info("avail " + rc.getAvailability());
            if (rc instanceof JobComponent) {
                mr = getMeasurementReport(rc);
                log.info(JOB + " " + map(mr));
                // assertAll(mr, getResourceDescriptor(JOB));
            }
        }
    }

}
