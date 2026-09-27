package dev.stardust.modules;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Files;
import com.google.gson.Gson;
import dev.stardust.Stardust;
import dev.stardust.util.LogUtil;
import dev.stardust.util.MsgUtil;
import com.google.gson.GsonBuilder;
import dev.stardust.util.StardustUtil;
import java.nio.file.StandardOpenOption;
import net.fabricmc.loader.api.FabricLoader;
import dev.stardust.gui.screens.SolitaireScreen;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.Setting;
import dev.stardust.gui.widgets.solitaire.model.DrawMode;
import dev.stardust.gui.widgets.solitaire.model.SaveState;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import dev.stardust.gui.widgets.solitaire.model.ColorSchemes;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     See also: SolitaireScreen.java && dev/stardust/gui/widgets/solitaire/*
 **/
public class Solitaire extends Module {
    public Solitaire() {
        super(Stardust.CATEGORY, "Solitaire", "What more could you ask for?");
        runInMainMenu = true;
    }

    private static final String GAME_FOLDER = "meteor-client/minigames/solitaire";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public final Setting<DrawMode> drawMode = settings.getDefaultGroup().add(
        new EnumSetting.Builder<DrawMode>()
            .name("draw-mode")
            .defaultValue(DrawMode.Draw_One)
            .build()
    );
    public final Setting<Boolean> shouldSave = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("save-games")
            .description("Saves your game state when closing the Minesweeper screen.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> renderMap = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("force-render-minimap")
            .description("Continues rendering the Xaeros minimap while the Minesweeper screen is open.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> sounds = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("game-sounds")
            .description("Plays game sounds.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Double> soundVolume = settings.getDefaultGroup().add(
        new DoubleSetting.Builder()
            .name("sounds-volume")
            .min(0.1).max(4.0)
            .defaultValue(0.5)
            .visible(sounds::get)
            .build()
    );
    public final Setting<ColorSchemes> colorScheme = settings.getDefaultGroup().add(
        new EnumSetting.Builder<ColorSchemes>()
            .name("color-scheme")
            .defaultValue(ColorSchemes.Themed)
            .build()
    );

    public SaveState saveData = null;

    public void saveGame(SaveState data) {
        saveData = data;
        Path saveFolder = FabricLoader.getInstance().getGameDir().resolve(GAME_FOLDER);

        //noinspection ResultOfMethodCallIgnored
        saveFolder.toFile().mkdirs();
        Path save = saveFolder.resolve("save.json");
        if (!Files.exists(save)) {
            if (!StardustUtil.checkOrCreateFile(mc, GAME_FOLDER + "/save.json")) {
                MsgUtil.sendModuleMsg("Failed to create save file§c..!", this.name);
            }
        }
        try (Writer writer = Files.newBufferedWriter(save, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(data, writer);
        } catch (Exception err) {
            LogUtil.error(err.toString(), this.name);
        }
    }

    public void clearSave() {
        saveGame(null);
    }

    @Override
    public void onActivate() {
        Path saveFolder = FabricLoader.getInstance().getGameDir().resolve(GAME_FOLDER);

        //noinspection ResultOfMethodCallIgnored
        saveFolder.toFile().mkdirs();
        Path save = saveFolder.resolve("save.json");
        if (!Files.exists(save)) {
            if (!StardustUtil.checkOrCreateFile(mc, GAME_FOLDER + "/save.json")) {
                MsgUtil.sendModuleMsg("Failed to create save file§c..!", this.name);
            }
        }

        SaveState data = null;
        try (Reader reader = Files.newBufferedReader(save)) {
            data = GSON.fromJson(reader, SaveState.class);
        } catch (Exception err) {
            LogUtil.error(err.toString(), this.name);
        }

        if (data != null) saveData = data;

        try {
            mc.setScreen(new SolitaireScreen(this, GuiThemes.get(), "Solitaire"));
        } catch (Exception err) {
            LogUtil.error("Failed to open the Solitaire screen..!", this.name);
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.screen instanceof SolitaireScreen) {
            try {
                mc.setScreen(null);
            } catch (Exception err) {
                LogUtil.error("Failed to close the Solitaire screen..!", this.name);
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList list = theme.horizontalList();
        WButton clearSave = list.add(theme.button("Clear Save")).widget();

        clearSave.action = this::clearSave;

        return list;
    }
}
