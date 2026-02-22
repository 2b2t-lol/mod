package twobeetwoteelol.api.records;

public record ApiResult<T>(T value, String error) {
    public static <T> ApiResult<T> ok(T value) {
        return new ApiResult<>(value, null);
    }

    public static <T> ApiResult<T> fail(String error) {
        return new ApiResult<>(null, error);
    }

    public boolean failed() {
        return error != null;
    }
}
