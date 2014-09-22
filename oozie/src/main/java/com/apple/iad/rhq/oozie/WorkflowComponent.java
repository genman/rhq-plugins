package com.apple.iad.rhq.oozie;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

import com.apple.iad.rhq.http.HttpComponent;
import com.apple.iad.rhq.http.JSONTreeProvider;
import com.apple.iad.rhq.http.HttpComponent.Format;

/**
 * Job component, represents the most recent execution of a job by app ID.
 */
public class WorkflowComponent extends JobComponent {

    private static final long UNDEF = -1L;

    private HttpComponent jobStatus;

    private Date createdTime;

    private Date endTime;

    private Date lastModTime;

    private String user;

    private Date startTime;

    /**
     * Current elapsed time for this job, in milliseconds.
     */
    private long elapsed;

    /**
     * Time since this job last ended and succeeded, in milliseconds.
     * Small race condition: Recency can be set to UNDEF when it is defined.
     */
    private long recency;

    @Override
    public AvailabilityType getAvailability() {
        log.debug("avail for " + name);
        Map map;
        try {
            JSONTreeProvider tp = new JSONTreeProvider(jobStatus.getBody());
            map = (Map) tp.getTree();
        } catch (Exception e) {
            log.debug("cannot check " + e);
            return AvailabilityType.DOWN;
        }

        List<Map> workflows = (List<Map>)map.get("workflows");
        int c = 0;
        for (Map<String, String> m : workflows) {
            String appName = m.get("appName");
            if (appName.equals(this.name)) {
                try {
                    if (c == 0) { // most recent job
                        parse(m);
                    } else if (recency == UNDEF) { // prior job
                        status = Status.valueOf(m.get("status"));
                        endTime = parseDate(m.get("endTime"));
                        if (status == Status.SUCCEEDED && endTime != null) {
                            recency = System.currentTimeMillis() - endTime.getTime();
                        }
                    }
                } catch (ParseException e) {
                    log.warn("unable to parse " + m, e);
                }
                c++;
            }
        }
        if (status == null)
            log.warn("missing appName " + name);
        if (avail.contains(status))
            return AvailabilityType.UP;
        else
            return AvailabilityType.DOWN;
    }

    private void parse(Map<String, String> m) throws ParseException {
        status = Status.valueOf(m.get("status"));
        log.debug("status is " + status);
        createdTime = parseDate(m.get("createdTime"));
        startTime = parseDate(m.get("startTime"));
        endTime = parseDate(m.get("endTime"));
        lastModTime = parseDate(m.get("lastModTime"));
        if (createdTime != null) {
            if (endTime != null) {
                elapsed = endTime.getTime() - createdTime.getTime();
                recency = System.currentTimeMillis() - endTime.getTime();
            } else {
                elapsed = System.currentTimeMillis() - createdTime.getTime();
            }
        } else {
            elapsed = UNDEF;
            recency = UNDEF;
        }
        id = m.get("id");
        user = m.get("user");
    }

    @Override
    public void start(ResourceContext context)
            throws InvalidPluginConfigurationException, Exception {
        JobsComponent parent = (JobsComponent) context.getParentResourceComponent();
        name = context.getResourceKey();
        Configuration c = new Configuration();
        c.setSimpleValue(HttpComponent.PLUGINCONFIG_URL, "/oozie/v1/jobs?filter=name%3D" + name + "&len=2");
        c.setSimpleValue(HttpComponent.PLUGINCONFIG_FORMAT, Format.jsonTree.name());
        this.jobStatus = new HttpComponent<ResourceComponent<?>>(c, parent);
    }

    @Override
    public void stop() {
    }

    @Override
    public void getValues(MeasurementReport report,
            Set<MeasurementScheduleRequest> metrics) throws Exception {
        for (MeasurementScheduleRequest metric : metrics) {
            String name = metric.getName();
            if (name.equals("status"))
                report.addData(new MeasurementDataTrait(metric, status.toString()));
            else if (name.equals("createdTime") && createdTime != null)
                report.addData(new MeasurementDataTrait(metric, isoDF.format(createdTime)));
            else if (name.equals("startTime") && startTime != null)
                report.addData(new MeasurementDataTrait(metric, isoDF.format(startTime)));
            else if (name.equals("lastModTime") && lastModTime != null)
                report.addData(new MeasurementDataTrait(metric, isoDF.format(lastModTime)));
            else if (name.equals("endTime") && endTime != null)
                report.addData(new MeasurementDataTrait(metric, isoDF.format(endTime)));
            else if (name.equals("elapsed") && elapsed != UNDEF)
                report.addData(new MeasurementDataNumeric(metric, (double) elapsed));
            else if (name.equals("recency") && recency != UNDEF)
                report.addData(new MeasurementDataNumeric(metric, (double) recency));
            else if (name.equals("id"))
                report.addData(new MeasurementDataTrait(metric, id));
            else if (name.equals("user"))
                report.addData(new MeasurementDataTrait(metric, user));
        }
        /*
         * "appPath":null,
         * "status":"SUCCEEDED",
         * "createdTime":"Tue, 19 Feb 2013 18:53:40 GMT",
         * "conf":null,
         * "lastModTime":"Tue, 19 Feb 2013 18:55:26 GMT",
         * "endTime":"Tue, 19 Feb 2013 18:55:26 GMT",
         * "run":0,"externalId":null,
         * "appName":"sf-wf",
         * "id":"0000029-130218180147934-oozie-oozi-W",
         * "startTime":"Tue, 19 Feb 2013 18:53:40 GMT",
         * "toString":"Workflow id[0000029-130218180147934-oozie-oozi-W] status[SUCCEEDED]",
         * "group":"users","user":"iadetl",
         * "consoleUrl":"http:\/\/vp25q03ad-hadoopfeeder006.iad.apple.com:11000\/oozie?job=0000029-130218180147934-oozie-oozi-W",
         * "actions":[]
         */
    }

}
