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
    @Shadow
    private EditBox nameField;

    public AnvilScreenMixin(AnvilMenu handler, Inventory playerInventory, Component title, Identifier texture) {
        super(handler, playerInventory, title, texture);
    }

    /**
     * See StashBrander.java
     * Helps to minimize packet spam by drastically reducing the amount of RenameItemC2SPackets that are sent.
     * */
    @Inject(method = "onSlotUpdate", at = @At("HEAD"), cancellable = true)
    private void maybeCancelNameFieldUpdate(AbstractContainerMenu handler, int slotId, ItemStack stack, CallbackInfo ci) {
        Modules mods = Modules.get();
        if (mods == null) return;
        StashBrander sb = mods.get(StashBrander.class);

        if (slotId == 0 && sb.isActive()) {
            ci.cancel();
            this.nameField.setEditable(true);
            this.setFocused(this.nameField);
        }
    }
}
