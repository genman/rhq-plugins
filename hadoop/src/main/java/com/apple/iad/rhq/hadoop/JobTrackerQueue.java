package com.apple.iad.rhq.hadoop;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Resource component representing an individual Hadoop job tracker queue.
 *
 */
public class JobTrackerQueue implements ResourceComponent<JobTrackerQueue>, MeasurementFacet {

    static final int TIMEOUT = 1000 * 10;
    private final Log log = LogFactory.getLog(getClass());
    private JobTrackerInfoHtml server;
    private String queue;

    @Override
    public AvailabilityType getAvailability() {
        try {
            getQueueData();
            return AvailabilityType.UP;
        } catch (IOException e) {
            return AvailabilityType.DOWN;
        }
    }

    @Override
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        queue = context.getResourceKey();
        server = (JobTrackerInfoHtml) context.getParentResourceComponent();
    }

    @Override
    public void stop() {
        server = null;
    }

    private QueueData getQueueData() throws IOException {
        return new QueueData(server.forQueue(queue));
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        QueueData queueData = getQueueData();
        log.debug("queue data " + queueData);
        for (MeasurementScheduleRequest mr : metrics) {
            String n = mr.getName();
            if (n.equals("jobCount"))
                report.addData(new MeasurementDataNumeric(mr, (double)queueData.jobCount));
            if (n.equals("mapTotal"))
                report.addData(new MeasurementDataNumeric(mr, (double)queueData.mapTotal));
            if (n.equals("mapComplete"))
                report.addData(new MeasurementDataNumeric(mr, (double)queueData.mapComplete));
            if (n.equals("reducersComplete"))
                report.addData(new MeasurementDataNumeric(mr, (double)queueData.reducersComplete));
            if (n.equals("reducersTotal"))
                report.addData(new MeasurementDataNumeric(mr, (double)queueData.reducersTotal));
        }
    }

}
