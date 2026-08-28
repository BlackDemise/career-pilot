package blackdemise.cp.common;

public record ApiResponse(int timestamp, int statusCode, String message, Object result) {

    public static ApiResponse success(int statusCode, String message, Object result) {
        return new ApiResponse((int) (System.currentTimeMillis() / 1000), statusCode, message, result);
    }

    public static ApiResponse error(int statusCode, String message) {
        return success(statusCode, message, null);
    }

    public static ApiResponse validationError(String message, Object fieldErrors) {
        return success(400, message, fieldErrors);
    }
}