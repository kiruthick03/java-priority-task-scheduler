package com.example.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central scheduler: accepts tasks with priority and optional delay; executes with worker pool.
 */
public final class TaskScheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

    private final PriorityBlockingQueue<ScheduledTask> queue = new PriorityBlockingQueue<>();
    private final List<Worker> workers = new ArrayList<>();
    private final TaskRegistry registry = new TaskRegistry();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final int workerCount;
    private final Integer httpPort; // nullable
    private MonitorServer monitorServer; // nullable

    private TaskScheduler(int workerCount, Integer httpPort) {
        this.workerCount = workerCount;
        this.httpPort = httpPort;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void start() {
        if (!started.compareAndSet(false, true)) return;
        log.info("Starting scheduler with {} worker(s)", workerCount);
        for (int i = 0; i < workerCount; i++) {
            Worker w = new Worker("worker-" + (i + 1), queue, registry, shutdown);
            w.start();
            workers.add(w);
        }
        if (httpPort != null) {
            monitorServer = new MonitorServer(httpPort, registry, queue);
            monitorServer.start();
        }
    }

    public TaskId submit(String name, Priority priority, Task task, long delayMillis) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(priority);
        Objects.requireNonNull(task);
        long due = System.currentTimeMillis() + Math.max(0, delayMillis);
        TaskId id = new TaskId();
        ScheduledTask st = new ScheduledTask(id, name, priority, task, due);
        registry.onQueued(id, name, priority);
        queue.offer(st);
        log.info("Submitted task {} ({}) prio={} delay={}ms due={}", id, name, priority, delayMillis, due);
        return id;
    }

    public TaskId submit(String name, Priority priority, Task task) {
        return submit(name, priority, task, 0);
    }

    public TaskRegistry registry() {
        return registry;
    }

    public void shutdownGracefully() {
        shutdown.set(true);
        for (Worker w : workers) {
            w.interrupt();
        }
        if (monitorServer != null) {
            monitorServer.stop();
        }
        log.info("Scheduler shutdown requested. Waiting for workers to finish...");
    }

    @Override
    public void close() {
        shutdownGracefully();
    }

    public static final class Builder {
        private int workerCount = Runtime.getRuntime().availableProcessors();
        private Integer httpPort = null;

        public Builder workerCount(int workerCount) {
            if (workerCount <= 0) throw new IllegalArgumentException("workerCount must be > 0");
            this.workerCount = workerCount;
            return this;
        }

        public Builder startHttpMonitor(int port) {
            this.httpPort = port;
            return this;
        }

        public TaskScheduler build() {
            return new TaskScheduler(workerCount, httpPort);
        }
    }
}
