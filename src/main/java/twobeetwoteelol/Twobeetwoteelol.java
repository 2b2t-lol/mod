package twobeetwoteelol;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import twobeetwoteelol.commands.DashboardCmd;
import twobeetwoteelol.modules.SignUploader;
import twobeetwoteelol.settings.ExcludedLocationsSetting;

public class Twobeetwoteelol extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Loading 2b2t-lol");
        SettingsWidgetFactory.registerCustomFactory(ExcludedLocationsSetting.class, theme -> (table, setting) -> {
            WTable widgetTable = table.add(theme.table()).expandX().widget();
            ExcludedLocationsSetting.fillTable(theme, widgetTable, (ExcludedLocationsSetting) setting);
        });
        Modules.get().add(new SignUploader());
        Commands.add(new DashboardCmd());

        LOG.info("2b2t-lol loaded");
    }

    @Override
    public String getPackage() {
        return getClass().getPackageName();
    }
}
