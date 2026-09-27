package dev.stardust.modules;

import java.util.List;
import java.util.ArrayDeque;
import dev.stardust.Stardust;
import net.minecraft.world.item.Item;
import net.minecraft.util.Tuple;
import dev.stardust.util.MsgUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import java.util.concurrent.ThreadLocalRandom;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.inventory.ContainerInput;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.inventory.GrindstoneMenu;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import dev.stardust.mixin.accessor.ClientConnectionAccessor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import dev.stardust.mixin.accessor.GrindstoneScreenHandlerAccessor;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Grinder extends Module {
    public Grinder() { super(Stardust.CATEGORY, "Grinder", "Automatically grinds enchantments off of select items in the grindstone."); }

    public enum ModuleMode {
        Packet, Interact
    }

    private final Setting<ModuleMode> moduleMode = settings.getDefaultGroup().add(
        new EnumSetting.Builder<ModuleMode>()
            .name("mode")
            .description("Packet is faster but might also get you kicked in some scenarios.")
            .defaultValue(ModuleMode.Packet)
            .build()
    );
    private final Setting<List<Item>> itemList = settings.getDefaultGroup().add(
        new ItemListSetting.Builder()
            .name("Items")
            .description("Items to automatically grind enchantments from.")
            .filter(item -> item.getDefaultInstance().isEnchantable())
            .build()
    );
    private final Setting<Boolean> grindNamed = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("grind-named-items")
            .description("Grind enchantments off of items which have a custom name applied to them.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> combine = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("combine-items")
            .description("Combines alike items in the grindstone in order to process them quicker. DESTROYS A PORTION OF THE INPUT ITEMS.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> muteGrindstone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("mute-grindstone")
            .description("Mutes the grindstone sounds.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> closeOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("close-grindstone")
            .description("Automatically close the grindstone screen when no more enchantments can be removed.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> disableOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("disable-on-done")
            .description("Automatically disable the module when no more enchantments can be removed.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> pingOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("sound-ping")
            .description("Play a sound cue when there are no more enchantments to remove.")
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
    private final Setting<Integer> tickRate = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("tick-rate")
            .description("Increase this if the server is kicking you.")
            .min(0).max(1000)
            .sliderRange(0, 100)
            .defaultValue(2)
            .visible(() -> moduleMode.get().equals(ModuleMode.Interact))
            .build()
    );

    private int timer = 0;
    private boolean notified = false;
    private ItemStack combinedItem = null;
    private ItemStack currentTarget = null;
    private final IntArrayList projectedEmpty = new IntArrayList();
    private final IntArrayList processedSlots = new IntArrayList();

    private boolean hasValidItems(GrindstoneMenu handler) {
        if (mc.player == null) return false;
        for (int n = 0; n < mc.player.getInventory().getNonEquipmentItems().size() + 3; n++) {
            if (n == 2) continue; // skip output slot
            if (isValidItem(handler.getSlot(n).getItem())) return true;
        }
        return false;
    }

    private boolean hasValidEnchantments(ItemStack stack) {
        if (!stack.isEnchanted()) return false;
        Object2IntMap<Holder<Enchantment>> enchants = new Object2IntArrayMap<>();

        Utils.getEnchantments(stack, enchants);
        if (enchants.size() == 1 && Utils.hasEnchantment(stack, Enchantments.BINDING_CURSE)) return false;
        else if (enchants.size() == 1 && Utils.hasEnchantment(stack, Enchantments.VANISHING_CURSE)) return false;
        else if (enchants.size() == 2 && Utils.hasEnchantment(stack, Enchantments.BINDING_CURSE) && Utils.hasEnchantment(stack, Enchantments.VANISHING_CURSE)) return false;

        return !enchants.isEmpty();
    }

    private boolean isValidItem(ItemStack item) {
        return itemList.get().contains(item.getItem())
            && hasValidEnchantments(item)
            && (grindNamed.get() || !item.has(DataComponents.CUSTOM_NAME));
    }

    private int predictEmptySlot(GrindstoneMenu handler) {
        if (mc.player == null) return -1;
        for (int n = mc.player.getInventory().getNonEquipmentItems().size() + 2; n >= 3; n--) {
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

    private void finished() {
        if (mc.player == null) return;
        if (!notified) {
            if (chatFeedback) MsgUtil.sendModuleMsg("No more enchantments to grind away§a..!", this.name);
            if (pingOnDone.get()) mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), ThreadLocalRandom.current().nextFloat(0.69f, 1.337f));
        }
        notified = true;
        processedSlots.clear();
        projectedEmpty.clear();
        if (closeOnDone.get()) mc.player.closeContainer();
        if (disableOnDone.get()) toggle();
    }

    /** 26.1: ServerboundContainerClickPacket 改为 (int,int,short,byte,ContainerInput,Int2ObjectMap<HashedStack>,HashedStack)，
     *  changedSlots / carried 都要 HashedStack；哈希生成器取自 ClientPacketListener.decoratedHashOpsGenenerator()。 */
    private ServerboundContainerClickPacket buildClickPacket(int containerId, int stateId, int slot, Int2ObjectMap<ItemStack> changedSlots) {
        net.minecraft.network.HashedPatchMap.HashGenerator hasher = mc.getConnection().decoratedHashOpsGenenerator();
        Int2ObjectMap<net.minecraft.network.HashedStack> hashed = new Int2ObjectOpenHashMap<>();
        changedSlots.forEach((k, v) -> hashed.put(k, net.minecraft.network.HashedStack.create(v, hasher)));
        return new ServerboundContainerClickPacket(
            containerId, stateId, (short) slot, (byte) 0,
            ContainerInput.QUICK_MOVE, hashed, net.minecraft.network.HashedStack.create(ItemStack.EMPTY, hasher)
        );
    }

    private ServerboundContainerClickPacket generatePacket(GrindstoneMenu handler) {
        if (mc.player == null) return null;
        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();

        if (currentTarget != null && combinedItem != null) {
            // empty output
            changedSlots.put(0, ItemStack.EMPTY);
            changedSlots.put(1, ItemStack.EMPTY);
            changedSlots.put(2, ItemStack.EMPTY);
            int shiftClickTargetSlot = predictEmptySlot(handler);

            if (shiftClickTargetSlot == -1) {
                MsgUtil.sendModuleMsg("Failed to predict empty target slot§c..!", this.name);
                return null;
            }

            if (combinedItem.isEmpty()) {
                combinedItem = ((GrindstoneScreenHandlerAccessor) handler).invokeGrind(currentTarget);
            }
            changedSlots.put(shiftClickTargetSlot, combinedItem.copy());

            combinedItem = null;
            currentTarget = null;
            return buildClickPacket(handler.containerId, handler.getStateId(), 2, changedSlots);
        } else if (currentTarget != null) {
            // fill input slot 2
            for (int n = 3; n < mc.player.getInventory().getNonEquipmentItems().size() + 3; n++) {
                if (processedSlots.contains(n)) continue;
                ItemStack stack = handler.getSlot(n).getItem();
                if (!isValidItem(stack) || !stack.is(currentTarget.getItem())) continue;
                Tuple<ItemStack, Integer> combinedStackPlusDamage = combineStacks(handler, currentTarget, stack);

                combinedItem = combinedStackPlusDamage.getA();

                processedSlots.add(1);
                processedSlots.add(n);
                projectedEmpty.add(n);
                changedSlots.put(1, stack);
                changedSlots.put(n, ItemStack.EMPTY);
                changedSlots.put(2, ((GrindstoneScreenHandlerAccessor) handler).invokeGrind(combinedItem));

                return buildClickPacket(handler.containerId, handler.getStateId(), n, changedSlots);
            }
            combinedItem = ItemStack.EMPTY;
            return generatePacket(handler);
        } else {
            // fill input slot 1
            for (int n = 3; n < mc.player.getInventory().getNonEquipmentItems().size() + 3; n++) {
                if (processedSlots.contains(n)) continue;
                ItemStack stack = handler.getSlot(n).getItem();
                if (!isValidItem(stack)) continue;

                currentTarget = stack;
                processedSlots.add(0);
                processedSlots.add(n);
                projectedEmpty.add(n);
                changedSlots.put(0, stack);
                changedSlots.put(n, ItemStack.EMPTY);
                changedSlots.put(2, ((GrindstoneScreenHandlerAccessor) handler).invokeGrind(stack));

                if (!combine.get()) combinedItem = ItemStack.EMPTY;

                return buildClickPacket(handler.containerId, handler.getStateId(), n, changedSlots);
            }
        }

        return null;
    }

    private Tuple<ItemStack, Integer> combineStacks(GrindstoneMenu handler, ItemStack stack1, ItemStack stack2) {
        if (!stack1.is(stack2.getItem())) return new Tuple<>(ItemStack.EMPTY, 0);

        int j = stack1.getMaxDamage() - stack1.getDamageValue();
        int k = stack1.getMaxDamage() - stack2.getDamageValue();
        int l = j + k + stack1.getMaxDamage() * 5 / 100;
        int m = Math.max(stack1.getMaxDamage() - l, 0);

        ((GrindstoneScreenHandlerAccessor) handler).invokeTransferEnchantments(stack1, stack2);

        return new Tuple<>(stack1, m);
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        notified = false;
        combinedItem = null;
        currentTarget = null;
        projectedEmpty.clear();
        processedSlots.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (mc.screen == null) {
            notified = false;
            return;
        }
        if (!(mc.screen instanceof GrindstoneScreen)) return;
        if (!(mc.player.containerMenu instanceof GrindstoneMenu grindstone)) return;

        switch (moduleMode.get()) {
            case Packet -> {
                if (mc.getConnection() == null || notified) return;
                ArrayDeque<ServerboundContainerClickPacket> packets = new ArrayDeque<>();

                boolean exhausted = false;
                while (!exhausted) {
                    ServerboundContainerClickPacket packet = generatePacket(grindstone);

                    if (packet == null) {
                        exhausted = true;
                    } else {
                        packets.addLast(packet);
                    }
                }

                while (!packets.isEmpty()) {
                    ((ClientConnectionAccessor) mc.getConnection()
                        .getConnection())
                        .invokeSendImmediately(packets.removeFirst(), null, true);
                }
                finished();
            }
            case Interact -> {
                if (timer >= tickRate.get()) {
                    timer = 0;
                } else {
                    ++timer;
                    return;
                }

                ItemStack input1 = grindstone.getSlot(GrindstoneMenu.INPUT_SLOT).getItem();
                ItemStack input2 = grindstone.getSlot(GrindstoneMenu.ADDITIONAL_SLOT).getItem();
                ItemStack output = grindstone.getSlot(GrindstoneMenu.RESULT_SLOT).getItem();

                if (!hasValidItems(grindstone)) finished();
                else if (input1.isEmpty() && input2.isEmpty()) {
                    Item turboItem = null;
                    for (int n = 3; n < mc.player.getInventory().getNonEquipmentItems().size() + 3; n++) {
                        ItemStack stack = grindstone.getSlot(n).getItem();
                        if (!hasValidEnchantments(stack)) continue;
                        else if (!itemList.get().contains(stack.getItem())) continue;
                        else if (stack.has(DataComponents.CUSTOM_NAME) && !grindNamed.get()) continue;
                        if (combine.get() && turboItem != null && stack.getItem() != turboItem) continue;

                        if (!combine.get()) {
                            InvUtils.shiftClick().slotId(n);
                            return;
                        } else if (turboItem != null) {
                            InvUtils.shiftClick().slotId(n);
                            return;
                        } else {
                            InvUtils.shiftClick().slotId(n);
                            turboItem = stack.getItem();
                        }
                    }
                    if (!combine.get()) finished();
                } else if (!output.isEmpty() && (itemList.get().contains(input1.getItem()) || itemList.get().contains(input2.getItem()))) {
                    if (!input1.isEmpty()) {
                        if (!input1.has(DataComponents.CUSTOM_NAME)|| grindNamed.get()) {
                            InvUtils.shiftClick().slotId(GrindstoneMenu.RESULT_SLOT);
                        }
                    } else if (!input2.isEmpty()) {
                        if (!input2.has(DataComponents.CUSTOM_NAME) || grindNamed.get()) {
                            InvUtils.shiftClick().slotId(GrindstoneMenu.RESULT_SLOT);
                        }
                    }
                } else if (!input1.isEmpty() && !input2.isEmpty()) {
                    InvUtils.shiftClick().slotId(GrindstoneMenu.INPUT_SLOT);
                    InvUtils.shiftClick().slotId(GrindstoneMenu.ADDITIONAL_SLOT);
                }
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!muteGrindstone.get() || !(event.packet instanceof ClientboundSoundPacket packet)) return;
        if (packet.getSound().value().equals(SoundEvents.GRINDSTONE_USE)) event.cancel();
    }
}
