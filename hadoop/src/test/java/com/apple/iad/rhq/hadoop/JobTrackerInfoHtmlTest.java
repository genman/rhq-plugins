package com.apple.iad.rhq.hadoop;

import java.net.URL;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

import com.apple.iad.rhq.testing.ComponentTest;

@Test
public class JobTrackerInfoHtmlTest extends ComponentTest {

    private final Log log = LogFactory.getLog(getClass());
    private static final String name = "JobTrackerInfoHtml";
    String url = "http://vg61l01ad-hadoop002:50030/jobtracker.jsp";
    JobTrackerInfoHtml html;

    @Override
    protected void before() throws Exception {
        setProcessScan(false);
        super.before();
        html = (JobTrackerInfoHtml) manuallyAdd(name);
    }

    @Override
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
        if (resourceType.getName().equals(name)) {
            log.info("conf " + configuration);
            configuration.getSimple("url").setStringValue(url);
        }
    }

    public void testReport() throws Exception {
        assertUp(html);
        MeasurementReport mr = getMeasurementReport(html);
        log.info("report " + mr);
        assertAll(mr, this.getResourceDescriptor(name));
    }

    public void testQueues() throws Exception {
        ResourceComponent rc = getComponent("Queue adj_etl");
        MeasurementReport measurementReport = getMeasurementReport(rc);
        ResourceDescriptor resourceDescriptor = getResourceDescriptor("JobTrackerQueue");
        assertAll(measurementReport, resourceDescriptor);
        JobTrackerQueueDiscovery qd = new JobTrackerQueueDiscovery();
        Set<String> queues = qd.getQueues(new URL(url));
        for (String s : queues) {
            log.info("queue " + s);
            log.info(new QueueData(html.forQueue(s)));
        }
    }


}
