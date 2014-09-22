package com.apple.iad.rhq.splunk;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

import com.splunk.Service;
import com.splunk.ServiceInfo;

/**
 * Splunk monitoring component.
 * Works on regular Splunk and Splunk Forwarder instances.
 */
public class SplunkComponent implements ResourceComponent<ResourceComponent<?>>, MeasurementFacet {

    private final Log log = LogFactory.getLog(getClass());

    private MetricsTail tail;

    /**
     * Monitor this configuration.
     */
    private Configuration config;

    /**
     * Service information.
     */
    private ServiceInfo serviceInfo;

    @Override
    public void start(ResourceContext context) throws Exception {
        config = context.getPluginConfiguration();
        String metricsLog = config.getSimpleValue("metricsLog", "var/log/splunk/metrics.log");
        String home = config.getSimpleValue("home", "/opt/splunk");
        File logfile;
        if (metricsLog.startsWith(File.separator)) {
            logfile = new File(metricsLog);
        } else {
            logfile = new File(home + File.separator + metricsLog);
        }
        this.tail = new MetricsTail(logfile);
    }

    /**
     * Triggers 'tail' of metrics log file; parsing values from {@link #names} and storing
     * in {@link #values}.
     */
    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        try {
            tail.read();
        } catch (IOException e) {
            log.warn("could not tail " + tail, e);
        }

        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();
            if (name.equals("guid")) {
                report.addData(new MeasurementDataTrait(request, serviceInfo.getGuid()));
                continue;
            }
            if (name.equals("mode")) {
                report.addData(new MeasurementDataTrait(request, serviceInfo.getMode()));
                continue;
            }
            if (name.equals("licenseState")) {
                report.addData(new MeasurementDataTrait(request, serviceInfo.getLicenseState()));
                continue;
            }
            Double d = tail.getValue(name);
            if (d == null) {
                log.warn("measurement not found " + name);
                continue;
            }
            report.addData(new MeasurementDataNumeric(request, d));
        }
    }

    /**
     * Returns UP if the information can be obtained and
     * the metrics file can be read.
     */
    @Override
    public AvailabilityType getAvailability() {
        try {
            log.debug("probe");
            Service service = SplunkDiscovery.service(config);
            serviceInfo = service.getInfo();
            service.logout();
            /*
            Splunk file rotation makes the metrics file unreadable
            This is Splunk case 117457
            log.debug("tail");
            tail.read();
            */
            log.debug("done");
            return AvailabilityType.UP;
        } catch (Exception e) {
            log.debug("failed checking splunk", e);
            return AvailabilityType.DOWN;
        }
    }

    @Override
    public void stop() {
    }

}