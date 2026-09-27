package dev.stardust.modules;

import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.nio.file.Files;
import net.minecraft.world.level.block.*;
import dev.stardust.Stardust;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import dev.stardust.util.MsgUtil;
import dev.stardust.util.LogUtil;
import java.util.stream.Collectors;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import dev.stardust.util.StardustUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;

import net.minecraft.world.level.block.state.BlockState;
/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class WaxAura extends Module {
    public WaxAura() { super(Stardust.CATEGORY, "WaxAura", "Automatically waxes signs within your reach."); }

    private final String BLACKLIST_FILE = "meteor-client/waxaura-blacklist.txt";

    private final SettingGroup sgWax = settings.createGroup("Wax Settings");
    private final SettingGroup sgESP = settings.createGroup("ESP Settings");
    private final SettingGroup sgBlacklist = settings.createGroup("Blacklist Settings");

    private final Setting<Boolean> hotbarOnly = sgWax.add(
        new BoolSetting.Builder()
            .name("hotbar-only")
            .description("Only use honeycombs if they're already in your hotbar.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> swapBack = sgWax.add(
        new BoolSetting.Builder()
            .name("swap-back")
            .description("Swap honeycombs back to where they came from in your inventory after using.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> standingStill = sgWax.add(
        new BoolSetting.Builder()
            .name("standing-still")
            .description("Wait until you are standing still to wax signs. Prevents rubberbanding with a low tick-rate value.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> hangingSigns = sgWax.add(
        new BoolSetting.Builder()
            .name("hanging-signs")
            .description("Wax hanging signs in addition to other types of signs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> contentBlacklist = sgBlacklist.add(
        new BoolSetting.Builder()
            .name("content-blacklist")
            .description("Ignore waxing signs that contain specific words or phrases (line-separated list in waxaura-blacklist.txt)")
            .defaultValue(false)
            .onChanged(it -> {
                if (it && StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) {
                    this.blacklisted.clear();
                    initBlacklistText();
                    if (mc.player != null) {
                        MsgUtil.sendModuleMsg("Please write one blacklisted item for each line fo the file.", this.name);
                        MsgUtil.sendModuleMsg("Spaces and other punctuation will be treated literally.", this.name);
                        MsgUtil.sendModuleMsg("Toggle the module after updating the file's contents.", this.name);
                    }
                }
            })
            .build()
    );

    private final Setting<Boolean> openBlacklistFile = sgBlacklist.add(
        new BoolSetting.Builder()
            .name("open-blacklist-file")
            .description("Open the waxaura-blacklist.txt file.")
            .defaultValue(false)
            .onChanged(it -> {
                if (it) {
                    if (StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) StardustUtil.openFile(BLACKLIST_FILE);
                    resetBlacklistFileSetting();
                }
            })
            .build()
    );

    private final Setting<Boolean> espNonWaxed = sgESP.add(
        new BoolSetting.Builder()
            .name("ESP-unwaxed-signs")
            .description("Render signs which aren't yet waxed through walls.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> espRange = sgESP.add(
        new IntSetting.Builder()
            .name("ESP-range")
            .description("Range in blocks to render unwaxed signs.")
            .range(6, 512)
            .sliderRange(16, 256)
            .defaultValue(128)
            .build()
    );

    private final Setting<ESPBlockData> espSettings = sgESP.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("ESP-settings")
            .defaultValue(
                new ESPBlockData(
                    ShapeMode.Both,
                    new SettingColor(234, 255, 42, 255),
                    new SettingColor(255, 41, 0, 44),
                    true,
                    new SettingColor(239, 255, 59, 200)
                )
            )
            .build()
    );

    private final Setting<Integer> tickRate = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("tick-rate")
            .range(2, 200)
            .sliderRange(2, 20)
            .defaultValue(2)
            .build()
    );

    private int timer = 0;
    private int combSlot = -1;
    private int rotPriority = 69420;
    private SignBlockEntity currentSign = null;
    private final HashSet<String> blacklisted = new HashSet<>();
    private final HashSet<BlockPos> signsToESP = new HashSet<>();
    private final HashSet<SignBlockEntity> signsToWax = new HashSet<>();

    private void initBlacklistText() {
        File blackListFile = FabricLoader.getInstance().getGameDir().resolve(BLACKLIST_FILE).toFile();

        try(Stream<String> lineStream = Files.lines(blackListFile.toPath())) {
            blacklisted.addAll(lineStream.toList());
        }catch (Exception err) {
            LogUtil.error("Failed to read from "+ blackListFile.getAbsolutePath() +"! - Why:\n"+err, this.name);
        }
    }

    private void resetBlacklistFileSetting() { openBlacklistFile.set(false); }

    private boolean isSignEmpty(SignBlockEntity sbe) {
        return !sbe.getFrontText().hasMessage(mc.player) && !sbe.getBackText().hasMessage(mc.player);
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

    private void getSignsToESP() {
        for (BlockEntity be : Utils.blockEntities()) {
            if (be instanceof SignBlockEntity sbe && !sbe.isWaxed() && !isSignEmpty(sbe)) {
                if (!contentBlacklist.get() || !containsBlacklistedText(sbe)) signsToESP.add(sbe.getBlockPos());
            }
        }
    }

    private void getSignsToWax() {
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        for (BlockPos pos : BlockPos.withinManhattan(mc.player.blockPosition(), 5, 5, 5)) {
            if (mc.level.getBlockEntity(pos) instanceof SignBlockEntity sbe && !sbe.isWaxed() && !isSignEmpty(sbe)) {
                if (!contentBlacklist.get() || !containsBlacklistedText(sbe)) signsToWax.add(sbe);
            }
        }
    }

    private void waxSign(SignBlockEntity sbe) {
        if (mc.player == null || mc.gameMode == null) return;

        BlockPos pos = sbe.getBlockPos();
        Vec3 hitVec = Vec3.atCenterOf(pos);
        BlockHitResult hit = new BlockHitResult(hitVec, mc.player.getDirection().getOpposite(), pos, false);

        ItemStack current = mc.player.getInventory().getSelectedItem();
        if (current.getItem() != Items.HONEYCOMB) {
            int end;
            if (hotbarOnly.get()) {
                end = 9;
            } else end = mc.player.getInventory().getNonEquipmentItems().size();

            for (int n = 0; n < end; n++) {
                ItemStack stack = mc.player.getInventory().getItem(n);
                if (stack.getItem() == Items.HONEYCOMB) {
                    combSlot = n;
                    timer = Math.max(0, tickRate.get() - 5);
                    if (n < 9) InvUtils.swap(n, true);
                    else InvUtils.move().from(n).to(mc.player.getInventory().getSelectedSlot());
                    return;
                }
            }
        } else {
            Rotations.rotate(
                Rotations.getYaw(pos),
                Rotations.getPitch(pos), rotPriority,
                () -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit)
            );
            ++rotPriority;
            currentSign = null;
            if (swapBack.get()) timer = -1;
        }
    }

    @Override
    public void onActivate() {
        if (mc.level == null) {
            toggle();
            return;
        }
        if (contentBlacklist.get() && StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) initBlacklistText();
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        combSlot = -1;
        currentSign = null;
        signsToWax.clear();
        signsToESP.clear();
        rotPriority = 69420;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.gameMode == null) return;

        getSignsToESP();
        if (standingStill.get()) {
            Vec3 vel = mc.player.getDeltaMovement();
            if (vel.length() >= 0.08d) return;
        }
        if (timer % 2 == 0) getSignsToWax();

        ++timer;
        ItemStack active = mc.player.getActiveItem();
        if ((active.has(DataComponents.FOOD) || Utils.isThrowable(active.getItem())) && mc.player.getTicksUsingItem() > 0) return;
        if (timer >= tickRate.get()) {
            timer = 0;
            if (currentSign != null) waxSign(currentSign);
            else {
                synchronized (signsToWax) {
                    signsToWax.removeIf(this::isSignEmpty);
                    signsToWax.removeIf(SignBlockEntity::isWaxed);
                    signsToWax.removeIf(sbe -> contentBlacklist.get() && containsBlacklistedText(sbe));
                    signsToWax.removeIf(sbe -> !hangingSigns.get() && sbe instanceof HangingSignBlockEntity);
                    signsToWax.removeIf(sbe -> !sbe.getBlockPos().closerThan(mc.player.blockPosition(), 6));

                    if (signsToWax.isEmpty()) {
                        if (swapBack.get() && combSlot != -1) {
                            if (combSlot < 9) InvUtils.swapBack();
                            else InvUtils.move().from(mc.player.getInventory().getSelectedSlot()).to(combSlot);
                            combSlot = -1;
                        }
                        return;
                    }
                    currentSign = signsToWax.stream().toList().get(0);

                    waxSign(currentSign);
                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null || !espNonWaxed.get()) return;
        List<BlockPos> valid = signsToESP
            .stream()
            .filter(pos -> pos.closerThan(mc.player.blockPosition(), espRange.get()))
            .filter(pos -> mc.level.getBlockEntity(pos) instanceof SignBlockEntity sbe && !sbe.isWaxed())
            .filter(pos -> hangingSigns.get() || !(mc.level.getBlockEntity(pos) instanceof HangingSignBlockEntity))
            .toList();

        ESPBlockData esp = espSettings.get();
        for (BlockPos pos : valid) {
            BlockState state = mc.level.getBlockState(pos);
            VoxelShape shape = state.getOutlineShape(mc.level, pos);

            double x1 = pos.getX() + shape.min(Direction.Axis.X);
            double y1 = pos.getY() + shape.min(Direction.Axis.Y);
            double z1 = pos.getZ() + shape.min(Direction.Axis.Z);
            double x2 = pos.getX() + shape.max(Direction.Axis.X);
            double y2 = pos.getY() + shape.max(Direction.Axis.Y);
            double z2 = pos.getZ() + shape.max(Direction.Axis.Z);

            event.renderer.box(
                x1, y1, z1, x2, y2, z2,
                esp.sideColor, esp.lineColor, esp.shapeMode, 0
            );

            if (esp.tracer) {
                try {
                    double offsetX;
                    double offsetY;
                    double offsetZ;
                    if (state.getBlock() instanceof SignBlock || state.getBlock() instanceof HangingSignBlock) {
                        offsetX = pos.getX() + .5;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .5;
                    } else if (state.getBlock() instanceof WallSignBlock || state.getBlock() instanceof WallHangingSignBlock) {
                        Direction facing;
                        if (state.getBlock() instanceof WallSignBlock) {
                            facing = state.getValue(WallSignBlock.FACING);
                        } else facing = state.getValue(WallHangingSignBlock.FACING);
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

                    event.renderer.line(
                        RenderUtils.center.x,
                        RenderUtils.center.y,
                        RenderUtils.center.z,
                        offsetX,
                        offsetY,
                        offsetZ,
                        esp.tracerColor
                    );
                } catch (Exception err) {
                    LogUtil.error(err.toString(), this.name);
                }
            }
        }
    }
}
