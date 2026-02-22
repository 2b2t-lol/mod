package twobeetwoteelol.api.records;

public record UploadResult(boolean success, boolean unauthorized, int stored, String errorMessage) {
    public static UploadResult success(int stored) {
        return new UploadResult(true, false, stored, null);
    }

    public static UploadResult unauthorized(String errorMessage) {
        return new UploadResult(false, true, 0, errorMessage);
    }

    public static UploadResult failure(String errorMessage) {
        return new UploadResult(false, false, 0, errorMessage);
    }
}
