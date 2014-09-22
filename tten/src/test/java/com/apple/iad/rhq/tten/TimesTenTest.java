package com.apple.iad.rhq.tten;

import static org.rhq.plugins.database.BasePooledConnectionProvider.PASSWORD;
import static org.rhq.plugins.database.BasePooledConnectionProvider.URL;
import static org.rhq.plugins.database.BasePooledConnectionProvider.USERNAME;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.PrintWriter;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

public class TimesTenTest extends ComponentTest {

    private static final String TIMES_TEN = "TimesTen";
    private ResourceComponent<?> server;

    @Override
    protected void before() throws Exception {
        log.info("before");
        com.timesten.jdbc.Logger.setLogWriter(new PrintWriter(System.out));
        super.before();

        ResourceType rt = getResourceType(TIMES_TEN);
        Configuration configuration = getConfiguration(rt);
        String url = System.getProperty("jdbc.url", "jdbc:timesten:client:ttc_server=vp25q03ad-oracle034.iad.apple.com;tcp_port=53397;dsn=vp2_master2");
        configuration.setSimpleValue(URL, url);
        configuration.setSimpleValue(USERNAME, "iadm");
        configuration.setSimpleValue(PASSWORD, "iadm");
        configuration.setSimpleValue("java.library.path", "iadm");
        log.info(configuration.getProperties());

        server = manuallyAdd(rt, configuration);
        assertNotNull(server);

        MeasurementReport report = getMeasurementReport(server);
        assertAll(report, getResourceDescriptor(rt));
    }

    @AfterTest
    protected void after() throws Exception {
        super.after();
    }

    @Test
    public void test() {
        assertUp(server);
    }

}
