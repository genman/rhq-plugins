package com.apple.iad.rhq.snmp;

import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.util.CommonTimer;
import org.snmp4j.util.ThreadFactory;
import org.snmp4j.util.TimerFactory;
import org.snmp4j.util.WorkerTask;

/**
 * Thread pool provider class for the SNMP4J library.
 * This class is intended to limit the number of threads created in RHQ
 * as well as detect conditions where threads are consumed without bounds.
 */
public class Threading implements ThreadFactory, TimerFactory, java.util.concurrent.ThreadFactory, RejectedExecutionHandler {

    private final Log log = LogFactory.getLog(getClass());
    private final boolean debug = log.isDebugEnabled();
    private final AtomicInteger count = new AtomicInteger(0);
    private final ExecutorService es;
    private final long joinTimeout;

    /**
     * Wraps a {@link ScheduledExecutorService}.
     * TODO: This class is frequently created and destroyed; should probably
     * just use the simple java.util.Timer class instead.
     */
    class CommonTimerImpl implements CommonTimer, java.util.concurrent.ThreadFactory {

        private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1, this);
        private final AtomicInteger count2 = new AtomicInteger(0);
        private final int count = Threading.this.count.getAndIncrement();

        CommonTimerImpl() {
            if (debug)
                log.debug("created CommonTimerImpl-" + count);
        }

        @Override
        public void schedule(TimerTask task, long delay) {
            ses.schedule(task, delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public void schedule(TimerTask task, Date firstTime, long period) {
            long delay = firstTime.getTime() - System.currentTimeMillis();
            if (delay < 0)
                delay = 0;
            ses.scheduleWithFixedDelay(task, delay, period, TimeUnit.MILLISECONDS);
        }

        @Override
        public void schedule(TimerTask task, long delay, long period) {
            ses.scheduleWithFixedDelay(task, delay, period, TimeUnit.MILLISECONDS);
        }

        @Override
        public void cancel() {
            ses.shutdownNow();
            if (debug)
                log.debug("canceled CommonTimerImpl-" + count);
        }

        @Override
        public Thread newThread(Runnable run) {
            Thread t = new Thread(run);
            t.setName("snmp-timer-" + count + "-" + count2.getAndIncrement());
            return t;
        }

    }

    /**
     * Constructs a new instance, limiting the total number of threads to count.
     */
    public Threading(int count) {
        es = new ThreadPoolExecutor(count, count, 1000L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(100), this, this);
        joinTimeout = SNMP4JSettings.getThreadJoinTimeout();
    }

    @Override
    public CommonTimer createTimer() {
        return new CommonTimerImpl();
    }

    @Override
    public WorkerTask createWorkerThread(final String name, final WorkerTask task, boolean daemon) {
        return new WorkerTask() {
            Future<?> future;

            @Override
            public void run() {
                // TODO set the name ... not clear if necessary though
                future = es.submit(task);
            }

            @Override
            public void terminate() {
                task.terminate();
                future.cancel(false);
            }

            @Override
            public void join() throws InterruptedException {
                try {
                    future.get(joinTimeout, TimeUnit.MILLISECONDS);
                } catch (ExecutionException e) {
                    // we don't really need to know; SNMP lib throws socket exceptions
                    log.trace(e);
                } catch (TimeoutException e) {
                    log.trace(e);
                }
            }

            @Override
            public void interrupt() {
                future.cancel(true);
                task.interrupt();
            }
        };
    }

    @Override
    public Thread newThread(Runnable run) {
        Thread t = new Thread(run);
        t.setName("snmp-" + count.getAndIncrement());
        return t;
    }

    /**
     * Throws an exception after logging an error.
     */
    @Override
    public void rejectedExecution(Runnable run, ThreadPoolExecutor tpe) {
        log.error("thread pool full; throwing exception here", new Throwable());
        throw new RejectedExecutionException();
    }

    /**
     * Stops threading.
     */
    public void close() {
        es.shutdownNow();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

}
