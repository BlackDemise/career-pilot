package blackdemise.cp.common.exception;

import org.springframework.http.HttpStatus;

// Base type for domain exceptions that map directly to an HTTP status + error code (see
// GlobalExceptionHandler and api.instructions.md's error response shape).
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
