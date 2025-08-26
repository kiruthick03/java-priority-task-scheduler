package com.example.scheduler;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Tracks tasks for monitoring. */
public final class TaskRegistry {

    public static final class Entry {
        public final TaskId id;
        public final String name;
        public final Priority priority;
        public volatile TaskStatus status;
        public final long enqueuedAt;
        public volatile Long startedAt;
        public volatile Long finishedAt;
        public volatile String error;

        Entry(TaskId id, String name, Priority priority) {
            this.id = id;
            this.name = name;
            this.priority = priority;
            this.status = TaskStatus.QUEUED;
            this.enqueuedAt = System.currentTimeMillis();
        }
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final Deque<String> recentIds = new ArrayDeque<>();

    public void onQueued(TaskId id, String name, Priority prio) {
        Entry e = new Entry(id, name, prio);
        entries.put(id.value(), e);
        remember(id.value());
    }

    public void onStarted(TaskId id) {
        Entry e = entries.get(id.value());
        if (e != null) {
            e.status = TaskStatus.RUNNING;
            e.startedAt = System.currentTimeMillis();
        }
    }

    public void onSucceeded(TaskId id) {
        Entry e = entries.get(id.value());
        if (e != null) {
            e.status = TaskStatus.SUCCEEDED;
            e.finishedAt = System.currentTimeMillis();
        }
        completed.incrementAndGet();
    }

    public void onFailed(TaskId id, Throwable t) {
        Entry e = entries.get(id.value());
        if (e != null) {
            e.status = TaskStatus.FAILED;
            e.finishedAt = System.currentTimeMillis();
            e.error = t.toString();
        }
        failed.incrementAndGet();
    }

    public Map<String, Entry> snapshot() {
        return Map.copyOf(entries);
    }

    public long completedCount() { return completed.get(); }
    public long failedCount() { return failed.get(); }

    public List<Entry> recent(int limit) {
        List<Entry> out = new ArrayList<>();
        int i = 0;
        for (String id : recentIds) {
            if (i++ >= limit) break;
            Entry e = entries.get(id);
            if (e != null) out.add(e);
        }
        return out;
    }

    private void remember(String id) {
        synchronized (recentIds) {
            recentIds.addFirst(id);
            while (recentIds.size() > 1000) {
                recentIds.removeLast();
            }
        }
    }
}
