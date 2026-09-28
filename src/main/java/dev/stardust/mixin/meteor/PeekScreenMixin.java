package dev.stardust.mixin.meteor;

import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;
import dev.stardust.util.MsgUtil;
import dev.stardust.util.LogUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.item.equipment.Equippable;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.PeekScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Allows you to preview items from the peek screen by giving you a client-side ghost item when you left-click.
 **/
@Mixin(value = PeekScreen.class, remap = false)
public abstract class PeekScreenMixin extends ShulkerBoxScreen {
    public PeekScreenMixin(ShulkerBoxMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Unique
    private BetterTooltips btt = null;

    // See BetterTooltipsMixin.java
    // 26.1: 鼠标回调改为 mouseClicked(MouseButtonEvent, boolean)，按钮号走 event.button()
    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true, remap = true)
    private void hijackMouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (mc.player == null) return;
        if (btt == null) {
            Modules mods = Modules.get();
            if (mods == null) return;
            btt = mods.get(BetterTooltips.class);

            if (btt == null) return;
        }
        if (!btt.isActive()) return;
        var setting = btt.settings.get("peek-ghost-items");
        if (setting == null) return;
        try {
            // 26.1: AbstractContainerScreen 的 Yarn focusedSlot -> 官方 hoveredSlot
            if ((boolean) setting.get() && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && hoveredSlot != null && !hoveredSlot.getItem().isEmpty()) {
                FindItemResult empty;
                if (InvUtils.testInMainHand(ItemStack::isEmpty)) {
                    empty = new FindItemResult(mc.player.getInventory().getSelectedSlot(), mc.player.getMainHandItem().getCount());
                } else {
                    empty = InvUtils.find(ItemStack::isEmpty, 0, 8);
                }

                if (empty.found()) {
                    ItemStack stack = hoveredSlot.getItem();

                    // Skull-block items aren't swappable by default,
                    // causing the ghost item to disappear without this.
                    // I don't distinguish the item type here, allowing you to put any ghost-item on your head.
                    // 26.1: Equippable.Builder 方法全部加了 set 前缀 (swappable -> setSwappable 等)
                    Equippable equippableComponent = Equippable.builder(EquipmentSlot.HEAD)
                        .setSwappable(true)
                        .setAllowedEntities(EntityType.PLAYER)
                        .setDispensable(true)
                        .build();
                    if (stardust$shouldSetComponent(stack))
                        stack.set(DataComponents.EQUIPPABLE, equippableComponent);

                    mc.player.getInventory().setItem(empty.slot(), stack);
                    cir.setReturnValue(true);
                } else {
                    MsgUtil.sendModuleMsg("Peeking at ghost items requires an empty hotbar slot§c..!", "better-tooltips");
                    cir.setReturnValue(false);
                }
            }
        } catch (Exception err) {
            LogUtil.error(err.toString(), "PeekScreenMixin");
        }
    }

    @Unique
    private boolean stardust$shouldSetComponent(ItemStack stack) {
        return (!stack.has(DataComponents.EQUIPPABLE)
            || !stack.get(DataComponents.EQUIPPABLE).swappable());
    }
}
