package com.example.scheduler;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Internal wrapper for queueing. Orders by dueTime then priority then sequence.
 */
final class ScheduledTask implements Comparable<ScheduledTask> {
    private static final AtomicLong SEQ = new AtomicLong(0);

    final TaskId id;
    final String name;
    final Priority priority;
    final Task task;
    final long dueTimeMillis; // epoch millis
    final long sequence; // to stabilize comparator

    ScheduledTask(TaskId id, String name, Priority priority, Task task, long dueTimeMillis) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.priority = Objects.requireNonNull(priority);
        this.task = Objects.requireNonNull(task);
        this.dueTimeMillis = dueTimeMillis;
        this.sequence = SEQ.getAndIncrement();
    }

    boolean isDue(long nowMillis) {
        return nowMillis >= dueTimeMillis;
    }

    @Override
    public int compareTo(ScheduledTask other) {
        int cmp = Long.compare(this.dueTimeMillis, other.dueTimeMillis);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(this.priority.rank(), other.priority.rank());
        if (cmp != 0) return cmp;
        return Long.compare(this.sequence, other.sequence);
    }

    @Override
    public String toString() {
        return "ScheduledTask{" +
                "id=" + id +
                ", name='" + name + ''' +
                ", priority=" + priority +
                ", due=" + Instant.ofEpochMilli(dueTimeMillis) +
                ", seq=" + sequence +
                '}';
    }
}
