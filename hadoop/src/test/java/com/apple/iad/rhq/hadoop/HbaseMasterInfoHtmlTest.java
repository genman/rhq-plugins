package com.apple.iad.rhq.hadoop;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

@Test
public class HbaseMasterInfoHtmlTest extends ComponentTest {

    private final Log log = LogFactory.getLog(getClass());
    private static final String name = "HbaseMasterInfoHtml";
    String url  = "http://vg61l01ad-hadoop001.apple.com:60010/master.jsp";
    String url2 = "http://localhost:60010/master-status";
    HbaseMasterInfoHtml html;

    @Override
    protected void before() throws Exception {
        setProcessScan(false);
        super.before();
        html = (HbaseMasterInfoHtml) manuallyAdd(name);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(name)) {
            log.info("conf " + configuration);
            configuration.getSimple("url").setStringValue(url2);
        }
    }

    public void testReport() throws Exception {
        assertUp(html);
        MeasurementReport mr = getMeasurementReport(html);
        log.info("report " + mr.getTraitData());
        log.info("report " + mr.getNumericData());
        assertAll(mr, this.getResourceDescriptor(name));
    }

}

