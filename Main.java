import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        int serverPort;
        String targetServerUrl;
        int maxUsers;
        Map<String, Long> cacheExpirationTimes = new HashMap<>();

        try (InputStream input = new FileInputStream("config.conf")) {
            Properties properties = new Properties();
            properties.load(input);

            serverPort = Integer.parseInt(properties.getProperty("server.port", "8080"));
            targetServerUrl = properties.getProperty("server.target.url", "http://127.0.0.1:80");
            maxUsers = Integer.parseInt(properties.getProperty("server.max.users", "10"));

            properties.stringPropertyNames().forEach(key -> {
                if (key.startsWith("cache.expiration.time.")) {
                    String contentType = key.substring("cache.expiration.time.".length());
                    cacheExpirationTimes.put(contentType, Long.parseLong(properties.getProperty(key)));
                }
            });

            System.out.println("Configurations Loaded:");
            System.out.println("Server Port: " + serverPort);
            System.out.println("Target Server URL: " + targetServerUrl);
            System.out.println("Maximum Users (Threads): " + maxUsers);
            System.out.println("Cache Expiration Times: " + cacheExpirationTimes);

        } catch (IOException ex) {
            System.out.println("Error loading configurations. Using defaults.");
            ex.printStackTrace();
            serverPort = 8080;
            targetServerUrl = "http://127.0.0.1:80";
            maxUsers = 10;
        }

        try {
            FileProxyServer server = new FileProxyServer(serverPort, targetServerUrl, maxUsers, cacheExpirationTimes);
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start the server.");
            e.printStackTrace();
        }
    }
}
