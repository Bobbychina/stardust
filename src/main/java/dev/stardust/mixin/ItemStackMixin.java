package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;
import dev.stardust.modules.AntiToS;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow
    public abstract Rarity getRarity();

    @Shadow
    public abstract Component getName();

    // See AntiToS.java
    @Inject(method = "getFormattedName", at = @At("HEAD"), cancellable = true)
    private void censorItemTooltip(CallbackInfoReturnable<Component> cir) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(this.getName().getString())) {
            cir.setReturnValue(Component.empty().append(antiToS.censorText(this.getName().getString()).formatted(this.getRarity().color())));
        }
    }

    @Inject(method = "toHoverableText", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;contains(Lnet/minecraft/core/component/DataComponentType;)Z"))
    private void censorHoveredText(CallbackInfoReturnable<Component> cir, @Local(ordinal = 0)LocalRef<MutableComponent> name) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(name.get().getString())) {
            name.set(Component.empty().append(antiToS.censorText(name.get().getString())));
        }
    }
}
