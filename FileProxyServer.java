import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileProxyServer {
    private final int port;
    private final String targetServerUrl;
    private final int maxUsers;
    private final Map<String, Long> cacheExpirationTimes;

    public FileProxyServer(int port, String targetServerUrl, int maxUsers, Map<String, Long> cacheExpirationTimes) {
        this.port = port;
        this.targetServerUrl = targetServerUrl;
        this.maxUsers = maxUsers;
        this.cacheExpirationTimes = cacheExpirationTimes;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        System.out.println("Proxy server running on http://localhost:" + port);

        server.createContext("/", new ProxyHandler(targetServerUrl, cacheExpirationTimes));
        server.createContext("/list-cache", new CacheListHandler());
        server.createContext("/clear-cache", new CacheClearHandler());
        server.createContext("/delete-cache", new CacheDeleteHandler());

        ExecutorService threadPool = Executors.newFixedThreadPool(maxUsers);
        server.setExecutor(threadPool);

        server.start();

        System.out.println("Server is running with a thread pool of " + maxUsers + " threads.");
    }

    static class CacheListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder response = new StringBuilder("Cached URIs:\n");
            ProxyHandler.cache.forEach((key, value) -> response.append(key).append("\n"));
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.toString().getBytes());
            }
        }
    }

    static class CacheClearHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ProxyHandler.cache.clear();
            String response = "Cache cleared.";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    static class CacheDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod()) &&
                !"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String response = "Only DELETE or GET methods are allowed on this endpoint.";
                exchange.sendResponseHeaders(405, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String requestUri = exchange.getRequestURI().getPath().replace("/delete-cache/", "").trim();
            requestUri = requestUri.replaceAll("/+$", ""); // Remove trailing slashes

            System.out.println("Attempting to delete cache key: '" + requestUri + "'");

            if (requestUri.isEmpty()) {
                String response = "No cache key provided for deletion.";
                exchange.sendResponseHeaders(400, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            if (ProxyHandler.cache.containsKey(requestUri)) {
                ProxyHandler.cache.remove(requestUri);
                String response = "Cache entry for '" + requestUri + "' has been deleted.";
                System.out.println("Cache key deleted: '" + requestUri + "'");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                String response = "No cache entry found for '" + requestUri + "'.";
                System.out.println("Cache key not found: '" + requestUri + "'");
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}



