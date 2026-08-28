package blackdemise.cp.ai;

// Wraps any failure from the AI provider (timeout, HTTP error, malformed response).
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
