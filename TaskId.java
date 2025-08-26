package com.example.scheduler;

import java.util.Objects;
import java.util.UUID;

/** Stable identifier for tasks. */
public final class TaskId {
    private final String id;

    public TaskId() {
        this.id = UUID.randomUUID().toString();
    }

    public TaskId(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public String value() {
        return id;
    }

    @Override
    public String toString() { return id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskId)) return false;
        TaskId taskId = (TaskId) o;
        return id.equals(taskId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
