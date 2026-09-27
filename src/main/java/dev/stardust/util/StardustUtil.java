package dev.stardust.util;

import java.io.File;
import java.util.UUID;
import java.time.Instant;
import java.util.Optional;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.ClickEvent;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.Packet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.game.*;
import io.netty.util.internal.ThreadLocalRandom;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ResolvableProfile;
import meteordevelopment.meteorclient.utils.world.Dimension;
import dev.stardust.mixin.accessor.ClientConnectionAccessor;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.util.Crypt;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.mixin.ClientPlayNetworkHandlerAccessor;

/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
public class StardustUtil {
    public static final boolean XAERO_AVAILABLE = FabricLoader.getInstance().isModLoaded("xaeroworldmap")
        && FabricLoader.getInstance().isModLoaded("xaerominimap");

    public enum RainbowColor {
        Reds(new String[]{"§c", "§4"}),
        Yellows(new String[]{"§e", "§6"}),
        Greens(new String[]{"§a", "§2"}),
        Cyans(new String[]{"§b", "§3"}),
        Blues(new String[]{"§9", "§1"}),
        Purples(new String[]{"§d", "§5"});

        public final String[] labels;

        RainbowColor(String[] labels) { this.labels = labels; }

        public static RainbowColor getFirst() {
            return RainbowColor.values()[ThreadLocalRandom.current().nextInt(RainbowColor.values().length)];
        }

        public static RainbowColor getNext(RainbowColor previous) {
            return switch (previous) {
                case Reds -> Yellows;
                case Yellows -> Greens;
                case Greens -> Cyans;
                case Cyans -> Blues;
                case Blues -> Purples;
                case Purples -> Reds;
            };
        }
    }

    public enum TextColor {
        Black("§0"), White("§f"), Gray("§8"), Light_Gray("§7"),
        Dark_Green("§2"), Green("§a"), Dark_Aqua("§3"), Aqua("§b"),
        Dark_Blue("§1"), Blue("§9"), Dark_Red("§4"), Red("§c"),
        Dark_Purple("§5"), Purple("§d"), Gold("§6"), Yellow("§e"),
        Random("");

        public final String label;

        TextColor(String label) {
            this.label = label;
        }
    }

    public enum TextFormat {
        Plain(""), Italic("§o"), Bold("§l"),
        Underline("§n"), Strikethrough("§m"),
        Obfuscated("§k");

        public final String label;

        TextFormat(String label) {
            this.label = label;
        }
    }

    /** Random Color-Code */
    public static String rCC() {
        String color = "§7";
        TextColor[] colors = TextColor.values();

        // Omit gray, light_gray, and black from accent colors.
        while (color.equals("§0") || color.equals("§8") || color.equals("§7")) {
            int luckyIndex = ThreadLocalRandom.current().nextInt(colors.length);
            color = colors[luckyIndex].label;
        }

        return color;
    }

    public static ItemStack chooseMenuIcon() {
        int luckyIndex = ThreadLocalRandom.current().nextInt(menuIcons.length);

        return menuIcons[luckyIndex];
    }

