import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProxyHandler implements HttpHandler {
    public static final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();
    private final String targetBaseUrl;
    private final Map<String, Long> cacheExpirationTimes;
    private final ScheduledExecutorService cacheCleaner;

    public ProxyHandler(String targetBaseUrl, Map<String, Long> cacheExpirationTimes) {
        this.targetBaseUrl = targetBaseUrl;
        this.cacheExpirationTimes = cacheExpirationTimes;

        this.cacheCleaner = Executors.newSingleThreadScheduledExecutor();
        this.cacheCleaner.scheduleAtFixedRate(this::cleanExpiredCache, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void handle(HttpExchange exchange) {
        String requestUri = exchange.getRequestURI().toString();

        try {
            if (cache.containsKey(requestUri)) {
                CachedResponse cached = cache.get(requestUri);
                long expirationTime = cacheExpirationTimes.getOrDefault(cached.contentType, 60000L);
                if (System.currentTimeMillis() - cached.timestamp < expirationTime) {
                    System.out.println("Cache hit: " + requestUri);
                    sendResponse(exchange, cached.body, cached.statusCode, cached.contentType);
                    return;
                } else {
                    cache.remove(requestUri);
                }
            }

            System.out.println("Cache miss: " + requestUri);
            URL targetUrl = new URL(targetBaseUrl + requestUri);
            HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setRequestMethod(exchange.getRequestMethod());

            int responseCode = connection.getResponseCode();
            byte[] responseBody = connection.getInputStream().readAllBytes();
            String contentType = connection.getHeaderField("Content-Type");

            // Cache the response
            cache.put(requestUri, new CachedResponse(responseBody, responseCode, System.currentTimeMillis(), contentType));
            sendResponse(exchange, responseBody, responseCode, contentType);

        } catch (Exception e) {
            sendError(exchange, "Error: " + e.getMessage());
        }
    }

    private void sendResponse(HttpExchange exchange, byte[] body, int statusCode, String contentType) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private void sendError(HttpExchange exchange, String message) {
        try {
            exchange.sendResponseHeaders(500, message.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(message.getBytes());
            }
        } catch (IOException ignored) {}
    }

    private void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> {
            String contentType = entry.getValue().contentType;
            long expirationTime = cacheExpirationTimes.getOrDefault(contentType, 60000L);
            boolean isExpired = now - entry.getValue().timestamp >= expirationTime;
            if (isExpired) {
                System.out.println("Removed expired cache entry: " + entry.getKey());
            }
            return isExpired;
        });
    }

    public void shutdown() {
        cacheCleaner.shutdown();
        try {
            if (!cacheCleaner.awaitTermination(5, TimeUnit.SECONDS)) {
                cacheCleaner.shutdownNow();
            }
        } catch (InterruptedException e) {
            cacheCleaner.shutdownNow();
        }
    }
}

