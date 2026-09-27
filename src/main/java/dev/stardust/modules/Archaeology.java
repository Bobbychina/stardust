package dev.stardust.modules;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import dev.stardust.util.*;
import dev.stardust.Stardust;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.world.phys.BlockHitResult;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.level.block.entity.BlockEntity;
import meteordevelopment.meteorclient.settings.*;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import meteordevelopment.meteorclient.utils.Utils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Archaeology extends Module {
    public Archaeology() { super(Stardust.CATEGORY, "Archaeology", "Tools to assist in your archaeological endeavors."); }

    public enum SuspiciousBlocks {
        Both, SuspiciousSand, SuspiciousGravel
    }

    private static final ReferenceSet<Item> ARCHAEOLOGY_LOOT_TABLE = ReferenceSet.of(
        Items.SHEAF_POTTERY_SHERD, Items.SHELTER_POTTERY_SHERD, Items.ANGLER_POTTERY_SHERD,
        Items.ARCHER_POTTERY_SHERD, Items.ARMS_UP_POTTERY_SHERD, Items.BLADE_POTTERY_SHERD, Items.BREWER_POTTERY_SHERD,
        Items.DANGER_POTTERY_SHERD, Items.BURN_POTTERY_SHERD, Items.EXPLORER_POTTERY_SHERD, Items.FRIEND_POTTERY_SHERD,
        Items.HEART_POTTERY_SHERD, Items.HEARTBREAK_POTTERY_SHERD, Items.HOWL_POTTERY_SHERD, Items.MINER_POTTERY_SHERD,
        Items.MOURNER_POTTERY_SHERD, Items.PLENTY_POTTERY_SHERD, Items.PRIZE_POTTERY_SHERD, Items.SKULL_POTTERY_SHERD,
        Items.SNORT_POTTERY_SHERD, Items.SNIFFER_EGG, Items.MUSIC_DISC_RELIC, Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.EMERALD,
        Items.COAL, Items.TNT, Items.WHEAT, Items.BRICK, Items.STICK, Items.SUSPICIOUS_STEW, Items.DIAMOND, Items.GUNPOWDER, Items.WOODEN_HOE, Items.IRON_AXE,
        Items.GOLD_NUGGET, Items.BLUE_DYE, Items.WHITE_DYE, Items.YELLOW_DYE, Items.DEAD_BUSH, Items.FLOWER_POT, Items.LEAD, Items.WHEAT_SEEDS, Items.STRING,
        Items.SPRUCE_HANGING_SIGN, Items.CLAY, Items.LIGHT_BLUE_DYE, Items.ORANGE_DYE, Items.BROWN_CANDLE, Items.GREEN_CANDLE, Items.RED_CANDLE, Items.PURPLE_CANDLE,
        Items.BEETROOT_SEEDS, Items.BLUE_STAINED_GLASS_PANE, Items.LIGHT_BLUE_STAINED_GLASS_PANE, Items.MAGENTA_STAINED_GLASS_PANE, Items.YELLOW_STAINED_GLASS_PANE,
        Items.OAK_HANGING_SIGN, Items.PINK_STAINED_GLASS_PANE, Items.PURPLE_STAINED_GLASS_PANE, Items.RED_STAINED_GLASS_PANE
    );

    private final SettingGroup sgDig = settings.createGroup("Dig Settings");
    private final SettingGroup sgSurvey = settings.createGroup("Survey Settings");

    private final Setting<List<Item>> targetItems = sgDig.add(
        new ItemListSetting.Builder()
            .name("target-items")
            .description("Which items to follow through with brushing. All others will be ignored or broken.")
            .defaultValue(List.of(
                Items.SHEAF_POTTERY_SHERD, Items.SHELTER_POTTERY_SHERD, Items.ANGLER_POTTERY_SHERD,
                Items.ARCHER_POTTERY_SHERD, Items.ARMS_UP_POTTERY_SHERD, Items.BLADE_POTTERY_SHERD, Items.BREWER_POTTERY_SHERD,
                Items.DANGER_POTTERY_SHERD, Items.BURN_POTTERY_SHERD, Items.EXPLORER_POTTERY_SHERD, Items.FRIEND_POTTERY_SHERD,
                Items.HEART_POTTERY_SHERD, Items.HEARTBREAK_POTTERY_SHERD, Items.HOWL_POTTERY_SHERD, Items.MINER_POTTERY_SHERD,
                Items.MOURNER_POTTERY_SHERD, Items.PLENTY_POTTERY_SHERD, Items.PRIZE_POTTERY_SHERD, Items.SKULL_POTTERY_SHERD,
                Items.SNORT_POTTERY_SHERD, Items.SHEAF_POTTERY_SHERD, Items.SNIFFER_EGG, Items.MUSIC_DISC_RELIC, Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
                Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE
            ))
            .filter(this::isLootItem)
            .build()
    );
    private final Setting<Boolean> breakBad = sgDig.add(
        new BoolSetting.Builder()
            .name("break-bad")
            .description("Break suspicious blocks which don't contain target items.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> chat = sgDig.add(
        new BoolSetting.Builder()
            .name("break-notify")
            .description("Notifies you about the contents of bad suspicious blocks when breaking them.")
            .defaultValue(true)
            .visible(breakBad::get)
            .build()
    );
    private final Setting<Boolean> preventBreak = sgDig.add(
        new BoolSetting.Builder()
            .name("prevent-accidental-breakage")
            .description("Attempts to prevent you from accidentally breaking suspicious sand or gravel blocks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> chatNotify = sgSurvey.add(
        new BoolSetting.Builder()
            .name("chat-notify")
            .description("Notifies you in chat when a cluster of suspicious blocks is found.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> chatCoords = sgSurvey.add(
        new BoolSetting.Builder()
            .name("chat-coordinates")
            .description("Whether to add coordinates to the chat notification messages.")
            .visible(chatNotify::get)
            .defaultValue(false)
            .build()
    );
    private final Setting<SuspiciousBlocks> chatFor = sgSurvey.add(
        new EnumSetting.Builder<SuspiciousBlocks>()
            .name("chats-for")
            .description("Whether to send chat notifications for suspicious sand, gravel, or both.")
            .defaultValue(SuspiciousBlocks.Both)
            .visible(chatNotify::get)
            .build()
    );

    private final Setting<Boolean> waypoints = sgSurvey.add(
        new BoolSetting.Builder()
            .name("add-waypoints")
            .description("Adds waypoints to your Xaeros map on clusters of suspicious blocks.")
            .defaultValue(false)
            .visible(() -> StardustUtil.XAERO_AVAILABLE)
            .build()
    );
    private final Setting<Boolean> tempWaypoints = sgSurvey.add(
        new BoolSetting.Builder()
            .name("temporary-waypoints")
            .description("Temporary waypoints are removed when you disconnect from the server or close the game.")
            .defaultValue(true)
            .visible(() -> StardustUtil.XAERO_AVAILABLE && waypoints.get())
            .build()
    );
    private final Setting<SuspiciousBlocks> waypointsFor = sgSurvey.add(
        new EnumSetting.Builder<SuspiciousBlocks>()
            .name("waypoints-for")
            .description("Whether to add waypoints for suspicious sand, gravel, or both.")
            .defaultValue(SuspiciousBlocks.Both)
            .visible(() -> StardustUtil.XAERO_AVAILABLE && waypoints.get())
            .build()
    );

    private final Setting<Boolean> soundPing = sgSurvey.add(
        new BoolSetting.Builder()
            .name("sound-notification")
            .description("Play an audible notification when discovering an archaeological dig site.")
            .defaultValue(false)
            .build()
    );
    private final Setting<SuspiciousBlocks> soundFor = sgSurvey.add(
        new EnumSetting.Builder<SuspiciousBlocks>()
            .name("ping-for")
            .description("Whether to play sound notifications for suspicious sand, gravel, or both.")
            .defaultValue(SuspiciousBlocks.Both)
            .visible(soundPing::get)
            .build()
    );
    private final Setting<Double> pingVolume = sgSurvey.add(
        new DoubleSetting.Builder()
            .name("notification-volume")
            .description("Volume for the sound notification.")
            .range(0.0, 400.0).sliderRange(0.0, 100.0).defaultValue(50.0)
            .visible(soundPing::get)
            .build()
    );

    private final Setting<Boolean> render = sgSurvey.add(
        new BoolSetting.Builder()
            .name("render-suspicious-blocks")
            .description("Whether to render suspicious blocks.")
            .defaultValue(true)
            .build()
    );
    private final Setting<ESPBlockData> sandESP = sgSurvey.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("suspicious-sand-ESP")
            .description("Render settings for suspicious sand blocks.")
            .visible(render::get)
            .defaultValue(new ESPBlockData(
                ShapeMode.Both,
                new SettingColor(169, 169, 13, 255),
                new SettingColor(169, 169, 13, 7),
                true,
                new SettingColor(169, 169, 13, 137)
            ))
            .build()
    );
    private final Setting<ESPBlockData> gravelESP = sgSurvey.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("suspicious-gravel-ESP")
            .description("Render settings for suspicious gravel blocks.")
            .visible(render::get)
            .defaultValue(new ESPBlockData(
                ShapeMode.Both,
                new SettingColor(69, 69, 69, 255),
                new SettingColor(69, 69, 69, 25),
                true,
                new SettingColor(69, 69, 69, 137)
            ))
            .build()
    );

    private int timer = 0;
    private long lastPing = 0L;
    private int ticksBrushing = 0;
    private long lastNotified = 0L;
    private BlockPos lastFoundPos = null;
    private final List<Long> toIgnore = new LongArrayList();
    private final Set<BlockPos> goodBlocks = new HashSet<>();
    private final BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos testPos2 = new BlockPos.MutableBlockPos();
    private final Set<BlockPos> safeBlocksToBreak = new HashSet<>();
    private final Set<BlockPos> safeBlocksToBrush = new HashSet<>();
    private final Set<BlockPos> preventingBreakageBlocks = new HashSet<>();
    private final List<BlockPos> suspiciousSandBlocks = new ObjectArrayList<>();
    private final List<BlockPos> suspiciousGravelBlocks = new ObjectArrayList<>();

    private boolean isOutOfRange(BlockPos pos1, BlockPos pos2, int range) {
        if (pos1 == null || pos2 == null) return true;
        testPos2.set(pos2.getX(), pos1.getY(), pos2.getZ());
        return !pos1.closerThan(testPos2, range);
    }

    private boolean isSafeToBrush(BlockPos pos) {
        if (mc.level == null) return false;
        if (safeBlocksToBrush.contains(pos)) return true;

        if (mc.level.getBlockState(pos.below()).isAir() || mc.level.getBlockState(pos.below()).canBeReplaced()) {
            preventingBreakageBlocks.add(pos.below());
            MsgUtil.updateModuleMsg("It is not yet safe to brush this suspicious block, because it is §cfloating..! §8[§aTry placing a solid block underneath it§8]", this.name, "directBrushPrevent".hashCode());
            return false;
        }
        BlockPos.MutableBlockPos pos2 = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            pos2.set(pos.relative(dir));
            if (!toIgnore.contains(pos2.asLong())
                && (mc.level.getBlockState(pos2).is(Blocks.SUSPICIOUS_SAND)
                || mc.level.getBlockState(pos2).is(Blocks.SUSPICIOUS_GRAVEL)))
            {
                if (mc.level.getBlockState(pos2.below()).isAir() || mc.level.getBlockState(pos2.below()).canBeReplaced()) {
                    preventingBreakageBlocks.add(pos2);
                    MsgUtil.updateModuleMsg("It is not yet safe to brush this suspicious block, as doing so will update an adjacent floating one§e..!", this.name, "preventBreakageBrush".hashCode());
                    return false;
                }
            }
        }

        safeBlocksToBrush.add(pos);
        return true;
    }

    private boolean isSafeToBreak(BlockPos pos) {
        if (mc.level == null) return false;
        if (safeBlocksToBreak.contains(pos)) return true;
        if (!toIgnore.contains(pos.asLong())
            && (mc.level.getBlockState(pos).is(Blocks.SUSPICIOUS_SAND)
            || mc.level.getBlockState(pos).is(Blocks.SUSPICIOUS_GRAVEL))) return false;

        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        // Indirect block update check
        for (Direction dir : Direction.values()) {
            checkPos.set(pos.relative(dir));
            if (!toIgnore.contains(checkPos.asLong()) && mc.level.getBlockState(checkPos).is(Blocks.SUSPICIOUS_SAND) || mc.level.getBlockState(checkPos).is(Blocks.SUSPICIOUS_GRAVEL)) {
                if (mc.level.getBlockState(checkPos.below()).isAir() || mc.level.getBlockState(checkPos.below()).canBeReplaced()) {
                    preventingBreakageBlocks.add(new BlockPos(checkPos));
                    MsgUtil.updateModuleMsg("§aPreventing accidental breakage from indirect block update for floating suspicious block§c..!", this.name, "indirectBreakPrevent".hashCode());
                    return false;
                }
            }
        }

        checkPos.set(pos.above());
        while (checkPos.getY() < mc.level.getHeight()) {
            BlockState checkState = mc.level.getBlockState(checkPos);
            if (!toIgnore.contains(checkPos.asLong())
                && (checkState.is(Blocks.SUSPICIOUS_GRAVEL)
                || checkState.is(Blocks.SUSPICIOUS_SAND)))
            {
                preventingBreakageBlocks.add(new BlockPos(checkPos));
                return false;
            }
            else if (!(checkState.getBlock() instanceof FallingBlock)) {
                break;
            } else {
                // Indirect block update check
                for (Direction dir : Direction.values()) {
                    // Skip up & down since we're already scanning the column
                    if (dir.equals(Direction.UP) || dir.equals(Direction.DOWN)) continue;
                    BlockPos.MutableBlockPos indirectPos = new BlockPos.MutableBlockPos();
                    indirectPos.set(checkPos.relative(dir));
                    if (!toIgnore.contains(indirectPos.asLong())
                        && (mc.level.getBlockState(indirectPos).is(Blocks.SUSPICIOUS_GRAVEL)
                        || mc.level.getBlockState(indirectPos).is(Blocks.SUSPICIOUS_SAND)))
                    {
                        if (mc.level.getBlockState(indirectPos.below()).isAir() || mc.level.getBlockState(indirectPos.below()).canBeReplaced()) {
                            preventingBreakageBlocks.add(new BlockPos(indirectPos));
                            MsgUtil.updateModuleMsg("§ePreventing accidental breakage from indirect block update for floating suspicious block§c..!", this.name, "indirectBreakPrevent".hashCode());
                            return false;
                        }
                    }
                }
                checkPos.set(checkPos.above());
            }
        }

        safeBlocksToBreak.add(pos);
        return true;
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        lastPing = 0L;
        toIgnore.clear();
        ticksBrushing = 0;
        goodBlocks.clear();
        lastFoundPos = null;
        safeBlocksToBreak.clear();
        suspiciousSandBlocks.clear();
        suspiciousGravelBlocks.clear();
        preventingBreakageBlocks.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onStartBlockBreak(StartBreakingBlockEvent event) {
        if (!Utils.canUpdate() || !preventBreak.get()) return;
        if (!isSafeToBreak(event.blockPos)) {
            event.cancel();
            MsgUtil.updateModuleMsg("Preventing accidental breakage§c..!", this.name, "blockBreakPrevent".hashCode());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        ++timer;
        if (timer >= 5) {
            timer = 0;
            synchronized (suspiciousSandBlocks) {
                suspiciousSandBlocks
                    .removeIf(pos -> isOutOfRange(pos, mc.player.blockPosition(), 256) || !(mc.level.getBlockEntity(pos) instanceof BrushableBlockEntity));
            }
            synchronized (suspiciousGravelBlocks) {
                suspiciousGravelBlocks
                    .removeIf(pos -> isOutOfRange(pos, mc.player.blockPosition(), 256) || !(mc.level.getBlockEntity(pos) instanceof BrushableBlockEntity));
            }
            synchronized (preventingBreakageBlocks) {
                preventingBreakageBlocks.removeIf(pos -> !(mc.level.getBlockEntity(pos) instanceof BrushableBlockEntity));
            }
            safeBlocksToBreak
                .removeIf(pos -> !(mc.level.getBlockEntity(pos) instanceof BrushableBlockEntity) || isOutOfRange(pos, mc.player.blockPosition(), 256));
            safeBlocksToBrush
                .removeIf(pos -> !(mc.level.getBlockEntity(pos) instanceof BrushableBlockEntity) || isOutOfRange(pos, mc.player.blockPosition(), 256));

            for (BlockEntity be : Utils.blockEntities()) {
                if (suspiciousSandBlocks.contains(be.getBlockPos()) || suspiciousGravelBlocks.contains(be.getBlockPos()))
                    continue;
                if (be instanceof BrushableBlockEntity && !toIgnore.contains(be.getBlockPos().asLong())) {
                    if (mc.level.getBlockState(be.getBlockPos()).is(Blocks.SUSPICIOUS_GRAVEL)) {
                        suspiciousGravelBlocks.add(be.getBlockPos());
                    } else {
                        suspiciousSandBlocks.add(be.getBlockPos());
                    }

                    // Ignore Y value for distance checks
                    testPos.set(be.getBlockPos().getX(), lastFoundPos == null ? be.getBlockPos().getY() : lastFoundPos.getY(), be.getBlockPos().getZ());
                    if (lastFoundPos == null || isOutOfRange(lastFoundPos, testPos, 69)) {
                        lastFoundPos = be.getBlockPos();
                        if (StardustUtil.XAERO_AVAILABLE && waypoints.get()) switch (waypointsFor.get()) {
                            case SuspiciousSand -> {
                                if (mc.level.getBlockState(be.getBlockPos()).is(Blocks.SUSPICIOUS_SAND)) {
                                    MapUtil.addWaypoint(
                                        be.getBlockPos(), "Archaeology Dig Site", "ᛩ",
                                        MapUtil.Purpose.Normal, MapUtil.WpColor.Random, tempWaypoints.get()
                                    );
                                }
                            }
                            case SuspiciousGravel -> {
                                if (mc.level.getBlockState(be.getBlockPos()).is(Blocks.SUSPICIOUS_GRAVEL)) {
                                    MapUtil.addWaypoint(
                                        be.getBlockPos(), "Archaeology Dig Site", "ᛩ",
                                        MapUtil.Purpose.Normal, MapUtil.WpColor.Random, tempWaypoints.get()
                                    );
                                }
                            }
                            default -> MapUtil.addWaypoint(
                                be.getBlockPos(), "Archaeology Dig Site", "ᛩ",
                                MapUtil.Purpose.Normal, MapUtil.WpColor.Random, tempWaypoints.get()
                            );
                        }

                        if (soundPing.get()) switch (soundFor.get()) {
                            case SuspiciousSand -> {
                                if (mc.level.getBlockState(be.getBlockPos()).is(Blocks.SUSPICIOUS_SAND)) {
                                    long now = System.currentTimeMillis();
                                    if (now - lastPing >= 1337) {
                                        lastPing = now;
                                        mc.player.playSound(
                                            ThreadLocalRandom.current().nextInt(2) == 0 ? SoundEvents.BRUSH_SAND : SoundEvents.BRUSH_SAND_COMPLETED,
                                            pingVolume.get().floatValue(),
                                            ThreadLocalRandom.current().nextFloat(0.42f, 1.337f)
                                        );
                                    }
                                }
                            }
                            case SuspiciousGravel -> {
                                if (mc.level.getBlockState(be.getBlockPos()).is(Blocks.SUSPICIOUS_GRAVEL)) {
                                    long now = System.currentTimeMillis();
                                    if (now - lastPing >= 1337) {
                                        lastPing = now;
                                        mc.player.playSound(
                                            ThreadLocalRandom.current().nextInt(2) == 0 ? SoundEvents.BRUSH_GRAVEL : SoundEvents.BRUSH_GRAVEL_COMPLETED,
                                            pingVolume.get().floatValue(),
                                            ThreadLocalRandom.current().nextFloat(0.42f, 1.337f)
                                        );
                                    }
                                }
                            }
                            default -> {
                                long now = System.currentTimeMillis();
                                if (now - lastPing >= 1337) {
                                    lastPing = now;
                                    mc.player.playSound(
                                        mc.level.getBlockState(be.getBlockPos()).is(Blocks.SUSPICIOUS_SAND) ?
                                            ThreadLocalRandom.current().nextInt(2) == 0 ? SoundEvents.BRUSH_SAND : SoundEvents.BRUSH_SAND_COMPLETED :
                                            ThreadLocalRandom.current().nextInt(2) == 0 ? SoundEvents.BRUSH_GRAVEL : SoundEvents.BRUSH_GRAVEL_COMPLETED,
                                        pingVolume.get().floatValue(),
                                        ThreadLocalRandom.current().nextFloat(0.42f, 1.337f)
                                    );
                                }
                            }
                        }

                        if (chatNotify.get()) {
                            switch (chatFor.get()) {
                                case SuspiciousSand -> {
                                    if (mc.level.getBlockState(be.getBlockPos()).is(Blocks.SUSPICIOUS_GRAVEL)) continue;
                                }
                                case SuspiciousGravel -> {
                                    if (mc.level.getBlockState(be.getBlockPos()).is(Blocks.SUSPICIOUS_SAND)) continue;
                                }
                                default -> {
                                }
                            }
                            StringBuilder sb = new StringBuilder();

                            sb.append("Located an Archaeological Dig Site");
                            if (chatCoords.get()) {
                                sb.append(" at §8[§5").append(be.getBlockPos().getX()).append("§8, §5")
                                    .append(be.getBlockPos().getY()).append("§8, §5").append(be.getBlockPos().getZ()).append("§8]");
                            }
                            sb.append(StardustUtil.rCC()).append("..!");
                            MsgUtil.sendModuleMsg(sb.toString(), this.name);
                        }
                    }
                }
            }
        }

        HitResult target = mc.hitResult;
        if (!(target instanceof BlockHitResult blockHit)) return;

        BlockPos hitPos = blockHit.getBlockPos();
        if (mc.level.getBlockEntity(hitPos) instanceof BrushableBlockEntity) {
            if (toIgnore.contains(hitPos.asLong())) {
                mc.options.keyUse.setDown(false);

                if (breakBad.get()) {
                    FindItemResult result = InvUtils.findInHotbar(stack -> stack.getItem() instanceof ShovelItem);
                    if (result.found() && result.slot() != mc.player.getInventory().getSelectedSlot() && !(mc.player.getOffhandItem().getItem() instanceof ShovelItem)) {
                        InvUtils.swap(result.slot(), false);
                    }
                    if (isSafeToBreak(hitPos)) BlockUtils.breakBlock(hitPos, true);
                    else {
                        MsgUtil.updateModuleMsg("Preventing accidental breakage§c..!", this.name, "blockBreakPrevent".hashCode());
                    }
                }
                return;
            }

            FindItemResult result = InvUtils.findInHotbar(Items.BRUSH);
            if (result.found()) {
                boolean offHand = mc.player.getOffhandItem().is(Items.BRUSH);
                if (mc.player.getInventory().getSelectedSlot() == result.slot() || offHand) {
                    if (isSafeToBrush(hitPos)) {
                        if (!mc.player.isUsingItem() && mc.gameMode != null) {
                            mc.gameMode.useItem(mc.player, offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                        }
                        mc.options.keyUse.setDown(true);
                    }
                } else {
                    InvUtils.swap(result.slot(),false);
                    if (isSafeToBrush(hitPos)) {
                        if (!mc.player.isUsingItem() && mc.gameMode != null) {
                            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                        }
                        mc.options.keyUse.setDown(true);
                    }
                }
            } else {
                long now = System.currentTimeMillis();
                if (now - lastNotified >= 7777) {
                    lastNotified = now;
                    MsgUtil.sendModuleMsg("§4No brush found in hotbar§7..!", this.name);
                }
            }
        } else if (mc.player.getMainHandItem().is(Items.BRUSH) || mc.player.getOffhandItem().is(Items.BRUSH)) {
            mc.options.keyUse.setDown(false);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        HitResult target = mc.hitResult;
        if (!(target instanceof BlockHitResult blockHit)) return;

        BlockPos hitPos = blockHit.getBlockPos();
        if (toIgnore.contains(hitPos.asLong())) return;
        if (mc.level.getBlockEntity(hitPos) instanceof BrushableBlockEntity brushableBlock) {
            if (StardustUtil.XAERO_AVAILABLE && waypoints.get() && (suspiciousGravelBlocks.contains(hitPos) || suspiciousSandBlocks.contains(hitPos))) {
                MapUtil.removeWaypoints(
                    "Archaeology",
                    pos -> pos.closerThan(hitPos, 64),
                    Optional.of(hitPos.getY())
                );
            }

            ItemStack stack = brushableBlock.getItem();
            if (stack == ItemStack.EMPTY || stack.getItem() == Items.AIR) {
                preventingBreakageBlocks.add(hitPos);
                if (mc.player.isUsingItem()) ++ticksBrushing;

                if (ticksBrushing > 20) {
                    ticksBrushing = 0;
                    LogUtil.warn("Retrying brush packet after response timeout...", this.name);
                    mc.player.stopUsingItem();
                    mc.options.keyUse.setDown(false);
                }
                return;
            } else {
                ticksBrushing = 0;
            }

            if (!targetItems.get().contains(stack.getItem())) {
                toIgnore.add(hitPos.asLong());
                if (chat.get() && breakBad.get()) {
                    MsgUtil.sendModuleMsg("Breaking suspicious block containing §c" + stack.getHoverName().getString() + "§8.", this.name);
                }
                preventingBreakageBlocks.add(hitPos);
            } else {
                goodBlocks.add(hitPos);
                preventingBreakageBlocks.add(hitPos);
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get()) return;
        if (mc.player == null || mc.level == null) return;

        for (BlockPos pos : preventingBreakageBlocks) {
            if (toIgnore.contains(pos.asLong())) {
                RenderUtil.renderBlock(
                    event, pos,
                    new SettingColor(169, 13, 13, 255, true),
                    new SettingColor(169, 13, 13, 13, true),
                    ShapeMode.Both
                );
            } else if (goodBlocks.contains(pos)) {
                RenderUtil.renderBlock(
                    event, pos,
                    new SettingColor(13, 169, 69, 255, true),
                    new SettingColor(13, 169, 69, 13, true),
                    ShapeMode.Both
                );
            } else {
                RenderUtil.renderBlock(
                    event, pos,
                    new SettingColor(137, 169, 4, 255, true),
                    new SettingColor(137, 169, 4, 13, true),
                    ShapeMode.Both
                );
            }
        }

        ESPBlockData sand = sandESP.get();
        ESPBlockData gravel = gravelESP.get();
        for (BlockPos pos : suspiciousSandBlocks) {
            if (preventingBreakageBlocks.contains(pos)) continue;
            if (RenderUtil.shouldRenderBox(sand)) {
                int distance = pos.distManhattan(mc.player.blockPosition());
                if (distance <= 128) {
                    Color sideColor = new Color(sand.sideColor.r, sand.sideColor.g, sand.sideColor.b, Mth.clamp((int) Math.floor(sand.sideColor.a * ((128 - distance) * 0.333)), sand.sideColor.a, Math.max(sand.sideColor.a, 69)));
                    RenderUtil.renderBlock(event, pos, sand.lineColor, sideColor, sand.shapeMode);
                } else {
                    RenderUtil.renderBlock(event, pos, sand.lineColor, sand.sideColor, sand.shapeMode);
                }
            }
            if (RenderUtil.shouldRenderTracer(sand)) {
                RenderUtil.renderTracerTo(event, pos, sand.tracerColor);
            }
        }
        for (BlockPos pos : suspiciousGravelBlocks) {
            if (preventingBreakageBlocks.contains(pos)) continue;
            if (RenderUtil.shouldRenderBox(gravel)) {
                int distance = pos.distManhattan(mc.player.blockPosition());
                if (distance <= 128) {
                    Color sideColor = new Color(gravel.sideColor.r, gravel.sideColor.g, gravel.sideColor.b, Mth.clamp((int) Math.floor(gravel.sideColor.a * ((128 - distance) * 0.333)), gravel.sideColor.a, Math.max(gravel.sideColor.a, 69)));
                    RenderUtil.renderBlock(event, pos, gravel.lineColor, sideColor, gravel.shapeMode);
                } else {
                    RenderUtil.renderBlock(event, pos, gravel.lineColor, gravel.sideColor, gravel.shapeMode);
                }
            }
            if (RenderUtil.shouldRenderTracer(gravel)) {
                RenderUtil.renderTracerTo(event, pos, gravel.tracerColor);
            }
        }
    }

    public boolean isLootItem(Item item) {
        return ARCHAEOLOGY_LOOT_TABLE.contains(item);
    }
}
