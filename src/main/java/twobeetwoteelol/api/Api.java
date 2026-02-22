package twobeetwoteelol.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import twobeetwoteelol.api.records.*;
import twobeetwoteelol.model.SignPayload;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public final class Api {
    private final HttpClient httpClient;
    private String jwtToken;
    private long jwtExpires;

    public Api(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private static JsonObject buildSignsBody(List<SignPayload> signs) {
        JsonArray signsArray = new JsonArray();

        for (SignPayload sign : signs) {
            JsonObject signJson = new JsonObject();
            signJson.addProperty("dimension", sign.dimension());
            signJson.addProperty("x", sign.x());
            signJson.addProperty("y", sign.y());
            signJson.addProperty("z", sign.z());

            JsonArray text = new JsonArray();
            for (String line : sign.frontLines()) {
                text.add(line);
            }

            signJson.add("text", text);
            signsArray.add(signJson);
        }

        JsonObject body = new JsonObject();
        body.add("signs", signsArray);
        return body;
    }

    private static JsonObject parseObject(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }

        try {
            JsonElement element = JsonParser.parseString(rawJson);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String getString(JsonObject json, String key) {
        JsonElement value = json == null ? null : json.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private static long getLong(JsonObject json, String key, long fallback) {
        JsonElement value = json == null ? null : json.get(key);
        if (value == null || !value.isJsonPrimitive()) return fallback;

        try {
            return value.getAsLong();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String statusMessage(HttpResponse<String> response, String fallback) {
        String error = getString(parseObject(response.body()), "error");
        if (!isBlank(error)) {
            return error;
        }

        return fallback + " (" + response.statusCode() + ").";
    }

    private static JsonObject actionBody(String action) {
        JsonObject body = new JsonObject();
        body.addProperty("action", action);
        return body;
    }

    public synchronized void resetSession() {
        jwtToken = null;
        jwtExpires = 0L;
    }

    public synchronized UploadResult uploadSigns(
        String baseUrl,
        String accessToken,
        String username,
        String uuid,
        List<SignPayload> signs,
        Runnable onAuthSuccess
    ) {
        String authError = ensureJwt(baseUrl, accessToken, username, uuid, onAuthSuccess);
        if (authError != null) {
            return UploadResult.failure(authError);
        }

        UploadResult result = postSigns(baseUrl, signs, jwtToken);
        if (!result.unauthorized()) {
            return result;
        }

        resetSession();
        authError = ensureJwt(baseUrl, accessToken, username, uuid, onAuthSuccess);
        if (authError != null) {
            return UploadResult.failure(authError);
        }

        return postSigns(baseUrl, signs, jwtToken);
    }

    public synchronized DashboardLinkResult createDashboardLink(
        String baseUrl,
        String accessToken,
        String username,
        String uuid
    ) {
        ApiResult<ApiChallengeData> challengeResult = requestChallenge(baseUrl);
        if (challengeResult.failed()) {
            return DashboardLinkResult.fail(challengeResult.error());
        }

        ApiChallengeData challenge = challengeResult.value();
        ApiResult<HttpResponse<String>> joinResult = joinSessionServer(accessToken, uuid, challenge.serverId());
        if (joinResult.failed()) {
            return DashboardLinkResult.fail(joinResult.error());
        }

        HttpResponse<String> joinResponse = joinResult.value();
        if (joinResponse.statusCode() / 100 != 2) {
            return DashboardLinkResult.fail("Minecraft join failed " + joinResponse.statusCode());
        }

        return requestDashboardCode(baseUrl, challenge.challengeToken(), username);
    }

    private String ensureJwt(
        String baseUrl,
        String accessToken,
        String username,
        String uuid,
        Runnable onAuthSuccess
    ) {
        long now = System.currentTimeMillis();
        if (jwtToken != null && now + 10_000L < jwtExpires) {
            return null;
        }

        ApiResult<ApiChallengeData> challengeResult = requestChallenge(baseUrl);
        if (challengeResult.failed()) {
            return challengeResult.error();
        }

        ApiChallengeData challenge = challengeResult.value();
        ApiResult<HttpResponse<String>> joinResult = joinSessionServer(accessToken, uuid, challenge.serverId());
        if (joinResult.failed()) {
            return joinResult.error();
        }

        HttpResponse<String> joinResponse = joinResult.value();
        if (joinResponse.statusCode() / 100 != 2) {
            return "Minecraft session join failed (" + joinResponse.statusCode() + ").";
        }

        ApiResult<ApiJwtData> completeResult = completeChallenge(baseUrl, challenge.challengeToken(), username);
        if (completeResult.failed()) {
            return completeResult.error();
        }

        ApiJwtData jwt = completeResult.value();
        jwtToken = jwt.token();
        jwtExpires = System.currentTimeMillis() + Math.max(60L, jwt.expiresInSeconds()) * 1000L;

        if (onAuthSuccess != null) {
            onAuthSuccess.run();
        }

        return null;
    }

    private ApiResult<JsonObject> authAction(String baseUrl, JsonObject body, String failureLabel) {
        ApiResult<HttpResponse<String>> sendResult = postJson(baseUrl + "/api/internal/auth", body, null);
        if (sendResult.failed()) return ApiResult.fail(sendResult.error());

        HttpResponse<String> response = sendResult.value();
        if (response.statusCode() / 100 != 2) return ApiResult.fail(statusMessage(response, failureLabel));

        JsonObject payload = parseObject(response.body());
        return payload == null ? ApiResult.fail("Server response was invalid.") : ApiResult.ok(payload);
    }

    private ApiResult<ApiChallengeData> requestChallenge(String baseUrl) {
        ApiResult<JsonObject> actionResult = authAction(baseUrl, actionBody("challenge"), "Challenge request failed");
        if (actionResult.failed()) return ApiResult.fail(actionResult.error());

        JsonObject payload = actionResult.value();
        String serverId = getString(payload, "serverId");
        String challengeToken = getString(payload, "challengeToken");
        if (isBlank(serverId) || isBlank(challengeToken)) {
            return ApiResult.fail("Challenge response missing fields.");
        }

        return ApiResult.ok(new ApiChallengeData(serverId, challengeToken));
    }

    private ApiResult<ApiJwtData> completeChallenge(String baseUrl, String challengeToken, String username) {
        JsonObject body = actionBody("complete");
        body.addProperty("challengeToken", challengeToken);
        body.addProperty("username", username);

        ApiResult<JsonObject> actionResult = authAction(baseUrl, body, "Authentication failed");
        if (actionResult.failed()) return ApiResult.fail(actionResult.error());

        JsonObject payload = actionResult.value();
        String token = getString(payload, "token");
        if (isBlank(token)) {
            return ApiResult.fail("Authentication token missing.");
        }

        long expiresInSeconds = getLong(payload, "expiresInSeconds", 60L * 60L * 24L);
        return ApiResult.ok(new ApiJwtData(token, expiresInSeconds));
    }

    private DashboardLinkResult requestDashboardCode(String baseUrl, String challengeToken, String username) {
        JsonObject body = actionBody("dashboard_code");
        body.addProperty("challengeToken", challengeToken);
        body.addProperty("username", username);

        ApiResult<JsonObject> actionResult = authAction(
            baseUrl,
            body,
            "Dashboard link request failed"
        );
        if (actionResult.failed()) return DashboardLinkResult.fail(actionResult.error());

        JsonObject payload = actionResult.value();
        String dashboardUrl = getString(payload, "dashboardUrl");
        if (isBlank(dashboardUrl)) {
            dashboardUrl = resolveDashboardUrl(baseUrl, getString(payload, "path"));
        }

        if (isBlank(dashboardUrl)) {
            return DashboardLinkResult.fail("Dashboard link missing.");
        }

        long expiresInSeconds = getLong(payload, "expiresInSeconds", 300L);
        return DashboardLinkResult.ok(dashboardUrl, expiresInSeconds);
    }

    private String resolveDashboardUrl(String baseUrl, String path) {
        if (isBlank(path)) {
            return null;
        }

        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        return path.startsWith("/") ? baseUrl + path : baseUrl + "/" + path;
    }

    private ApiResult<HttpResponse<String>> joinSessionServer(String accessToken, String uuid, String serverId) {
        JsonObject body = new JsonObject();
        body.addProperty("accessToken", accessToken);
        body.addProperty("selectedProfile", uuid);
        body.addProperty("serverId", serverId);

        return postJson("https://sessionserver.mojang.com/session/minecraft/join", body, null);
    }

    private UploadResult postSigns(String baseUrl, List<SignPayload> signs, String bearerToken) {
        ApiResult<HttpResponse<String>> sendResult = postJson(baseUrl + "/api/internal/signs", buildSignsBody(signs), bearerToken);
        if (sendResult.failed()) {
            return UploadResult.failure(sendResult.error());
        }

        HttpResponse<String> response = sendResult.value();
        if (response.statusCode() == 401) {
            return UploadResult.unauthorized("Bearer token blocked");
        }

        if (response.statusCode() / 100 != 2) {
            return UploadResult.failure(statusMessage(response, "Failed upload"));
        }

        JsonObject payload = parseObject(response.body());
        int stored = (int) getLong(payload, "stored", signs.size());
        return UploadResult.success(stored);
    }

    private ApiResult<HttpResponse<String>> postJson(String url, JsonObject body, String bearerToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");

        if (!isBlank(bearerToken)) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }

        HttpRequest request = builder
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
            .build();

        return send(request);
    }

    private ApiResult<HttpResponse<String>> send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            return ApiResult.ok(response);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return ApiResult.fail("Request interrupted.");
        } catch (IOException error) {
            String message = error.getMessage();
            return ApiResult.fail("Network failed" + message);
        }
    }
}
