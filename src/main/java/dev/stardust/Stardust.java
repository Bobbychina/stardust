package dev.stardust;

import org.slf4j.Logger;
import dev.stardust.modules.*;
import dev.stardust.commands.*;
import dev.stardust.gui.themes.*;
import dev.stardust.util.MsgUtil;
import dev.stardust.util.TimeUtil;
import dev.stardust.hud.ConwayHud;
import com.mojang.logging.LogUtils;
import dev.stardust.util.StardustUtil;
import dev.stardust.config.StardustConfig;
import dev.stardust.managers.PacketManager;
import net.fabricmc.loader.api.FabricLoader;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import net.fabricmc.loader.api.metadata.CustomValue;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Stardust extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final HudGroup HUD_GROUP = new HudGroup("Stardust");
    public static final Category CATEGORY = new Category("Stardust", StardustUtil::chooseMenuIcon);

    private PacketManager packetManager;
    public static TimeUtil TIME;

    @Override
    public void onInitialize() {
        Commands.add(new Life());
        // [共存去重] Commands.add(new Loadout());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Commands.add(new Panorama());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Commands.add(new Stats2b2t());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Commands.add(new Playtime2b2t());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Commands.add(new LastSeen2b2t());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Commands.add(new FirstSeen2b2t());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）

        // [共存去重] Modules.get().add(new Honker());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new WaxAura());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new AntiToS());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new Updraft());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new Grinder());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new RoadTrip());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new Loadouts());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        Modules.get().add(new VaultESP());
        Modules.get().add(new AdBlocker());
        // [共存去重] Modules.get().add(new AutoDoors());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new AutoMason());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        Modules.get().add(new AutoSmith());
        // [共存去重] Modules.get().add(new BookTools());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new ChatSigns());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new RapidFire());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        Modules.get().add(new Solitaire());
        // [共存去重] Modules.get().add(new RocketMan());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new RocketJump());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new BannerData());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        Modules.get().add(new ChatPrefix());
        // [共存去重] Modules.get().add(new PagePirate());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        Modules.get().add(new Meteorites());
        Modules.get().add(new Minesweeper());
        // [共存去重] Modules.get().add(new Archaeology());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new MusicTweaks());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        Modules.get().add(new TreasureESP());
        // [共存去重] Modules.get().add(new LoreLocator());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new AxolotlTools());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new StashBrander());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new SignatureSign());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new SignHistorian());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new AutoDyeShulkers());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）
        // [共存去重] Modules.get().add(new AutoDrawDistance());   // 与另一 mod 同名，功能由保留侧提供（见 docs/PORT-NOTES.md）

        Hud.get().register(ConwayHud.INFO);

        GuiThemes.add(DarkTheme.INSTANCE);
        GuiThemes.add(SnowyTheme.INSTANCE);
        GuiThemes.add(LambdaTheme.INSTANCE);
        GuiThemes.add(StardustTheme.INSTANCE);
        GuiThemes.add(MidnightTheme.INSTANCE);
        GuiThemes.add(PhosphorTheme.INSTANCE);
        GuiThemes.add(MonochromeTheme.INSTANCE);

        TIME = new TimeUtil();
        packetManager = new PacketManager();

        StardustConfig.initialize();
        MsgUtil.initModulePrefixes();
        LOG.info("<✨> Stardust initialized.");

        if (!StardustUtil.XAERO_AVAILABLE) {
            LOG.warn("[Stardust] Skipping Xaero Map integration as one or both of xaero world map & xaero minimap are missing..!");
        }
    }


    @Override
    public String getPackage() {
        return "dev.stardust";
    }
    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Stardust.CATEGORY);
    }
    @Override
    public String getWebsite() { return "https://github.com/0xTas/stardust"; }
    @Override
    public GithubRepo getRepo() { return new GithubRepo("0xTas", "Stardust", "main", null); }
    @Override
    public String getCommit() {
        CustomValue commit = FabricLoader.getInstance()
            .getModContainer("stardust")
            .orElseThrow()
            .getMetadata()
            .getCustomValue(MeteorClient.MOD_ID + ":commit");

        return commit == null ? null : commit.getAsString();
    }
}
