package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import dev.stardust.modules.StashBrander;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {
    // 26.1: AnvilScreen 字段 nameField → name
    @Shadow
    private EditBox name;

    public AnvilScreenMixin(AnvilMenu handler, Inventory playerInventory, Component title, Identifier texture) {
        super(handler, playerInventory, title, texture);
    }

    /**
     * See StashBrander.java
     * Helps to minimize packet spam by drastically reducing the amount of RenameItemC2SPackets that are sent.
     * */
    @Inject(method = "slotChanged", at = @At("HEAD"), cancellable = true)
    private void maybeCancelNameFieldUpdate(AbstractContainerMenu handler, int slotId, ItemStack stack, CallbackInfo ci) {
        Modules mods = Modules.get();
        if (mods == null) return;
        StashBrander sb = mods.get(StashBrander.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (sb == null) return;

        if (slotId == 0 && sb.isActive()) {
            ci.cancel();
            this.name.setEditable(true);
            this.setFocused(this.name);
        }
    }
}
