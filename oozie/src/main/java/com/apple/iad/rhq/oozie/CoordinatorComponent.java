package com.apple.iad.rhq.oozie;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

import com.apple.iad.rhq.http.JSONTreeProvider;

/**
 * Job component, represents the most recent execution of a job by app ID.
 */
public class CoordinatorComponent extends JobComponent {

    private static final String START_TIME = "startTime";
    private static final String LAST_ACTION = "lastAction";
    private Date startTime;
    private Date lastAction;
    private Map<String, Object> traits = new HashMap<String, Object>();

    @SuppressWarnings("unchecked")
    @Override
    public AvailabilityType getAvailability() {
        log.debug("avail for " + name);
        Map map;
        try {
            JSONTreeProvider tp = (JSONTreeProvider) parent.getMeasurementProvider();
            map = (Map) tp.getTree();
        } catch (Exception e) {
            log.debug("cannot check " + e);
            return AvailabilityType.DOWN;
        }

        List<Map> jobs = (List<Map>)map.get("coordinatorjobs");
        status = null;
        for (Map<String, String> m : jobs) {
            String cname = m.get("coordJobName");
            if (cname.equals(this.name)) {
                try {
                    traits.putAll(m);
                    status = Status.valueOf(m.get("status"));
                    id = m.get("coordJobId");
                    startTime = parseDate(m.get(START_TIME));
                    if (startTime != null) {
                        traits.put(START_TIME, isoDF.format(startTime));
                    }
                    lastAction = parseDate(m.get(LAST_ACTION));
                    if (lastAction != null) {
                        traits.put(LAST_ACTION, isoDF.format(lastAction));
                    }
                } catch (ParseException e) {
                    log.warn("unable to parse " + m, e);
                }
                break;
            }
        }
        if (status == null)
            log.warn("missing " + name);
        if (avail.contains(status))
            return AvailabilityType.UP;
        else
            return AvailabilityType.DOWN;
    }

    @Override
    public void getValues(MeasurementReport report,
            Set<MeasurementScheduleRequest> metrics) throws Exception {
        getAvailability();
        long now = System.currentTimeMillis();
        for (MeasurementScheduleRequest metric : metrics) {
            String name = metric.getName();
            Object trait = traits.get(name);
            if (name.equals("startElapsed") && startTime != null) {
                report.addData(new MeasurementDataNumeric(metric, (double) (now - startTime.getTime())));
            } else if (name.equals("actionElapsed") && lastAction != null) {
                report.addData(new MeasurementDataNumeric(metric, (double) (lastAction.getTime() - now)));
            } else if (trait != null) {
                report.addData(new MeasurementDataTrait(metric, s(trait)));
            }
        }
    }

    private String s(Object trait) {
        return trait == null ? null : trait.toString();
    }

}
