package com.apple.iad.rhq.oozie;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Job component; base class for coordinator or workflow jobs.
 */
public abstract class JobComponent implements ResourceComponent, MeasurementFacet {

    protected final Log log = LogFactory.getLog(getClass());

    /**
     * "Tue, 19 Feb 2013 18:53:40 GMT", used by Oozie.
     */
    protected final DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

    /**
     * ISO format.
     */
    protected final DateFormat isoDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    {
        isoDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected String name;

    /**
     * From Oozie client.
     */
    public static enum Status {
        PREMATER, PREP, RUNNING, SUSPENDED, SUCCEEDED, KILLED, FAILED,
        PAUSED, PREPPAUSED, PREPSUSPENDED, RUNNINGWITHERROR, SUSPENDEDWITHERROR, PAUSEDWITHERROR, DONEWITHERROR
    }

    /**
     * Considered UP.
     */
    protected static final Set<Status> avail = EnumSet.<JobComponent.Status>of(
            Status.PREMATER, Status.PREP, Status.RUNNING, Status.SUCCEEDED
            );

    /**
     * Current status.
     */
    protected Status status;

    /**
     * Parent component.
     */
    protected JobsComponent parent;

    /**
     * Job ID.
     */
    protected String id;

    /**
     * Parses Oozie date format (default JDK?)
     */
    protected Date parseDate(String string) throws ParseException {
        if (string == null)
            return null;
        return df.parse(string);
    }

    @Override
    public void start(ResourceContext context)
            throws InvalidPluginConfigurationException, Exception {
        parent = (JobsComponent) context.getParentResourceComponent();
        name = context.getResourceKey();
    }

    @Override
    public void stop() {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [name=" + name + ", status=" + status + ", id=" + id + "]";
    }

}
