package com.example.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/** Demo main: submits sample tasks with mixed priorities and delays. */
public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        TaskScheduler scheduler = TaskScheduler.builder()
                .workerCount(3)
                .startHttpMonitor(8080)
                .build();
        scheduler.start();

        Random rnd = new Random();
        for (int i = 1; i <= 10; i++) {
            int idx = i;
            Priority p = (i % 3 == 0) ? Priority.HIGH : (i % 3 == 1) ? Priority.MEDIUM : Priority.LOW;
            long delay = rnd.nextInt(2000); // 0..1999 ms
            scheduler.submit("DemoTask-" + idx, p, () -> {
                try {
                    Thread.sleep(200 + rnd.nextInt(400)); // simulate work
                    if (rnd.nextDouble() < 0.1) throw new RuntimeException("Random failure");
                    log.info("Work for task {} done.", idx);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, delay);
        }

        // Let demo run for a bit then shutdown.
        Thread.sleep(5_000);
        scheduler.shutdownGracefully();
        Thread.sleep(500); // allow logs to flush
    }
}
