package dev.stardust.modules;

import java.util.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import net.minecraft.world.item.*;
import dev.stardust.Stardust;
import net.minecraft.world.level.block.*;
import net.minecraft.network.chat.Component;
import java.util.stream.Stream;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Tuple;
import net.minecraft.nbt.NbtOps;
import dev.stardust.util.MsgUtil;
import dev.stardust.util.LogUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import java.util.stream.Collectors;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import dev.stardust.util.StardustUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import java.nio.file.StandardOpenOption;
import net.minecraft.core.Direction;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.util.Mth;
import com.mojang.serialization.DataResult;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.fabricmc.loader.api.FabricLoader;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.BlockHitResult;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.client.multiplayer.ServerData;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.client.player.LocalPlayer;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import dev.stardust.mixin.accessor.ClientConnectionAccessor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import dev.stardust.mixin.accessor.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class SignHistorian extends Module {
    public SignHistorian() {
        super(Stardust.CATEGORY, "SignHistorian", "Records & restores broken or modified signs.");
    }

    private final String BLACKLIST_FILE = "meteor-client/sign-historian/content-blacklist.txt";

    private final SettingGroup sgESP = settings.createGroup("ESP Settings");
    private final SettingGroup sgSigns = settings.createGroup("Signs Settings");
    private final SettingGroup sgBlacklist = settings.createGroup("Content Blacklist");
    private final SettingGroup sgPrevention = settings.createGroup("Grief Prevention");

    private final Setting<Boolean> espSigns = sgESP.add(
        new BoolSetting.Builder()
            .name("ESP-signs")
            .description("Quick toggle for broken/modified ESP.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> espRange = sgESP.add(
        new IntSetting.Builder()
            .name("ESP-range")
            .description("Range in blocks to render broken or modified signs.")
            .range(16, 512)
            .sliderRange(16, 256)
            .defaultValue(128)
            .build()
    );

    private final Setting<Boolean> dynamicColor = sgESP.add(
        new BoolSetting.Builder()
            .name("dynamic-color")
            .description("Derive ESP side color from the sign's wood type.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ESPBlockData> destroyedSettings = sgESP.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("destroyed/Missing-signs-ESP")
            .description("Tip: left-click on the block a ghost sign is rendered on to view its original text content in your chat.")
            .defaultValue(
                new ESPBlockData(
                    ShapeMode.Both,
                    new SettingColor(255, 42, 0, 255),
                    new SettingColor(255, 42, 0, 69),
                    true,
                    new SettingColor(255, 42, 0, 137)
                )
            )
            .build()
    );

    private final Setting<ESPBlockData> modifiedSettings = sgESP.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("modified-signs-ESP")
            .description("Tip: right-click on a modified sign to view its original text content in your chat.")
            .defaultValue(
                new ESPBlockData(
                    ShapeMode.Both,
                    new SettingColor(237, 255, 42, 255),
                    new SettingColor(237, 255, 42, 44),
                    true,
                    new SettingColor(237, 255, 42, 137)
                )
            )
            .build()
    );

    private final Setting<Boolean> strictSetting = sgSigns.add(
        new BoolSetting.Builder()
            .name("strict-mode")
            .description("Only consider signs to be restored if they have the same dye color and glow ink values as the original.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> persistenceSetting = sgSigns.add(
        new BoolSetting.Builder()
            .name("persistence")
            .description("Save sign data to a file in order to persist SignHistorian's powers across play-sessions.")
            .defaultValue(false)
            .onChanged(it -> {
                if (it) {
                    if (this.serverSigns.isEmpty()) {
                        initOrLoadFromSignFile();
                    } else {
                        for (Tuple<SignBlockEntity, BlockState> entry : this.serverSigns.values()) {
                            this.saveSignToFile(entry.getLeft(), entry.getRight());
                        }
                        initOrLoadFromSignFile();
                    }
                }
            })
            .build()
    );

    private final Setting<Boolean> ignoreBrokenSetting = sgSigns.add(
        new BoolSetting.Builder()
            .name("ignore-purposefully-broken")
            .description("Ignores signs you break on purpose (but still tracks them in case you change your mind later.)")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> waxRestoration = sgSigns.add(
        new BoolSetting.Builder()
            .name("wax-restored-signs")
            .description("Automatically waxes signs that SignHistorian has restored.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> packetDelay = sgSigns.add(
        new IntSetting.Builder()
            .name("packet-delay")
            .description("How many ticks to delay before sending the UpdateSign packet. Lower values have a higher chance of being rejected by the AC.")
            .range(0, 500).sliderRange(0, 50).defaultValue(20)
            .build()
    );

    private final Setting<Boolean> contentBlacklist = sgBlacklist.add(
        new BoolSetting.Builder()
            .name("content-blacklist")
            .description("Ignore signs that contain specific words or phrases (line-separated list in sign-historian/content-blacklist.txt)")
            .defaultValue(false)
            .onChanged(it -> {
                if (it && StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) {
                    this.blacklisted.clear();
                    initBlacklistText();
                    if (mc.player != null) {
                        MsgUtil.sendModuleMsg("Please write one blacklisted item for each line of the file.", this.name);
                        MsgUtil.sendModuleMsg("Spaces and other punctuation will be treated literally.", this.name);
                        MsgUtil.sendModuleMsg("You must toggle this setting or the module after updating the file's contents.", this.name);
                    }
                }
            })
            .build()
    );

    private final Setting<Boolean> openBlacklistFile = sgBlacklist.add(
        new BoolSetting.Builder()
            .name("open-blacklist-file")
            .description("Open the content-blacklist.txt file.")
            .defaultValue(false)
            .onChanged(it -> {
                if (it) {
                    if (StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) StardustUtil.openFile(BLACKLIST_FILE);
                    resetBlacklistFileSetting();
                }
            })
            .build()
    );

    private final Setting<Boolean> griefPrevention = sgPrevention.add(
        new BoolSetting.Builder()
            .name("mob-grief-alarm")
            .description("Warns you when nearby signs are in danger of an approaching creeper or wither.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> chatNotification = sgPrevention.add(
        new BoolSetting.Builder()
            .name("chat-notification")
            .description("Warns you in chat when nearby signs are in danger of an approaching creeper.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> alarmVolume = sgPrevention.add(
        new DoubleSetting.Builder()
            .name("volume")
            .sliderMax(0)
            .sliderMax(200)
            .defaultValue(0)
            .build()
    );

    private final Setting<ESPBlockData> dangerESP = sgPrevention.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("grief-prevention-ESP")
            .defaultValue(
                new ESPBlockData(
                    ShapeMode.Both,
                    new SettingColor(255, 0, 25, 255),
                    new SettingColor(255, 0, 25, 255),
                    true,
                    new SettingColor(255, 0, 25, 255)
                )
            )
            .build()
    );

    private int timer = 0;
    private int dyeSlot = -1;
    private int pingTicks = 0;
    private int gracePeriod = 0;
    private int packetTimer = 0;
    private int rotationPriority = 69420;
    private boolean didDisableWaxAura = false;
    private BlockPos lastTargetedSign = null;
    private ResourceKey<Level> currentDim = null;
    private final HashSet<String> blacklisted = new HashSet<>();
    private final Set<BlockPos> signsBrokenByPlayer = new HashSet<>();
    private final Set<SignBlockEntity> modifiedSigns = new HashSet<>();
    private final Set<SignBlockEntity> destroyedSigns = new HashSet<>();
    private final HashSet<SignBlockEntity> signsToWax = new HashSet<>();
    private final HashSet<SignBlockEntity> signsToGlowInk = new HashSet<>();
    private final HashMap<Integer, Vec3> trackedGriefers = new HashMap<>();
    private final HashSet<Monster> approachingGriefers = new HashSet<>();
    private final ArrayDeque<ServerboundSignUpdatePacket> packetQueue = new ArrayDeque<>();
    private final HashMap<SignBlockEntity, WoodType> woodTypeMap = new HashMap<>();
    private final HashMap<SignBlockEntity, DyeColor> signsToColor = new HashMap<>();
    private final HashMap<Integer, Tuple<Boolean, Long>> grieferHadLineOfSight = new HashMap<>();
    private final Map<BlockPos, Tuple<SignBlockEntity, BlockState>> serverSigns = new HashMap<>();

    private void initBlacklistText() {
        File blackListFile = FabricLoader.getInstance().getGameDir().resolve(BLACKLIST_FILE).toFile();

        try(Stream<String> lineStream = Files.lines(blackListFile.toPath())) {
            blacklisted.addAll(lineStream.toList());
        }catch (Exception err) {
            LogUtil.error("Failed to read from "+ blackListFile.getAbsolutePath() +"! - Why:\n"+err, this.name);
        }
    }

    private void resetBlacklistFileSetting() { openBlacklistFile.set(false); }

    private void initOrLoadFromSignFile() {
        if (mc.level == null || mc.getConnection() == null) return;
        Path historianFolder = FabricLoader.getInstance().getGameDir().resolve("meteor-client/sign-historian");

        try {
            //noinspection ResultOfMethodCallIgnored
            historianFolder.toFile().mkdirs();
            ServerData server = mc.getConnection().getServerData();
            if (server == null) return;

            String address = server.ip.replace(":", "_");
            String dimKey;
            if (currentDim != null) dimKey = currentDim.location().toString().replace("minecraft:", "");
            else dimKey = mc.level.dimension().identifier().toString().replace("minecraft:", "");
            Path signsFile = historianFolder.resolve( dimKey+"."+address+".signs");
            if (signsFile.toFile().exists()) {
                readSignsFromFile(signsFile);
            } else if (signsFile.toFile().createNewFile()) {
                MsgUtil.sendModuleMsg("Sign data will be saved to §2§o" + signsFile.getFileName() + " §7in your §7§ometeor-client/sign-historian §7folder.", this.name);
                readSignsFromFile(signsFile);
            }
        } catch (Exception err) {
            LogUtil.error(err.toString(), this.name);
        }
    }

    private void readSignsFromFile(Path signsFile) {
        try(Stream<String> lineStream = Files.lines(signsFile)) {
            List<String> entries = lineStream.toList();
            for (String sign : entries) {
                try {
                    String[] parts = sign.split(" -\\|- ");
                    if (parts.length != 2) continue;
                    CompoundTag reconstructed = TagParser.parseTag(parts[0].trim());
                    CompoundTag stateReconstructed = TagParser.parseTag(parts[1].trim());
                    BlockPos bPos = BlockEntity.getPosFromTag(reconstructed);

                    DataResult<BlockState> result = BlockState.CODEC.parse(NbtOps.INSTANCE, stateReconstructed);
                    BlockState state = result.result().orElse(null);

                    if (state == null) continue;
                    BlockEntity be = BlockEntity.createFromNbt(bPos, state, reconstructed, mc.level.registryAccess());

                    if (be instanceof SignBlockEntity sbeReconstructed) {
                        if (!serverSigns.containsKey(bPos)) {
                            if (state.getBlock() instanceof SignBlock signBlock) {
                                woodTypeMap.put(sbeReconstructed, signBlock.getWoodType());
                            }
                            serverSigns.put(bPos, new Tuple<>(sbeReconstructed, sbeReconstructed.getBlockState()));
                        }
                    }
                } catch (Exception err) {
                    LogUtil.error("Failed to parse SignBlockEntity Nbt: "+err, this.name);
                }
            }
        }catch (Exception e) {
            LogUtil.error(e.toString(), this.name);
        }
    }

    private void writeSignToFile(CompoundTag metadata, CompoundTag cachedState, Path signsFile) {
        try {
            Files.writeString(signsFile, metadata+" -|- "+cachedState+"\n", StandardOpenOption.APPEND);
        } catch (Exception err) {
            LogUtil.error(err.toString(), this.name);
        }
    }

    private void saveSignToFile(SignBlockEntity sign, BlockState state) {
        if (mc.level == null || mc.getConnection() == null) return;
        Path historianFolder = FabricLoader.getInstance().getGameDir().resolve("meteor-client/sign-historian");

        try {
            CompoundTag stateNbt = NbtUtils.fromBlockState(state);
            CompoundTag metadata = sign.saveWithFullMetadata(mc.level.registryAccess());

            //noinspection ResultOfMethodCallIgnored
            historianFolder.toFile().mkdirs();
            ServerData server = mc.getConnection().getServerData();
            if (server == null) return;

            String address = server.ip.replace(":", "_");
            String dimKey;
            if (currentDim != null) dimKey = currentDim.location().toString().replace("minecraft:", "");
            else dimKey = mc.level.dimension().identifier().toString().replace("minecraft:", "");
            Path signsFile = historianFolder.resolve(dimKey+"."+address+".signs");
            if (signsFile.toFile().exists()) {
                writeSignToFile(metadata, stateNbt, signsFile);
            } else if (signsFile.toFile().createNewFile()) {
                writeSignToFile(metadata, stateNbt, signsFile);
            }
        } catch (Exception err) {
            LogUtil.error(err.toString(), this.name);
        }
    }

    private Vec3 getTracerOffset(BlockPos pos, BlockState state) {
        double offsetX;
        double offsetY;
        double offsetZ;
        try {
            if (state.getBlock() instanceof SignBlock || state.getBlock() instanceof HangingSignBlock) {
                offsetX = pos.getX() + .5;
                offsetY = pos.getY() + .5;
                offsetZ = pos.getZ() + .5;
            } else if (state.getBlock() instanceof WallSignBlock || state.getBlock() instanceof WallHangingSignBlock) {
                Direction facing = state.getValue(WallSignBlock.FACING);
                switch (facing) {
                    case NORTH -> {
                        offsetX = pos.getX() + .5;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .937;
                    }
                    case EAST -> {
                        offsetX = pos.getX() + .1337;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .5;
                    }
                    case SOUTH -> {
                        offsetX = pos.getX() + .5;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .1337;
                    }
                    case WEST -> {
                        offsetX = pos.getX() + .937;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .5;
                    }
                    default -> {
                        offsetX = pos.getX() + .5;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .5;
                    }
                }
            } else {
                offsetX = pos.getX() + .5;
                offsetY = pos.getY() + .5;
                offsetZ = pos.getZ() + .5;
            }
        } catch (Exception err) {
            offsetX = pos.getX() + .5;
            offsetY = pos.getY() + .5;
            offsetZ = pos.getZ() + .5;
        }

        return new Vec3(offsetX, offsetY, offsetZ);
    }

    private SettingColor colorFromWoodType(WoodType type) {
        if (type == null || !dynamicColor.get()) {
            return dangerESP.get().sideColor;
        } else if (type == WoodType.OAK) {
            return new SettingColor(181, 146, 94, dangerESP.get().sideColor.a);
        } else if (type == WoodType.BIRCH) {
            return new SettingColor(212, 200, 139, dangerESP.get().sideColor.a);
        } else if (type == WoodType.SPRUCE) {
            return new SettingColor(126, 93, 53, dangerESP.get().sideColor.a);
        } else if (type == WoodType.JUNGLE) {
            return new SettingColor(181, 133, 98, dangerESP.get().sideColor.a);
        } else if (type == WoodType.ACACIA) {
            return new SettingColor(170, 92, 49, dangerESP.get().sideColor.a);
        } else if (type == WoodType.BAMBOO) {
            return new SettingColor(133, 124, 53, dangerESP.get().sideColor.a);
        } else if (type == WoodType.CHERRY) {
            return new SettingColor(227, 191, 184, dangerESP.get().sideColor.a);
        } else if (type == WoodType.WARPED) {
            return new SettingColor(57, 140, 138, dangerESP.get().sideColor.a);
        } else if (type == WoodType.CRIMSON) {
            return new SettingColor(124, 57, 85, dangerESP.get().sideColor.a);
        } else if (type == WoodType.MANGROVE) {
            return new SettingColor(109, 41, 44, dangerESP.get().sideColor.a);
        } else if (type == WoodType.DARK_OAK) {
            return new SettingColor(72, 46, 23, dangerESP.get().sideColor.a);
        } else return dangerESP.get().sideColor;
    }

    private BlockPos getTargetedSign() {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return null;
        HitResult trace = player.pick(7,0, false);
        if (trace != null) {
            BlockPos pos = ((BlockHitResult) trace).getBlockPos();
            if (mc.level.getBlockEntity(pos) instanceof SignBlockEntity) return pos;
        }

        return null;
    }

    // See AbstractSignEditScreenMixin.java
    public SignText getRestoration(SignBlockEntity sign) {
        if (!serverSigns.containsKey(sign.getBlockPos())) return null;
        Tuple<SignBlockEntity, BlockState> data = serverSigns.get(sign.getBlockPos());

        if (!destroyedSigns.contains(data.getLeft())) return null;
        if (contentBlacklist.get() && containsBlacklistedText(data.getLeft())) return null;
        if (ignoreBrokenSetting.get() && signsBrokenByPlayer.contains(sign.getBlockPos())) return null;

        SignBlockEntity sbe = data.getLeft();
        Component[] restoration = new Component[4];
        for (int n = 0; n < data.getLeft().getFrontText().getMessages(false).length; n++) {
            // Signs placed in 1.8 - 1.12 (the majority of them) are "technically" irreplaceable due to metadata differences.
            // You might say that they're the *new* old signs. Either way you can tell that they've been (re)placed after 1.19.
            // To compensate for this, I'll hide a SignHistorian watermark in the NBT data which should clear up any confusion :]
            if (sbe.saveWithoutMetadata(mc.level.registryAccess()).toString().contains("{\"extra\":[") && n == 3) {
                StringBuilder sb = new StringBuilder();
                int lineLen = mc.font.width(sbe.getFrontText().getMessage(n, false).getString());
                int spaceLeftHalved = (90 - lineLen) / 2; // center original text

                while (mc.font.width(sb.toString()) < spaceLeftHalved) sb.append(" ");
                sb.append(sbe.getFrontText().getMessage(n, false).getString());
                while (mc.font.width(sb.toString()) < 91) sb.append(" ");
                sb.append("**Pre-1.19 sign restored by 0xTas' SignHistorian**");
                restoration[n] = Component.literal(sb.toString());
            } else {
                restoration[n] = Component.literal(sbe.getFrontText().getMessage(n, false).getString());
            }
        }

        if (sbe.getFrontText().getColor() != DyeColor.BLACK) {
            signsToColor.put(sign, sbe.getFrontText().getColor());
        }
        if (sbe.getFrontText().hasGlowingText()) {
            signsToGlowInk.add(sign);
        }
        if (waxRestoration.get()) {
            signsToWax.add(sign);
        }

        destroyedSigns.remove(sbe);
        return new SignText(restoration, restoration, DyeColor.BLACK, false);
    }

    private boolean isSameSign(SignBlockEntity sbe1, SignBlockEntity sbe2) {
        SignText front1 = sbe1.getFrontText();
        SignText front2 = sbe2.getFrontText();

        int n = 0;
        for (Component line : front1.getMessages(false)) {
            String compensatedLine = line
                .getString()
                .replace("**Pre-1.19 sign restored by 0xTas' SignHistorian**", "")
                .trim();

            if (!compensatedLine.equals(front2.getMessage(n, false).getString().trim())) return false;
            ++n;
        }

        if (strictSetting.get()) {
            if (sbe1.getFrontText().getColor() != sbe2.getFrontText().getColor()) return false;
            if (sbe1.getFrontText().hasGlowingText() != sbe2.getFrontText().hasGlowingText()) return false;
        }

        return ((SignBlock) sbe1.getBlockState().getBlock()).getWoodType() == ((SignBlock) sbe2.getBlockState().getBlock()).getWoodType();
    }

    private boolean containsBlacklistedText(SignBlockEntity sbe) {
        String front = Arrays.stream(sbe.getFrontText().getMessages(false))
            .map(Component::getString)
            .collect(Collectors.joining(" "))
            .trim();

        String back = Arrays.stream(sbe.getBackText().getMessages(false))
            .map(Component::getString)
            .collect(Collectors.joining(" "))
            .trim();

        return blacklisted.stream()
            .anyMatch(line -> front.toLowerCase().contains(line.trim().toLowerCase())
                || back.toLowerCase().contains(line.trim().toLowerCase()));
    }

    private boolean hasNearbySigns() {
        if (!Utils.canUpdate()) return false;
        for (BlockPos pos : BlockPos.withinManhattan(mc.player.blockPosition(), 6, 6, 6)) {
            if (mc.level.getBlockEntity(pos) instanceof SignBlockEntity sbe) {
                if (sbe.getFrontText().hasMessage(mc.player) || sbe.getBackText().hasMessage(mc.player)) return true;
            }
        }
        return false;
    }

    private boolean mobHasLineOfSight(Monster mob) {
        Vec3 mobEyePos = mob.getEyePosition();
        Vec3 eyePos = mc.player.getEyePosition();
        HitResult lineOfSightCheck = mc.level.raycast(
            new ClipContext(
                mobEyePos, eyePos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.WATER, mob
            )
        );

        return lineOfSightCheck.getType() != HitResult.Type.BLOCK;
    }

    private boolean isMobAThreat(Monster mob) {
        if (!Utils.canUpdate()) return false;

        Vec3 newPos = mob.position();
        Vec3 playerPos = mc.player.position();
        Vec3 lastPos = trackedGriefers.get(mob.getId());

        if (lastPos == null) return false;
        double newDistance = playerPos.distanceToSqr(newPos);
        double oldDistance = playerPos.distanceToSqr(lastPos);

        long now = System.currentTimeMillis();
        boolean mobHasLoS = mobHasLineOfSight(mob);
        if (grieferHadLineOfSight.get(mob.getId()) == null) {
            grieferHadLineOfSight.put(mob.getId(), new Tuple<>(mobHasLoS, now));
        } else {
            Tuple<Boolean, Long> prevLoSCheck = grieferHadLineOfSight.get(mob.getId());
            if (mobHasLoS || now - prevLoSCheck.getB() >= 7000) {
                grieferHadLineOfSight.put(mob.getId(), new Tuple<>(mobHasLoS, now));
            }
        }

        if (mob instanceof Creeper creeper) {
            if (newDistance <= Mth.square(10)) {
                return mobHasLoS || (newDistance < oldDistance && grieferHadLineOfSight.get(creeper.getId()).getA());
            } else if (newDistance <= Mth.square(20)) {
                return  (newDistance < oldDistance && mobHasLoS);
            }
        } else if (mob instanceof WitherBoss wither) {
            if (newDistance <= Mth.square(16)) {
                return true;
            } else if (newDistance <= Mth.square(32)) {
                return mobHasLoS || (newDistance < oldDistance && grieferHadLineOfSight.get(wither.getId()).getA());
            } else if (newDistance <= Mth.square(48)) {
                return (newDistance < oldDistance && mobHasLoS);
            }
        }

        return false;
    }

    private void processSign(SignBlockEntity sbe) {
        if (!sbe.getFrontText().hasMessage(mc.player) && !sbe.getBackText().hasMessage(mc.player)) return;
        else if (contentBlacklist.get() &&  containsBlacklistedText(sbe)) return;

        BlockPos pos = sbe.getBlockPos();
        if (serverSigns.containsKey(pos)) {
            if (isSameSign(sbe, serverSigns.get(pos).getLeft())) {
                modifiedSigns.remove(serverSigns.get(pos).getLeft());
            } else {
                modifiedSigns.add(serverSigns.get(pos).getLeft());
            }
            destroyedSigns.remove(serverSigns.get(pos).getLeft());
        } else {
            if (sbe.getBlockState().getBlock() instanceof SignBlock signBlock) {
                woodTypeMap.put(sbe, signBlock.getWoodType());
            }
            serverSigns.put(pos, new Tuple<>(sbe, sbe.getBlockState()));
            if (persistenceSetting.get()) {
                saveSignToFile(sbe, sbe.getBlockState());
            }
        }
    }

    private void interactSign(SignBlockEntity sbe, Item dye) {
        if (!Utils.canUpdate() || mc.gameMode == null) return;

        BlockPos pos = sbe.getBlockPos();
        Vec3 hitVec = Vec3.atCenterOf(pos);
        BlockHitResult hit = new BlockHitResult(hitVec, mc.player.getDirection().getOpposite(), pos, false);

        ItemStack current = mc.player.getInventory().getSelectedItem();
        if (current.getItem() != dye) {
            for (int n = 0; n < mc.player.getInventory().getNonEquipmentItems().size(); n++) {
                ItemStack stack = mc.player.getInventory().getItem(n);
                if (stack.getItem() == dye) {
                    if (current.getItem() instanceof SignItem && current.getCount() > 1) dyeSlot = n;
                    if (n < 9) InvUtils.swap(n, true);
                    else InvUtils.move().from(n).to(mc.player.getInventory().getSelectedSlot());

                    timer = 3;
                    return;
                }
            }
        } else {
            Rotations.rotate(
                Rotations.getYaw(pos),
                Rotations.getPitch(pos), rotationPriority,
                () -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit)
            );
            ++rotationPriority;
        }

        if (dye == Items.GLOW_INK_SAC) {
            signsToGlowInk.remove(sbe);
            if (!signsToWax.contains(sbe) && !signsToColor.containsKey(sbe)) timer = -1;
        } else if (dye == Items.HONEYCOMB){
            signsToWax.remove(sbe);
            if (!signsToColor.containsKey(sbe) && !signsToGlowInk.contains(sbe)) timer = -1;
        } else {
            signsToColor.remove(sbe);
            if (!signsToGlowInk.contains(sbe) && !signsToWax.contains(sbe)) timer = -1;
        }
    }

    @Override
    public void onActivate() {
        if (!Utils.canUpdate()) {
            toggle();
            return;
        }
        if (persistenceSetting.get()) initOrLoadFromSignFile();
        if (contentBlacklist.get() && StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) initBlacklistText();
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        pingTicks = 0;
        gracePeriod = 0;
        currentDim = null;
        signsToWax.clear();
        woodTypeMap.clear();
        serverSigns.clear();
        packetQueue.clear();
        signsToColor.clear();
        modifiedSigns.clear();
        destroyedSigns.clear();
        signsToGlowInk.clear();
        trackedGriefers.clear();
        lastTargetedSign = null;
        rotationPriority = 69420;
        didDisableWaxAura = false;
        approachingGriefers.clear();
        signsBrokenByPlayer.clear();
        grieferHadLineOfSight.clear();
    }

    @EventHandler
    private void onBlockInteract(InteractBlockEvent event) {
        if (!Utils.canUpdate()) return;
        for (SignBlockEntity sbe : modifiedSigns) {
            if (event.result.getBlockPos().closerThan(sbe.getBlockPos(), 1)) {
                MsgUtil.sendModuleMsg("§e§lOriginal§7§l: §7§o" + Arrays.stream(sbe.getFrontText().getMessages(false)).map(Component::getString).collect(Collectors.joining(" ")), this.name);
                MsgUtil.sendModuleMsg(
                    "§6§lWoodType§7§l: " + ((SignBlock) sbe.getBlockState().getBlock()).getWoodType().name()
                    + " | §3§lColor§7§l: " + sbe.getText(true).getColor().name()
                    + " | §f§lGlow Ink§7§l: " + sbe.getText(true).hasGlowingText(), this.name
                );
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockAttack(PacketEvent.Send event) {
        if (!Utils.canUpdate()) return;
        if (!(event.packet instanceof ServerboundPlayerActionPacket packet)) return;
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return;

        for (SignBlockEntity ghost : destroyedSigns) {
            if (packet.getPos().closerThan(ghost.getBlockPos(), 1.5)) {
                MsgUtil.sendModuleMsg("§e§lOriginal§7§l: §7§o" + Arrays.stream(ghost.getFrontText().getMessages(false)).map(Component::getString).collect(Collectors.joining(" ")), this.name);
                MsgUtil.sendModuleMsg(
                    "§6§lWoodType§7§l: " + ((SignBlock) ghost.getBlockState().getBlock()).getWoodType().name()
                        + " | §3§lColor§7§l: " + ghost.getText(true).getColor().name()
                        + " | §f§lGlow Ink§7§l: " + ghost.getText(true).hasGlowingText(), this.name
                );
            }
        }
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (mc.level == null) return;
        if ((event.oldState.getBlock() instanceof SignBlock
            && !(event.newState.getBlock() instanceof SignBlock))
            || (event.oldState.getBlock() instanceof HangingSignBlock
            && !(event.newState.getBlock() instanceof HangingSignBlock))
            || (event.oldState.getBlock() instanceof WallSignBlock
            && !(event.newState.getBlock() instanceof WallSignBlock))
            || (event.oldState.getBlock() instanceof WallHangingSignBlock
            && !(event.newState.getBlock() instanceof WallHangingSignBlock)))
        {
            if (lastTargetedSign == null) return;
            if (lastTargetedSign.getX() == event.pos.getX() && lastTargetedSign.getY() == event.pos.getY() && lastTargetedSign.getZ() == event.pos.getZ()) {
                signsBrokenByPlayer.add(event.pos);
            }
        }
    }

    @EventHandler
    private void onScreenOpened(OpenScreenEvent event) {
        if (!(event.screen instanceof AbstractSignEditScreen editScreen)) return;
        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) editScreen).getBlockEntity();


        SignText restoration = getRestoration(sign);

        if (restoration != null) {
            event.cancel();
            List<String> msgs = Arrays.stream(restoration.getMessages(false)).map(Component::getString).toList();
            String[] messages = new String[msgs.size()];
            messages = msgs.toArray(messages);

            if (packetQueue.isEmpty()) packetTimer = 0;
            packetQueue.addLast(new ServerboundSignUpdatePacket(
                sign.getBlockPos(), true, messages[0], messages[1], messages[2], messages[3]
            ));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null) return;
        if (currentDim == null) currentDim = mc.level.dimension();
        else if (currentDim != mc.level.dimension()) {
            serverSigns.clear();
            currentDim = mc.level.dimension();
            if (persistenceSetting.get()) initOrLoadFromSignFile();
        }

        if (!packetQueue.isEmpty() && mc.getConnection() != null) {
            ++packetTimer;
            if (packetTimer >= packetDelay.get()) {
                packetTimer = 0;
                ((ClientConnectionAccessor) mc.getConnection().getConnection()).invokeSendImmediately(
                    packetQueue.removeFirst(), null, true
                );
            }
        }

        BlockPos targeted = getTargetedSign();
        if (lastTargetedSign == null) lastTargetedSign = targeted;
        else if (targeted == null) {
            if (gracePeriod < 2) ++gracePeriod;
            else {
                gracePeriod = 0;
                lastTargetedSign = null;
            }
        } else lastTargetedSign = targeted;
        if (mc.screen instanceof AbstractSignEditScreen) return;

        if (timer == -1 && dyeSlot != -1) {
            if (dyeSlot < 9) InvUtils.swapBack();
            else InvUtils.move().from(mc.player.getInventory().getSelectedSlot()).to(dyeSlot);
            dyeSlot = -1;
            timer = 3;
        }

        WaxAura waxAura = Modules.get().get(WaxAura.class);
        if (!signsToColor.isEmpty() || !signsToGlowInk.isEmpty() || !signsToWax.isEmpty()) {
            if (waxAura.isActive()) {
                waxAura.toggle();
                didDisableWaxAura = true;
            }
        }

        if (timer % 2 == 0) {
            List<BlockPos> inRange = serverSigns.keySet()
                .stream()
                .filter(pos -> pos.closerThan(mc.player.blockPosition(), espRange.get()))
                .toList();

            for (BlockPos pos : inRange) {
                if (!(mc.level.getBlockEntity(pos) instanceof SignBlockEntity sbe)) {
                    destroyedSigns.add(serverSigns.get(pos).getLeft());
                    modifiedSigns.remove(serverSigns.get(pos).getLeft());
                } else processSign(sbe);
            }

            for (BlockEntity be : Utils.blockEntities()) {
                if (be instanceof SignBlockEntity sbe) processSign(sbe);
            }
        } else if (griefPrevention.get()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                Monster griefingMob;
                if (entity instanceof Creeper creeper) {
                    griefingMob = creeper;
                } else if (entity instanceof WitherBoss wither) {
                    griefingMob = wither;
                } else continue;
                if (!trackedGriefers.containsKey(griefingMob.getId())) trackedGriefers.put(griefingMob.getId(), griefingMob.position());
            }

            if (!hasNearbySigns()) {
                approachingGriefers.clear();
            } else {
                approachingGriefers.removeIf(Entity::isRemoved);
                approachingGriefers.removeIf(mob -> !isMobAThreat(mob));

                ++pingTicks;
                if (!approachingGriefers.isEmpty()) {
                    if (pingTicks >= 60) {
                        pingTicks = 0;
                        mc.player.playSound(SoundEvents.PHANTOM_HURT, alarmVolume.get().floatValue(), 1f);
                        if (chatNotification.get()) {
                            MsgUtil.updateModuleMsg("§c§lNEARBY SIGNS IN DANGER OF MOB GRIEFING§8§L.", this.name, "MobGriefAlarm".hashCode());
                        }
                    }
                }

                List<Integer> toRemove = new ArrayList<>();
                for (int id : trackedGriefers.keySet()) {
                    Entity griefingEntity = mc.level.getEntity(id);
                    if (griefingEntity == null || griefingEntity.isRemoved() || (!(griefingEntity instanceof Creeper) && !(griefingEntity instanceof WitherBoss))) {
                        if (griefingEntity != null) grieferHadLineOfSight.remove(griefingEntity.getId());
                        toRemove.add(id);
                        continue;
                    }
                    Monster griefer = (Monster) griefingEntity;

                    if (isMobAThreat(griefer)) {
                        approachingGriefers.add(griefer);
                    }
                    trackedGriefers.put(id, griefer.position());
                }
                for (int id : toRemove) {
                    trackedGriefers.remove(id);
                }
            }
        }

        ++timer;
        if (timer > 4) {
            timer = 0;
            signsToWax.removeIf(sbe -> !sbe.getBlockPos().closerThan(mc.player.blockPosition(), 6));
            signsToGlowInk.removeIf(sbe -> !sbe.getBlockPos().closerThan(mc.player.blockPosition(), 6));
            List<SignBlockEntity> toColor = signsToColor.keySet()
                .stream()
                .filter(sbe -> sbe.getBlockPos().closerThan(mc.player.blockPosition(), 6))
                .toList();

            if (!toColor.isEmpty()) {
                SignBlockEntity sbe = toColor.get(0);
                interactSign(sbe, DyeItem.byColor(signsToColor.get(sbe)));
                return;
            }

            if (!signsToGlowInk.isEmpty()) {
                List<SignBlockEntity> signs = signsToGlowInk
                    .stream()
                    .toList();

                if (!signs.isEmpty()) {
                    SignBlockEntity sbe = signs.get(0);
                    interactSign(sbe, Items.GLOW_INK_SAC);
                    return;
                }
            }
            if (!signsToWax.isEmpty()) {
                List<SignBlockEntity> signs = signsToWax
                    .stream()
                    .toList();

                if (!signs.isEmpty()) {
                    SignBlockEntity sbe = signs.get(0);
                    interactSign(sbe, Items.HONEYCOMB);
                }
            } else if (didDisableWaxAura && !waxAura.isActive()) {
                waxAura.toggle();
                didDisableWaxAura = false;
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!Utils.canUpdate()) return;
        if (mc.getConnection().getOnlinePlayers().size() <= 1) return; // ignore queue

        if (espSigns.get()) {
            ESPBlockData mESP = modifiedSettings.get();
            ESPBlockData dESP = destroyedSettings.get();
            for (SignBlockEntity sign : destroyedSigns) {
                if (sign.getBlockState() == null) return;
                if (contentBlacklist.get() && containsBlacklistedText(sign)) continue;
                if (ignoreBrokenSetting.get() && signsBrokenByPlayer.contains(sign.getBlockPos())) continue;
                if (!sign.getBlockPos().closerThan(mc.player.blockPosition(), espRange.get())) continue;

                VoxelShape shape = sign.getBlockState().getShape(mc.level, sign.getBlockPos());
                double x1 = sign.getBlockPos().getX() + shape.min(Direction.Axis.X);
                double y1 = sign.getBlockPos().getY() + shape.min(Direction.Axis.Y);
                double z1 = sign.getBlockPos().getZ() + shape.min(Direction.Axis.Z);
                double x2 = sign.getBlockPos().getX() + shape.max(Direction.Axis.X);
                double y2 = sign.getBlockPos().getY() + shape.max(Direction.Axis.Y);
                double z2 = sign.getBlockPos().getZ() + shape.max(Direction.Axis.Z);

                if (dESP.sideColor.a > 0 || dESP.lineColor.a > 0) {
                    WoodType woodType = woodTypeMap.get(sign);
                    event.renderer.box(
                        x1, y1, z1, x2, y2, z2,
                        colorFromWoodType(woodType), dESP.lineColor,
                        dESP.shapeMode, 0
                    );
                }

                if (dESP.tracer && dESP.tracerColor.a > 0) {
                    Vec3 offsetVec = getTracerOffset(sign.getBlockPos(), sign.getBlockState());
                    event.renderer.line(
                        RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                        offsetVec.x, offsetVec.y, offsetVec.z, dESP.tracerColor
                    );
                }
            }
            for (SignBlockEntity sign : modifiedSigns) {
                if (sign.getBlockState() == null) continue;
                if (contentBlacklist.get() && containsBlacklistedText(sign)) continue;
                if (ignoreBrokenSetting.get() && signsBrokenByPlayer.contains(sign.getBlockPos())) continue;
                if (!sign.getBlockPos().closerThan(mc.player.blockPosition(), espRange.get())) continue;

                VoxelShape shape = sign.getBlockState().getShape(mc.level, sign.getBlockPos());
                double x1 = sign.getBlockPos().getX() + shape.min(Direction.Axis.X);
                double y1 = sign.getBlockPos().getY() + shape.min(Direction.Axis.Y);
                double z1 = sign.getBlockPos().getZ() + shape.min(Direction.Axis.Z);
                double x2 = sign.getBlockPos().getX() + shape.max(Direction.Axis.X);
                double y2 = sign.getBlockPos().getY() + shape.max(Direction.Axis.Y);
                double z2 = sign.getBlockPos().getZ() + shape.max(Direction.Axis.Z);

                if (mESP.sideColor.a > 0 || mESP.lineColor.a > 0) {
                    WoodType woodType = woodTypeMap.get(sign);
                    event.renderer.box(
                        x1, y1, z1, x2, y2, z2,
                        colorFromWoodType(woodType), mESP.lineColor,
                        mESP.shapeMode, 0
                    );
                }

                if (mESP.tracer && mESP.tracerColor.a > 0) {
                    Vec3 offsetVec = getTracerOffset(sign.getBlockPos(), sign.getBlockState());
                    event.renderer.line(
                        RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                        offsetVec.x, offsetVec.y, offsetVec.z, mESP.tracerColor
                    );
                }
            }
        }

        if (griefPrevention.get() && !approachingGriefers.isEmpty()) {
            approachingGriefers.removeIf(Entity::isRemoved);
            ESPBlockData dangerColor = dangerESP.get();
            for (Monster griefingMob : approachingGriefers) {
                WireframeEntityRenderer.render(
                    event, griefingMob, 1,
                    dangerColor.sideColor, dangerColor.lineColor, ShapeMode.Both
                );
                if (dangerColor.tracer) {
                    event.renderer.line(
                        RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                        griefingMob.getBoundingBox().getCenter().x, griefingMob.getBoundingBox().getCenter().y,
                        griefingMob.getBoundingBox().getCenter().z, dangerColor.tracerColor
                    );
                }
            }
        }
    }
}
