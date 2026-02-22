package twobeetwoteelol.api.records;

public record DashboardLinkResult(
    boolean success,
    String dashboardUrl,
    long expiresInSeconds,
    String errorMessage
) {
    public static DashboardLinkResult ok(String dashboardUrl, long expiresInSeconds) {
        return new DashboardLinkResult(true, dashboardUrl, expiresInSeconds, null);
    }

    public static DashboardLinkResult fail(String errorMessage) {
        return new DashboardLinkResult(false, null, 0L, errorMessage);
    }
}
