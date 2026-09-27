package dev.stardust.modules;

import java.util.List;
import java.util.ArrayDeque;
import dev.stardust.Stardust;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import dev.stardust.util.MsgUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import dev.stardust.util.StonecutterUtil;
import net.minecraft.network.protocol.Packet;
import meteordevelopment.orbit.EventHandler;
import java.util.concurrent.ThreadLocalRandom;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.inventory.ContainerInput;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.util.context.ContextMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.SelectableRecipe;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import dev.stardust.mixin.accessor.ClientConnectionAccessor;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class AutoMason extends Module {
    public AutoMason() { super(Stardust.CATEGORY, "AutoMason", "Automates masonry interactions with the stonecutter."); }

    public enum Mode {
        Packet, Interact
    }

    private final Setting<Mode> moduleMode = settings.getDefaultGroup().add(
        new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Packet is faster but might also get you kicked in some scenarios.")
            .defaultValue(Mode.Packet)
            .build()
    );
    private final Setting<Integer> batchDelay = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("packet-delay")
            .description("Increase this if the server is kicking you.")
            .min(0).max(1000)
            .sliderRange(0, 50)
            .defaultValue(1)
            .visible(() -> moduleMode.get().equals(Mode.Packet))
            .build()
    );
    private final Setting<Integer> tickRate = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("tick-rate")
            .description("Increase this if the server is kicking you.")
            .min(0).max(1000)
            .sliderRange(0, 100)
            .defaultValue(4)
            .visible(() -> moduleMode.get().equals(Mode.Interact))
            .build()
    );

    private final Setting<List<Item>> itemList = settings.getDefaultGroup().add(
        new ItemListSetting.Builder()
            .name("target-items")
            .description("Which target items you wish to craft in the Stonecutter.")
            .filter(item -> StonecutterUtil.STONECUTTER_BLOCKS.values().stream().anyMatch(v -> v.contains(item)))
            .build()
    );
    private final Setting<Boolean> muteCutter = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("mute-stonecutter")
            .description("Mute the stonecutter sounds.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> closeOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("close-stonecutter")
            .description("Automatically close the stonecutter screen when no more blocks can be crafted.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> disableOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("disable-when-done")
            .description("Automatically disable the module when no more blocks can be crafted.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> pingOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("sound-ping")
            .description("Play a sound cue when there are no more blocks to be crafted.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Double> pingVolume = settings.getDefaultGroup().add(
        new DoubleSetting.Builder()
            .name("ping-volume")
            .sliderMin(0.0)
            .sliderMax(5.0)
            .defaultValue(0.5)
            .visible(pingOnDone::get)
            .build()
    );

    private int timer = 0;
    private boolean notified = false;
    private ItemStack targetStack = null;
    private ItemStack outputStack = null;
    private final IntArrayList projectedEmpty = new IntArrayList();
    private final IntArrayList processedSlots = new IntArrayList();
    private final ArrayDeque<Packet<?>> packetQueue = new ArrayDeque<>();

    @Override
    public void onDeactivate() {
        timer = 0;
        notified = false;
        targetStack = null;
        outputStack = null;
        packetQueue.clear();
        processedSlots.clear();
        projectedEmpty.clear();
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof StonecutterScreen) {
            notified = false;
        }
    }

    @EventHandler
    private void onSoundPlay(PlaySoundEvent event) {
        if (!muteCutter.get()) return;
        if (event.sound.getIdentifier().equals(SoundEvents.UI_STONECUTTER_TAKE_RESULT.location())) {
            event.cancel();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.getConnection() == null) return;
        if (mc.player == null || mc.level == null) return;
        if (!(mc.player.containerMenu instanceof StonecutterMenu cutter)) return;

        if (!packetQueue.isEmpty()) {
            if (batchDelay.get() <= 0) {
                while (!packetQueue.isEmpty()) {
                    ((ClientConnectionAccessor) mc.getConnection().getConnection()).invokeSendImmediately(
                        packetQueue.removeFirst(), null, true
                    );
                }
            } else {
                ++timer;
                if (timer >= batchDelay.get()) {
                    timer = 0;
                    ((ClientConnectionAccessor) mc.getConnection().getConnection()).invokeSendImmediately(
                        packetQueue.removeFirst(), null, true
                    );
                }
            }

            if (packetQueue.isEmpty()) finished();
            return;
        }

        switch (moduleMode.get()) {
            case Packet -> {
                if (notified) return;
                if (itemList.get().isEmpty()) {
                    notified = true;
                    MsgUtil.sendModuleMsg("No target items selected§c..!", this.name);
                    finished();
                    return;
                }

                boolean exhausted = false;
                while (!exhausted) {
                    Packet<?> packet = generatePacket(cutter);

                    if (packet == null) {
                        exhausted = true;
                    } else packetQueue.addLast(packet);
                }
                if (packetQueue.isEmpty()) finished();
            }
            case Interact -> {
                if (timer >= tickRate.get()) {
                    timer = 0;
                } else {
                    ++timer;
                    return;
                }

                if (itemList.get().isEmpty()) {
                    if (!notified) {
                        MsgUtil.sendModuleMsg("No target items selected§c..!", this.name);
                        if (pingOnDone.get()) {
                            mc.player.playSound(
                                SoundEvents.EXPERIENCE_ORB_PICKUP,
                                pingVolume.get().floatValue(),
                                ThreadLocalRandom.current().nextFloat(0.69f, 1.337f)
                            );
                        }
                    }
                    notified = true;

                    finished();
                    return;
                }

                ItemStack input = cutter.getSlot(StonecutterMenu.INPUT_SLOT).getItem();
                ItemStack output = cutter.getSlot(StonecutterMenu.RESULT_SLOT).getItem();

                if (!hasValidItems(cutter)) finished();
                else if (input.isEmpty() && output.isEmpty()) {
                    for (int n = 2; n < mc.player.getInventory().getNonEquipmentItems().size() + 2; n++) {
                        ItemStack stack = cutter.getSlot(n).getItem();

                        if (!isValidItem(stack)) continue;
                        InvUtils.shiftClick().slotId(n);
                    }
                } else if (output.isEmpty()) {
                    SelectableRecipe.SingleInputSet<StonecutterRecipe> available = mc.level
                        .recipeAccess().stonecutterRecipes().filter(input);
                    ContextMap contextParameterMap = SlotDisplayContext.fromLevel(mc.level);

                    boolean found = false;
                    for (int n = 0; n < available.entries().size(); n++) {
                        SelectableRecipe.EntryGroup<StonecutterRecipe> entry = available.entries().get(n);
                        ItemStack recipeStack = entry.recipe().optionDisplay().getFirst(contextParameterMap);

                        if (recipeStack.isEmpty()) continue;
                        if (itemList.get().contains(recipeStack.getItem())) {
                            found = true;
                            cutter.clickMenuButton(mc.player, n);
                            ((ClientConnectionAccessor) mc.getConnection().getConnection()).invokeSendImmediately(
                                new ServerboundContainerButtonClickPacket(cutter.containerId, n), null, true
                            );
                            break;
                        }
                    }

                    if (!found) {
                        if (!notified) {
                            notified = true;
                            MsgUtil.sendModuleMsg("Desired recipe not found§c..!", this.name);
                            if (pingOnDone.get()) mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), ThreadLocalRandom.current().nextFloat(0.69f, 1.337f));
                        }
                        finished();
                    }
                } else {
                    InvUtils.shiftClick().slotId(StonecutterMenu.RESULT_SLOT);
                }
            }
        }
    }

    private void finished() {
        if (mc.player == null) return;
        if (!notified) {
            if (chatFeedback) MsgUtil.sendModuleMsg("No more items to craft§a..!", this.name);
            if (pingOnDone.get()) mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), ThreadLocalRandom.current().nextFloat(0.69f, 1.337f));
        }
        notified = true;
        processedSlots.clear();
        projectedEmpty.clear();
        if (closeOnDone.get()) mc.player.closeContainer();
        if (disableOnDone.get()) toggle();
    }

    private Packet<?> generatePacket(StonecutterMenu handler) {
        if (mc.player == null || mc.level == null) return null;
        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();

        if (targetStack != null && outputStack != null) {
            // take output
            changedSlots.put(0, ItemStack.EMPTY);
            changedSlots.put(1, ItemStack.EMPTY);
            int shiftClickTargetSlot = predictEmptySlot(handler);

            if (shiftClickTargetSlot == -1) {
                MsgUtil.sendModuleMsg("Failed to predict empty target slot §8[§7Is your inventory full§3..?§8]§c..!", this.name);
                return null;
            }
            changedSlots.put(shiftClickTargetSlot, new ItemStack(outputStack.getItem(), targetStack.getCount()));

            targetStack = null;
            outputStack = null;
            return new ServerboundContainerClickPacket(
                handler.containerId, handler.getStateId(), 1, 0,
                ContainerInput.QUICK_MOVE, ItemStack.EMPTY, changedSlots
            );
        } else if (targetStack != null) {
            // pick recipe
            SelectableRecipe.SingleInputSet<StonecutterRecipe> available = mc.level
                .recipeAccess().stonecutterRecipes().filter(targetStack);
            ContextMap contextParameterMap = SlotDisplayContext.fromLevel(mc.level);

            for (int n = 0; n < available.entries().size(); n++) {
                var entry = available.entries().get(n);
                ItemStack recipeStack = entry.recipe().optionDisplay().getFirst(contextParameterMap);

                if (recipeStack.isEmpty()) continue;
                if (itemList.get().contains(recipeStack.getItem())) {
                    outputStack = recipeStack;
                    return new ServerboundContainerButtonClickPacket(handler.containerId, n);
                }
            }
        } else {
            // fill input slot
            for (int n = 2; n < mc.player.getInventory().getNonEquipmentItems().size() + 2; n++) {
                if (processedSlots.contains(n)) continue;
                ItemStack stack = handler.getSlot(n).getItem();
                if (!isValidItem(stack)) continue;

                targetStack = stack;
                processedSlots.add(0);
                processedSlots.add(n);
                projectedEmpty.add(n);
                changedSlots.put(0, stack);
                changedSlots.put(n, ItemStack.EMPTY);

                return new ServerboundContainerClickPacket(
                    handler.containerId, handler.getStateId(), n, 0,
                    ContainerInput.QUICK_MOVE, ItemStack.EMPTY, changedSlots
                );
            }
        }

        return null;
    }

    private int predictEmptySlot(StonecutterMenu handler) {
        if (mc.player == null) return -1;
        for (int n = mc.player.getInventory().getNonEquipmentItems().size() + 1; n >= 2; n--) {
            if (processedSlots.contains(n) && !projectedEmpty.contains(n)) continue;
            if (projectedEmpty.contains(n)) {
                projectedEmpty.rem(n);
                return n;
            } else if (handler.getSlot(n).getItem().isEmpty()) {
                processedSlots.add(n);
                return n;
            }
        }
        return -1;
    }

    private boolean hasValidItems(StonecutterMenu handler) {
        if (mc.player == null) return false;
        for (int n = 0; n < mc.player.getInventory().getNonEquipmentItems().size() + 2; n++) {
            if (n == 1) continue; // skip output slot
            if (isValidItem(handler.getSlot(n).getItem())) return true;
        }
        return false;
    }

    private boolean isValidItem(ItemStack stack) {
        if (itemList.get().isEmpty()) return false;
        if (stack.isEmpty() || stack.is(Items.AIR)) return false;
        if (!StonecutterUtil.STONECUTTER_BLOCKS.containsKey(stack.getItem())) return false;

        return StonecutterUtil
            .STONECUTTER_BLOCKS
            .get(stack.getItem())
            .stream().anyMatch(item -> itemList.get().contains(item));
    }
}
