package com.apple.iad.rhq.testing;

import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.SigarProxy;
import org.rhq.core.domain.event.Event;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.EventPoller;

/**
 * Test event context stores events generated.
 */
public class TestEventContext implements EventContext {

    protected final Log log = LogFactory.getLog(getClass());

    private ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

    private List<Event> events = synchronizedList(new ArrayList<Event>());
    private Map<String, ScheduledFuture> scheduled = new ConcurrentHashMap<String, ScheduledFuture>();

    @Override
    public void publishEvent(Event event) {
        events.add(event);
    }

    /**
     * Returns the events for this context.
     */
    public List<Event> getEvents() {
        return new ArrayList(events);
    }

    @Override
    public void registerEventPoller(EventPoller poller, int pollingInterval) {
        registerEventPoller(poller, pollingInterval, "");
    }

    @Override
    public void registerEventPoller(final EventPoller poller, int pollingInterval, String sourceLocation) {
        Runnable run = new Runnable() {
            public void run() {
                events.addAll(poller.poll());
            }
        };
        ScheduledFuture<?> sf = pool.scheduleAtFixedRate(run, pollingInterval, pollingInterval, TimeUnit.SECONDS);
        scheduled.put(poller.getEventType() + sourceLocation, sf);
    }

    @Override
    public void unregisterEventPoller(String eventType) {
        log.info("unregisterEventPoller " + eventType);
        ScheduledFuture future = scheduled.remove(eventType);
        if (future != null)
            future.cancel(true);
    }

    @Override
    public void unregisterEventPoller(String eventType, String sourceLocation) {
        log.info("unregisterEventPoller " + eventType + " " + sourceLocation);
        ScheduledFuture future = scheduled.remove(eventType + sourceLocation);
        if (future != null)
            future.cancel(true);
    }

    @Override
    public SigarProxy getSigar() {
        return null;
    }

    /**
     * Closes the thread pool.
     */
    public void close() {
        pool.shutdownNow();
        clear();
    }

    /**
     * Clear the existing events.
     */
    public void clear() {
        events.clear();
    }

    @Override
    public String toString() {
        return "TestEventContext [pool=" + pool + ", events=" + events + ", scheduled=" + scheduled + "]";
    }

}
