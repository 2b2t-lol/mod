package twobeetwoteelol.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StringListSetting;
import net.minecraft.util.math.BlockPos;
import twobeetwoteelol.utils.StringyStringz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ExcludedLocationsSetting extends StringListSetting {
    private static final CharFilter SIGNED_INT_FILTER =
        (text, c) -> Character.isDigit(c) || (c == '-' && !text.contains("-"));
    private static final CharFilter UNSIGNED_INT_FILTER = (text, c) -> Character.isDigit(c);

    private final int defaultRange;

    public ExcludedLocationsSetting(
        String name,
        String description,
        List<String> defaultValue,
        Consumer<List<String>> onChanged,
        Consumer<Setting<List<String>>> onModuleActivated,
        IVisible visible,
        Class<? extends WTextBox.Renderer> renderer,
        CharFilter filter,
        int defaultRange
    ) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible, renderer, filter);
        this.defaultRange = Math.max(0, defaultRange);
    }

    public static void fillTable(GuiTheme theme, WTable table, ExcludedLocationsSetting setting) {
        table.clear();

        ArrayList<String> entries = new ArrayList<>(setting.get());

        for (int i = 0; i < setting.get().size(); i++) {
            int index = i;
            StringyStringz.ExclusionEntryText locationEntry =
                StringyStringz.parseExclusionEntry(setting.get().get(i), setting.defaultRange);

            WHorizontalList row = table.add(theme.horizontalList()).expandX().widget();
            row.spacing = 2;

            row.add(theme.label("X"));
            WTextBox xTextBox = row.add(
                theme.textBox(locationEntry.x(), "x", SIGNED_INT_FILTER, setting.renderer)
            ).minWidth(64).widget();

            row.add(theme.label("Z"));
            WTextBox zTextBox = row.add(
                theme.textBox(locationEntry.z(), "z", SIGNED_INT_FILTER, setting.renderer)
            ).minWidth(64).widget();

            row.add(theme.label("Range"));
            WTextBox rangeTextBox = row.add(
                theme.textBox(locationEntry.range(), "range", UNSIGNED_INT_FILTER, setting.renderer)
            ).minWidth(64).widget();

            Runnable persist = () -> entries.set(
                index,
                StringyStringz.formatExclusionEntry(
                    xTextBox.get(),
                    zTextBox.get(),
                    rangeTextBox.get()
                )
            );

            xTextBox.action = persist;
            zTextBox.action = persist;
            rangeTextBox.action = persist;

            Runnable setOnUnfocused = () -> {
                persist.run();
                setting.set(entries);
            };

            xTextBox.actionOnUnfocused = setOnUnfocused;
            zTextBox.actionOnUnfocused = setOnUnfocused;
            rangeTextBox.actionOnUnfocused = setOnUnfocused;

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                entries.remove(index);
                setting.set(entries);
                fillTable(theme, table, setting);
            };

            table.row();
        }

        if (!setting.get().isEmpty()) {
            table.add(theme.horizontalSeparator()).expandX();
            table.row();
        }

        WButton add = table.add(theme.button("Add")).expandX().widget();
        add.action = () -> {
            entries.add(defaultEntry(setting.defaultRange));
            setting.set(entries);
            fillTable(theme, table, setting);
        };

        WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            fillTable(theme, table, setting);
        };
    }

    private static String defaultEntry(int defaultRange) {
        int x = 0;
        int z = 0;

        if (mc.player != null) {
            BlockPos playerPos = mc.player.getBlockPos();
            x = playerPos.getX();
            z = playerPos.getZ();
        }

        return StringyStringz.formatExclusionEntry(
            Integer.toString(x),
            Integer.toString(z),
            Integer.toString(defaultRange)
        );
    }

    public static class Builder extends SettingBuilder<Builder, List<String>, ExcludedLocationsSetting> {
        private int defaultRange = 64;

        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(String... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        public Builder defaultRange(int defaultRange) {
            this.defaultRange = Math.max(0, defaultRange);
            return this;
        }

        @Override
        public ExcludedLocationsSetting build() {
            return new ExcludedLocationsSetting(
                name,
                description,
                defaultValue,
                onChanged,
                onModuleActivated,
                visible,
                null,
                null,
                defaultRange
            );
        }
    }
}
