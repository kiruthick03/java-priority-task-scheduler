package com.example.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class SchedulerTests {

    private TaskScheduler scheduler;

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.shutdownGracefully();
        }
    }

    @Test
    void executesHigherPriorityFirstWhenDueTimeEqual() throws InterruptedException {
        scheduler = TaskScheduler.builder().workerCount(1).build();
        scheduler.start();
        List<String> order = new CopyOnWriteArrayList<>();

        scheduler.submit("LOW", Priority.LOW, () -> order.add("LOW"), 0);
        scheduler.submit("HIGH", Priority.HIGH, () -> order.add("HIGH"), 0);
        scheduler.submit("MEDIUM", Priority.MEDIUM, () -> order.add("MEDIUM"), 0);

        // wait a moment for execution
        TimeUnit.MILLISECONDS.sleep(500);

        Assertions.assertEquals(List.of("HIGH", "MEDIUM", "LOW"), order);
    }

    @Test
    void respectsDelayBeforeExecution() throws InterruptedException {
        scheduler = TaskScheduler.builder().workerCount(1).build();
        scheduler.start();
        List<Long> times = new CopyOnWriteArrayList<>();
        long start = System.currentTimeMillis();

        scheduler.submit("A", Priority.HIGH, () -> times.add(System.currentTimeMillis()), 700);
        scheduler.submit("B", Priority.HIGH, () -> times.add(System.currentTimeMillis()), 0);

        TimeUnit.SECONDS.sleep(1);

        Assertions.assertEquals(2, times.size());
        long aDelay = times.get(0) - start;
        long bDelay = times.get(1) - start;

        // First executed should be B (~0ms), then A (~700ms)
        Assertions.assertTrue(bDelay < 300, "Task B should run quickly");
        Assertions.assertTrue(aDelay >= 600, "Task A should be delayed");
    }

    @Test
    void registryTracksSuccessAndFailure() throws InterruptedException {
        scheduler = TaskScheduler.builder().workerCount(1).build();
        scheduler.start();

        scheduler.submit("OK", Priority.MEDIUM, () -> {}, 0);
        scheduler.submit("FAIL", Priority.MEDIUM, () -> { throw new RuntimeException("boom"); }, 0);

        TimeUnit.MILLISECONDS.sleep(800);

        Assertions.assertEquals(1, scheduler.registry().completedCount());
        Assertions.assertEquals(1, scheduler.registry().failedCount());
    }
}
