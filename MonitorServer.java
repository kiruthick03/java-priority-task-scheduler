package com.example.scheduler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

/** Minimal HTTP server for /metrics (JSON) using JDK HttpServer. */
public final class MonitorServer {

    private final int port;
    private final TaskRegistry registry;
    private final PriorityBlockingQueue<ScheduledTask> queue;
    private HttpServer server;

    public MonitorServer(int port, TaskRegistry registry, PriorityBlockingQueue<ScheduledTask> queue) {
        this.port = port;
        this.registry = registry;
        this.queue = queue;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/metrics", new MetricsHandler());
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start monitor server on port " + port, e);
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private final class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Map<String, TaskRegistry.Entry> snap = registry.snapshot();
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append(""queueSize":").append(queue.size()).append(',');
            sb.append(""completed":").append(registry.completedCount()).append(',');
            sb.append(""failed":").append(registry.failedCount()).append(',');
            sb.append(""tasks":["); // recent (up to 50)
            var recent = registry.recent(50);
            for (int i = 0; i < recent.size(); i++) {
                var e = recent.get(i);
                sb.append("{")
                  .append(""id":"").append(e.id).append("",")
                  .append(""name":"").append(e.name).append("",")
                  .append(""priority":"").append(e.priority).append("",")
                  .append(""status":"").append(e.status).append("",");
                sb.append(""enqueuedAt":").append(e.enqueuedAt).append(",");
                sb.append(""startedAt":").append(e.startedAt == null ? "null" : e.startedAt).append(",");
                sb.append(""finishedAt":").append(e.finishedAt == null ? "null" : e.finishedAt).append(",");
                sb.append(""error":").append(e.error == null ? "null" : """ + e.error.replace(""","'") + """);
                sb.append("}");
                if (i < recent.size() - 1) sb.append(",");
            }
            sb.append("]");
            sb.append("}");
            byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }
}
