package twobeetwoteelol.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.command.CommandSource;
import twobeetwoteelol.Settings;
import twobeetwoteelol.api.Api;
import twobeetwoteelol.api.records.DashboardLinkResult;
import twobeetwoteelol.model.MinecraftSessionData;
import twobeetwoteelol.utils.StringyStringz;

import java.net.http.HttpClient;
import java.time.Duration;

public class DashboardCmd extends Command {
    private final Api api = new Api(
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
    );

    public DashboardCmd() {
        super(
            "2b2tlol-dashboard",
            "Generates a one-time 2b2t-lol dashboard login link."
        );
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            requestDashboardLink();
            return SINGLE_SUCCESS;
        });
    }

    private void requestDashboardLink() {
        String baseUrl = StringyStringz.normalizeBaseUrl(Settings.defaultServerUrl);
        if (baseUrl == null) {
            error("Invalid default server URL configuration.");
            return;
        }

        MinecraftSessionData.LoadResult sessionResult = MinecraftSessionData.from(mc.getSession());
        if (sessionResult.failed()) {
            error(sessionResult.errorMessage());
            return;
        }

        MinecraftSessionData sessionData = sessionResult.value();
        info("Generating dashboard link...");

        MeteorExecutor.execute(() -> {
            DashboardLinkResult result = api.createDashboardLink(
                baseUrl,
                sessionData.accessToken(),
                sessionData.username(),
                sessionData.uuid()
            );

            mc.execute(() -> {
                if (!result.success()) {
                    error("Dashboard link request failed: %s", result.errorMessage());
                    return;
                }

                String dashboardUrl = result.dashboardUrl();
                mc.keyboard.setClipboard(dashboardUrl);
                info("Dashboard link copied to clipboard.");
                info("Open this link (valid %d seconds): %s", result.expiresInSeconds(), dashboardUrl);
            });
        });
    }
}
