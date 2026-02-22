package twobeetwoteelol.model;

import net.minecraft.client.session.Session;

import java.util.UUID;

public record MinecraftSessionData(String accessToken, String username, String uuid) {
    public static LoadResult from(Session session) {
        if (session == null) {
            return LoadResult.fail("No Minecraft session found.");
        }

        String accessToken = session.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            return LoadResult.fail("No Minecraft access token available.");
        }

        String username = session.getUsername();
        if (username == null || username.isBlank()) {
            return LoadResult.fail("No Minecraft username available.");
        }

        UUID profileUuid = session.getUuidOrNull();
        if (profileUuid == null) {
            return LoadResult.fail("No Minecraft profile UUID available.");
        }

        return LoadResult.ok(new MinecraftSessionData(
            accessToken,
            username,
            profileUuid.toString().replace("-", "")
        ));
    }

    public record LoadResult(MinecraftSessionData value, String errorMessage) {
        public static LoadResult ok(MinecraftSessionData value) {
            return new LoadResult(value, null);
        }

        public static LoadResult fail(String errorMessage) {
            return new LoadResult(null, errorMessage);
        }

        public boolean failed() {
            return value == null;
        }
    }
}
