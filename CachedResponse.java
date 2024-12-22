public class CachedResponse {
    public byte[] body;
    public int statusCode;
    public long timestamp;
    public String contentType;

    public CachedResponse(byte[] body, int statusCode, long timestamp, String contentType) {
        this.body = body;
        this.statusCode = statusCode;
        this.timestamp = timestamp;
        this.contentType = contentType;
    }
}
