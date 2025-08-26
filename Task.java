package com.example.scheduler;

/**
 * A user-defined unit of work.
 * Wraps a Runnable and adds metadata hooks if needed in future.
 */
@FunctionalInterface
public interface Task extends Runnable {
    // Extendable for richer APIs later (e.g., getName(), cancel(), etc.)
}
