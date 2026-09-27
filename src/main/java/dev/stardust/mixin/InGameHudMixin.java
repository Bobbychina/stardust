package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
import dev.stardust.modules.AntiToS;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(Gui.class)
public class InGameHudMixin {
    @Shadow
    private ItemStack lastToolHighlight;

    // See AntiToS.java
    @Inject(
        method = "extractSelectedItemName",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z")
    )
    private void censorItemTooltip(GuiGraphicsExtractor context, CallbackInfo ci, @Local LocalRef<MutableComponent> itemName) {
        if (this.lastToolHighlight.isEmpty()) return;

        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(itemName.get().getString())) {
            itemName.set(Component.empty().append(antiToS.censorText(itemName.get().getString())).withStyle(this.lastToolHighlight.getRarity().color()));
        }
    }
}
