import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class CacheClient {
    private static final String PROXY_SERVER_URL = "http://172.50.104.198:8080";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Proxy Cache Management ===");
            System.out.println("1. List Cache Entries");
            System.out.println("2. Delete a Cache Entry");
            System.out.println("3. Clear All Cache");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    listCache();
                    break;
                case 2:
                    System.out.print("Enter the cache key (URI) to delete: ");
                    String cacheKey = scanner.nextLine().trim();
                    if (!cacheKey.isEmpty()) {
                        deleteCache(cacheKey);
                    } else {
                        System.out.println("Cache key cannot be empty.");
                    }
                    break;
                case 3:
                    clearCache();
                    break;
                case 4:
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    private static void listCache() {
        try {
            String url = PROXY_SERVER_URL + "/list-cache";
            HttpURLConnection connection = createConnection(url, "GET");

            System.out.println("Fetching cache entries...");
            printResponse(connection);

        } catch (Exception e) {
            System.err.println("Error fetching cache entries: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void deleteCache(String cacheKey) {
        try {
            // Ensure no duplicate slashes in the URL
            String normalizedKey = cacheKey.startsWith("/") ? cacheKey.substring(1) : cacheKey;
            String url = PROXY_SERVER_URL + "/delete-cache//" + normalizedKey;
    
            HttpURLConnection connection = createConnection(url, "DELETE");
    
            System.out.println("Deleting cache entry: " + cacheKey);
            printResponse(connection);
    
        } catch (Exception e) {
            System.err.println("Error deleting cache entry: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void clearCache() {
        try {
            String url = PROXY_SERVER_URL + "/clear-cache";
            HttpURLConnection connection = createConnection(url, "DELETE");

            System.out.println("Clearing all cache...");
            printResponse(connection);

        } catch (Exception e) {
            System.err.println("Error clearing cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static HttpURLConnection createConnection(String url, String method) throws Exception {
        URL endpoint = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
        connection.setRequestMethod(method);
        connection.setDoOutput(true);
        return connection;
    }

    private static void printResponse(HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine).append("\n");
            }

            System.out.println("Response:\n" + response);
            System.out.println("HTTP Status Code: " + connection.getResponseCode());

        } catch (Exception e) {
            System.err.println("Failed to read response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