    private static final ItemStack[] discIcons = {
        Items.MUSIC_DISC_5.getDefaultInstance(),
        Items.MUSIC_DISC_11.getDefaultInstance(),
        Items.MUSIC_DISC_13.getDefaultInstance(),
        Items.MUSIC_DISC_CAT.getDefaultInstance(),
        Items.MUSIC_DISC_FAR.getDefaultInstance(),
        Items.MUSIC_DISC_MALL.getDefaultInstance(),
        Items.MUSIC_DISC_STAL.getDefaultInstance(),
        Items.MUSIC_DISC_WARD.getDefaultInstance(),
        Items.MUSIC_DISC_WAIT.getDefaultInstance(),
        Items.MUSIC_DISC_CHIRP.getDefaultInstance(),
        Items.MUSIC_DISC_STRAD.getDefaultInstance(),
        Items.MUSIC_DISC_RELIC.getDefaultInstance(),
        Items.MUSIC_DISC_BLOCKS.getDefaultInstance(),
        Items.MUSIC_DISC_MELLOHI.getDefaultInstance(),
        Items.MUSIC_DISC_PIGSTEP.getDefaultInstance(),
        Items.MUSIC_DISC_CREATOR.getDefaultInstance(),
        Items.MUSIC_DISC_PRECIPICE.getDefaultInstance(),
        Items.MUSIC_DISC_OTHERSIDE.getDefaultInstance(),
        Items.MUSIC_DISC_CREATOR_MUSIC_BOX.getDefaultInstance(),
    };
    private static final ItemStack[] doorIcons = {
        Items.OAK_DOOR.getDefaultInstance(),
        Items.IRON_DOOR.getDefaultInstance(),
        Items.BIRCH_DOOR.getDefaultInstance(),
        Items.BAMBOO_DOOR.getDefaultInstance(),
        Items.CHERRY_DOOR.getDefaultInstance(),
        Items.JUNGLE_DOOR.getDefaultInstance(),
        Items.ACACIA_DOOR.getDefaultInstance(),
        Items.SPRUCE_DOOR.getDefaultInstance(),
        Items.WARPED_DOOR.getDefaultInstance(),
        Items.COPPER_DOOR.getDefaultInstance(),
        Items.CRIMSON_DOOR.getDefaultInstance(),
        Items.MANGROVE_DOOR.getDefaultInstance(),
        Items.DARK_OAK_DOOR.getDefaultInstance(),
        Items.EXPOSED_COPPER_DOOR.getDefaultInstance(),
        Items.OXIDIZED_COPPER_DOOR.getDefaultInstance(),
        Items.WEATHERED_COPPER_DOOR.getDefaultInstance()
    };
    private static final ItemStack[] menuIcons = {
        Items.CAKE.getDefaultInstance(),
        Items.SPAWNER.getDefaultInstance(),
        Items.BEDROCK.getDefaultInstance(),
        Items.GOAT_HORN.getDefaultInstance(),
        Items.HONEYCOMB.getDefaultInstance(),
        Items.LODESTONE.getDefaultInstance(),
        Items.DRAGON_EGG.getDefaultInstance(),
        Items.FILLED_MAP.getDefaultInstance(),
        Items.PINK_TULIP.getDefaultInstance(),
        Items.TURTLE_EGG.getDefaultInstance(),
        Items.NETHER_STAR.getDefaultInstance(),
        Items.WITHER_ROSE.getDefaultInstance(),
        Items.PINK_PETALS.getDefaultInstance(),
        Items.WARPED_SIGN.getDefaultInstance(),
        Items.CHERRY_SIGN.getDefaultInstance(),
        Items.WIND_CHARGE.getDefaultInstance(),
        Items.WRITTEN_BOOK.getDefaultInstance(),
        Items.DAMAGED_ANVIL.getDefaultInstance(),
        Items.CHERRY_SAPLING.getDefaultInstance(),
        Items.JACK_O_LANTERN.getDefaultInstance(),
        Items.KNOWLEDGE_BOOK.getDefaultInstance(),
        Items.FIREWORK_ROCKET.getDefaultInstance(),
        Items.TOTEM_OF_UNDYING.getDefaultInstance(),
        Items.LIME_SHULKER_BOX.getDefaultInstance(),
        Items.AMETHYST_CLUSTER.getDefaultInstance(),
        Items.FLOWERING_AZALEA.getDefaultInstance(),
        Items.PINK_SHULKER_BOX.getDefaultInstance(),
        Items.GILDED_BLACKSTONE.getDefaultInstance(),
        Items.OMINOUS_TRIAL_KEY.getDefaultInstance(),
        Items.HEART_POTTERY_SHERD.getDefaultInstance(),
        Items.LIGHT_BLUE_SHULKER_BOX.getDefaultInstance(),
        Items.ENCHANTED_GOLDEN_APPLE.getDefaultInstance(),
        Items.HEARTBREAK_POTTERY_SHERD.getDefaultInstance(),
        Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultInstance(),
        discIcons[ThreadLocalRandom.current().nextInt(discIcons.length)],
        doorIcons[ThreadLocalRandom.current().nextInt(doorIcons.length)],
        getCustomIcons()[ThreadLocalRandom.current().nextInt(getCustomIcons().length)]
    };

