package com.apple.iad.rhq.mongodb;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

import com.apple.iad.rhq.mongodb.ReplClient.Member;
import com.apple.iad.rhq.mongodb.ReplClient.Member.Field;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;

/**
 * MongoDB component.
 *
 * @author Elias Ross
 */
public class MongoDBServerComponent implements ResourceComponent<MongoDBServerComponent>, MeasurementFacet {

    public static final Pattern pattern = Pattern.compile("\\.");

    public static final String URI = "uri";

    private final Log log = LogFactory.getLog(getClass());

    private Properties info = new Properties();

    private MongoClientURI uri;

    private MongoClient client;

    /**
     * Starts this instance.
     */
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        Configuration config = resourceContext.getPluginConfiguration();
        uri = new MongoClientURI(config.getSimpleValue(URI, "mongodb://localhost:27017"));
        client = new MongoClient(uri);
    }

    @Override
    public void stop() {
        info.clear();
    }

    public AvailabilityType getAvailability() {
        try {
            client.getDatabaseNames();
            return AvailabilityType.UP;
        } catch (Exception e) {
            log.debug("cannot get db names " + e);
        }
        return AvailabilityType.DOWN;
    }

    /**
     * Returns the client.
     */
    public MongoClient getClient() {
        return client;
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> msrs) throws Exception {
        // Does not work reliably, at least with 2.0.1 and below servers
        /*
        ReplicaSetStatus rss = client.getReplicaSetStatus();
        */

        ReplClient repl;
        try {
            repl = new ReplClient(client);
        } catch (MongoException e) {
            // client only works if replication is on
            repl = null;
        }

        StatClient stat = new StatClient(client);
        for (MeasurementScheduleRequest msr : msrs) {
            String name = msr.getName();
            if (name.equals("locked")) {
                mr.addData(new MeasurementDataTrait(msr, "" + client.isLocked()));
            } else if (name.equals("state")) {
                if (repl != null)
                    mr.addData(new MeasurementDataTrait(msr, "" + repl.getState()));
                else
                    mr.addData(new MeasurementDataTrait(msr, "UNREPLICATED"));
            } else if (name.equals("primary") && repl != null) {
                Member primary = repl.getPrimary();
                Object pname = "?";
                if (primary != null)
                    pname = (Object)primary.get(Field.name);
                mr.addData(new MeasurementDataTrait(msr, "" + pname));
            } else if (name.equals("replSetName") && repl != null) {
                mr.addData(new MeasurementDataTrait(msr, repl.getSet()));
            } else if (name.equals("globalLock.ratio")) {
                // In version 2 the ratio is no longer calculated by the server
                Map m = (Map) stat.get("globalLock");
                Number t = (Number) m.get("totalTime");
                Number l = (Number) m.get("lockTime");
                if (t != null && l != null)
                    mr.addData(new MeasurementDataNumeric(msr, l.doubleValue() / t.doubleValue()));
            } else {
                String s[] = pattern.split(name);
                Object o = stat.get(s);
                if (o != null)
                    mr.addData(new MeasurementDataNumeric(msr, ((Number) o).doubleValue()));
                else
                    log.debug("not available " + name);
            }
        }
    }

}
