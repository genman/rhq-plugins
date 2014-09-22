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

import com.mongodb.DB;
import com.mongodb.DBCollection;

/**
 * MongoDB collection component.
 *
 * @author Elias Ross
 */
public class MongoDBCollectionComponent implements ResourceComponent<MongoDBComponent>, MeasurementFacet, DeleteResourceFacet {

    public static final String COLLECTION = "collection";

    private static final Log log = LogFactory.getLog(MongoDBCollectionComponent.class);

    private MongoDBComponent parent;

    private String colname;

    private DBCollection collection;

    /**
     * Starts this instance.
     */
    public void start(ResourceContext<MongoDBComponent> resourceContext) throws InvalidPluginConfigurationException, Exception {
        Configuration config = resourceContext.getPluginConfiguration();
        parent = resourceContext.getParentResourceComponent();
        colname = config.getSimpleValue(COLLECTION, "");
    }

    @Override
    public void stop() {
        parent = null;
    }

    public AvailabilityType getAvailability() {
        DB db = parent.getDB();
        boolean contains = db.getCollectionNames().contains(colname);
        if (contains) {
            collection = db.getCollection(colname);
            return AvailabilityType.UP;
        } else {
            collection = null;
            return AvailabilityType.DOWN;
        }
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> msrs) throws Exception {
        getAvailability();
        for (MeasurementScheduleRequest msr : msrs) {
            Object o = collection.getStats().get(msr.getName());
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

    @Override
    public void deleteResource() throws Exception {
        log.info("dropping " + collection);
        collection.drop();
    }

}