    private static ItemStack[] getCustomIcons() {
        // Encoded profile textures taken from illegal player head items on 2b2t.org (except for mine.)
        final String tasHeadTexture = "ewogICJ0aW1lc3RhbXAiIDogMTcyODQwNzM3MDc3MiwKICAicHJvZmlsZUlkIiA6ICJjZTA5ODE3NzBkMjc0NmY1YTM3ODUxODg5NzcxYmEyNyIsCiAgInByb2ZpbGVOYW1lIiA6ICIweFRhcyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yZGNlNGNlNWVhOWJjNWI1OTI1MmJlNDk1YTA5ZTQ0ZWFmMzc5NmRmNDY5OTU2MTdmZGQ4ZjFmMTBkNjU0ZjQyIgogICAgfQogIH0KfQ==";
        final String popbobHeadTexture = "eyJ0aW1lc3RhbXAiOjE0MTYwOTQxOTU4NTYsInByb2ZpbGVJZCI6IjBmNzVhODFkNzBlNTQzYzViODkyZjMzYzUyNDI4NGYyIiwicHJvZmlsZU5hbWUiOiJwb3Bib2IiLCJpc1B1YmxpYyI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzEyNTY4ODQ4NWI3MjUxMWFmOWY4NzVjZjQ4NjlmNjYxOTkwNWU2ZjJjNzc3NGIyMjYxNTJjYTY3ODIzODFlNiJ9fX0=";
        final String pyrobyteHeadTexture = "eyJ0aW1lc3RhbXAiOjE0MTYwOTQxOTUxOTUsInByb2ZpbGVJZCI6IjY4YjFiYjExY2ZhMzRlMTZhMDFkYjZkZGRhMGExMDgzIiwicHJvZmlsZU5hbWUiOiJQeXJvYnl0ZSIsImlzUHVibGljIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzhjZTMwODMxYjU1YTI0MTFjMGYzMTI2ZDVhNThlMzE2NDZkNGE4YjZmMzYxZjcyMzc5ZGY0ZTY5OTE0OTkifX19";
        final String iTristanHeadTexture = "ewogICJ0aW1lc3RhbXAiIDogMTcyODUwMDk2NjEwMywKICAicHJvZmlsZUlkIiA6ICI4ZDNmYTEyMmFjNGI0YjM1OGI1MzM5Mjc5NGJkZDU2MSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVTZW5wYWlPZjJiMnQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTcxNTFkNzliMDQzZWY5N2FkMjhhMjc5NDVmODY3OGRmMmE3OGU2NGE1MmQxYzkzMDgwNTdhMjFmMDQyMDNlNCIKICAgIH0KICB9Cn0=";
        final String hausemasterHeadTexture = "eyJ0aW1lc3RhbXAiOjE0MTYwOTQxOTU2NjIsInByb2ZpbGVJZCI6IjhmMmNlNDUzY2VmMjRiM2ViNjg2ZGMyMWI1MTlhMGExIiwicHJvZmlsZU5hbWUiOiJIYXVzZW1hc3RlciIsImlzUHVibGljIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGJiY2IyZTE5OTdjN2NiMWJkZjU2MTNkMTMyZWVjNmQ2NzEzM2EyMTYyMWUwZmFlMTU3YTZhZDhmOWIyIn19fQ==";
        final String jackTheRippaHeadTexture = "eyJ0aW1lc3RhbXAiOjE0MTYwOTQxOTUxOTMsInByb2ZpbGVJZCI6IjdmMTk3NjE4MzJjMjQ4NzY4NDFiY2VhMjliZDU4Y2FlIiwicHJvZmlsZU5hbWUiOiJKYWNrdGhlcmlwcGEiLCJpc1B1YmxpYyI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzExYjk0OWE2MWZhNGNjOGZmZjNkM2I0OTY4MmQyZjk2ZjQxMThmOTI4ZDg2MjIyMmVmNjU2ZTMyYTVmMTIifX19";
        final String cytoToxicTCellHeadTexture = "eyJ0aW1lc3RhbXAiOjE0MDY0MTc0NTE1MDgsInByb2ZpbGVJZCI6ImE0YTVlYmM0OWY0ZTQ3OTVhMjUzN2I4YjA1M2ZiMTdmIiwicHJvZmlsZU5hbWUiOiJDeXRvdG94aWNUY2VsbCIsImlzUHVibGljIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTlkMWU2YzRmNjFkZmNmZGE2NDE3MjJmNjU3NzJiMTI3YmI0NDFkMGViMjU4YTM2Y2MxOTEzYmU3NTkyNGIxIn19fQ==";

        // Get textures for the current player's head item
        Optional<Property> currentPlayerProfileProperties = mc.getGameProfile().getProperties().get("textures").stream().findFirst();

        String currentPlayerHeadTexture;
        if (currentPlayerProfileProperties.isPresent()) {
            currentPlayerHeadTexture = currentPlayerProfileProperties.get().value();
        } else {
            currentPlayerHeadTexture = "ewogICJ0aW1lc3RhbXAiIDogMTcyODQ5NzQxNzUwNCwKICAicHJvZmlsZUlkIiA6ICJkMDUwMzNmYzM3N2Q0OGU1ODFiMGJhYTY0NDBmNTIyOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJQYXVsc3RldmUwMDciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTk1YmQzOWQ5M2ZiYjI4NGVhNGEzYmJiMTljNzRlNTUxOGQwODRiNmZiMGQ5YjE1ZWQ2YzU2NzdmMDhkY2FhYyIKICAgIH0KICB9Cn0=";
        }

        String[] playerHeadTextures = {
            currentPlayerHeadTexture, tasHeadTexture, popbobHeadTexture, pyrobyteHeadTexture,
            iTristanHeadTexture, hausemasterHeadTexture, jackTheRippaHeadTexture, cytoToxicTCellHeadTexture
        };

        ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
        GameProfile profile = new GameProfile(UUID.randomUUID(), "Stardust");
        ResolvableProfile profileComponent = new ResolvableProfile(profile);

        // Apply a player head texture to the ItemStack
        profileComponent.properties().put(
            "textures",
            new Property(
                "textures", // Select a random player head texture from the playerHeadTextures array.
                playerHeadTextures[ThreadLocalRandom.current().nextInt(playerHeadTextures.length)],""
            )
        );
        playerHead.set(DataComponents.PROFILE, profileComponent);

        ItemStack enchantedPick = new ItemStack(
            ThreadLocalRandom.current().nextInt(2) == 0 ? Items.DIAMOND_PICKAXE : Items.NETHERITE_PICKAXE);
        enchantedPick.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        ItemStack[] enchantedGlass = new ItemStack[] {
            Items.GLASS.getDefaultInstance(),
            Items.RED_STAINED_GLASS.getDefaultInstance(),
            Items.CYAN_STAINED_GLASS.getDefaultInstance(),
            Items.LIME_STAINED_GLASS.getDefaultInstance(),
            Items.PINK_STAINED_GLASS.getDefaultInstance(),
            Items.WHITE_STAINED_GLASS.getDefaultInstance(),
            Items.BLACK_STAINED_GLASS.getDefaultInstance(),
            Items.LIGHT_BLUE_STAINED_GLASS.getDefaultInstance(),
        };

        for (ItemStack g : enchantedGlass) {
            g.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        ItemStack cgiElytra = new ItemStack(Items.ELYTRA);
        cgiElytra.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        ItemStack sword32k = new ItemStack(
            ThreadLocalRandom.current().nextInt(2) == 0 ? Items.DIAMOND_SWORD : Items.WOODEN_SWORD);
        sword32k.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        ItemStack illegalBow = new ItemStack(Items.BOW);
        illegalBow.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        ItemStack bindingPumpkin = new ItemStack(Items.CARVED_PUMPKIN);
        bindingPumpkin.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        ItemStack ripTridentFly = new ItemStack(Items.TRIDENT);
        ripTridentFly.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        return new ItemStack[] {
            playerHead,
            enchantedPick, sword32k, illegalBow, bindingPumpkin, cgiElytra, ripTridentFly,
            enchantedGlass[ThreadLocalRandom.current().nextInt(enchantedGlass.length)]
        };
    }

    public static boolean checkOrCreateFile(Minecraft mc, String fileName) {
        File file =FabricLoader.getInstance().getGameDir().resolve(fileName).toFile();

        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    if (mc.player != null) {
                        MsgUtil.sendMsg("Created " + file.getName() + " in your meteor-client folder.");
                        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));

                        MsgUtil.sendMsg("Click §2§lhere §r§7to open the file.", style);
                    }
                    return true;
                }
            }catch (Exception err) {
                LogUtil.error("Error creating " + file.getAbsolutePath() + "! - Why:\n" + err, "StardustUtil#checkOrCreateFile");
            }
        } else return true;

        return false;
    }

    public static void openFile(String fileName) {
        File file = FabricLoader.getInstance().getGameDir().resolve(fileName).toFile();

        try {
            Runtime runtime = Runtime.getRuntime();
            if (System.getenv("OS") == null) return;
            if (System.getenv("OS").contains("Windows")) {
                runtime.exec(new String[]{"rundll32", "url.dll,", "FileProtocolHandler", file.getAbsolutePath()});
            }else {
                runtime.exec(new String[]{"xdg-open", file.getAbsolutePath()});
            }
        } catch (Exception err) {
            MsgUtil.sendMsg("Failed to open " + file.getName() + "§c..!");
            LogUtil.error("Failed to open " + file.getAbsolutePath() + "! - Why:\n" + err, "StardustUtil#openFile");
        }
    }

    public static boolean isIn2b2tQueue() {
        if (mc.player == null || mc.getConnection() == null) return false;

        return PlayerUtils.getDimension().equals(Dimension.End)
            && mc.player.getAbilities().mayfly && mc.getConnection().getOnlinePlayers().size() <= 1;
    }

    public enum IllegalDisconnectMethod {
        Slot, Chat, Interact, Movement, SequenceBreak, InvalidSettings
    }

    public static void illegalDisconnect(boolean disableAutoReconnect, IllegalDisconnectMethod illegalDisconnectMethod) {
        if (!Utils.canUpdate()) return;
        if (disableAutoReconnect) disableAutoReconnect();

        Packet<?> illegalPacket = null;
        switch (illegalDisconnectMethod) {
            case Slot -> illegalPacket = new ServerboundSetCarriedItemPacket(-69);
            case Chat -> illegalPacket = new ServerboundChatPacket(
                "§",
                Instant.now(),
                Crypt.SaltSupplier.nextLong(),
                null,
                ((ClientPlayNetworkHandlerAccessor) mc.getConnection()).getLastSeenMessagesCollector().collect().update()
            );
            case Interact -> illegalPacket = PlayerInteractEntityC2SPacket.interact(mc.player, false, InteractionHand.MAIN_HAND);
            case Movement -> illegalPacket = new PlayerMoveC2SPacket.PositionAndOnGround(Double.NaN, 69, Double.NaN, false, false);
            case SequenceBreak -> illegalPacket = new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, -420, 13.37F, 69.69F);
            case InvalidSettings -> illegalPacket = new ServerboundClientInformationPacket(new ClientInformation(
                mc.options.languageCode, -69,
                mc.options.chatVisibility().get(), mc.options.chatColors().get(),
                mc.options.buildPlayerInformation().modelCustomisation(), mc.options.mainHand().get(),
                mc.options.buildPlayerInformation().textFilteringEnabled(), mc.options.allowServerListing().get(),
                mc.options.buildPlayerInformation().particleStatus()
            ));
        }
        if (illegalPacket != null) ((ClientConnectionAccessor) mc.getConnection().getConnection()).invokeSendImmediately(
            illegalPacket, null, true
        );
    }

    public static void disableAutoReconnect() {
        Modules mods = Modules.get();
        if (mods == null) return;
        AutoReconnect atrc = mods.get(AutoReconnect.class);
        if (atrc.isActive()) atrc.toggle();
    }
}
