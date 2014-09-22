package com.apple.iad.rhq.mongodb;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

import com.mongodb.CommandResult;
import com.mongodb.MongoException;

/**
 * MongoDB component.
 *
 * @author Elias Ross
 */
public class MongoDBComponent implements ResourceComponent<MongoDBServerComponent>, MeasurementFacet, DeleteResourceFacet {

    public static final String DB = "db";

    private static final Log log = LogFactory.getLog(MongoDBComponent.class);

    private MongoDBServerComponent parent;

    private String dbname;

    private CommandResult stats;

    /**
     * Starts this instance.
     */
    public void start(ResourceContext<MongoDBServerComponent> resourceContext) throws InvalidPluginConfigurationException, Exception {
        Configuration config = resourceContext.getPluginConfiguration();
        parent = resourceContext.getParentResourceComponent();
        dbname = config.getSimpleValue(DB, "admin");
    }

    @Override
    public void stop() {
        parent = null;
    }

    public AvailabilityType getAvailability() {
        try {
            stats = getDB().getStats();
            return AvailabilityType.UP;
        } catch (MongoException e) {
            log.debug("cannot get stats " + e);
        }
        return AvailabilityType.DOWN;
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> msrs) throws Exception {
        getAvailability();
        for (MeasurementScheduleRequest msr : msrs) {
            Object o = stats.get(msr.getName());
            if (o != null) {
                if (msr.getDataType() == DataType.TRAIT) {
                    mr.addData(new MeasurementDataTrait(msr, o.toString()));
                } else {
                    Number n = (Number)o;
                    mr.addData(new MeasurementDataNumeric(msr, n.doubleValue()));
                }
            } else {
                log.warn("not found " + msr.getName());
            }
        }
    }

    /**
     * Returns the database.
     */
    public com.mongodb.DB getDB() {
        return parent.getClient().getDB(dbname);
    }

    /**
     * Checks if this database is a master or not.
     */
    public boolean isMaster() {
        return ReplClient.isMaster(parent.getClient());
    }

    /**
     * Drops this database.
     */
    @Override
    public void deleteResource() throws Exception {
        log.info("drop " + dbname);
        getDB().dropDatabase();
    }

}
