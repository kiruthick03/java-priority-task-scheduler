package com.example.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/** Worker thread that executes tasks in order of (dueTime, priority). */
final class Worker extends Thread {
    private static final Logger log = LoggerFactory.getLogger(Worker.class);

    private final PriorityBlockingQueue<ScheduledTask> queue;
    private final TaskRegistry registry;
    private final AtomicBoolean shutdown;

    Worker(String name,
           PriorityBlockingQueue<ScheduledTask> queue,
           TaskRegistry registry,
           AtomicBoolean shutdown) {
        super(name);
        this.queue = queue;
        this.registry = registry;
        this.shutdown = shutdown;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (!shutdown.get()) {
                ScheduledTask st = queue.take(); // take head
                long now = System.currentTimeMillis();
                long wait = st.dueTimeMillis - now;
                if (wait > 0) {
                    // Not yet due: put back and sleep briefly to avoid busy spins.
                    queue.offer(st);
                    long sleep = Math.min(wait, 50);
                    Thread.sleep(sleep);
                    continue;
                }
                execute(st);
            }
        } catch (InterruptedException ie) {
            // graceful exit
            log.info("{} interrupted, exiting.", getName());
        }
    }

    private void execute(ScheduledTask st) {
        registry.onStarted(st.id);
        log.info("Starting task {} ({}) prio={}", st.id, st.name, st.priority);
        try {
            st.task.run();
            registry.onSucceeded(st.id);
            log.info("Completed task {} ({})", st.id, st.name);
        } catch (Throwable t) {
            registry.onFailed(st.id, t);
            log.error("Task {} ({}) failed: {}", st.id, st.name, t.toString());
        }
    }
}
