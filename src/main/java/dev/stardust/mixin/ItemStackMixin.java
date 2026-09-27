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
    public abstract Component getHoverName();

    // See AntiToS.java
    // 26.1: getFormattedName/toHoverableText → getStyledHoverName；不能挂在 getHoverName 上（handler 内部会再调 getHoverName，递归爆栈）
    @Inject(method = "getStyledHoverName", at = @At("HEAD"), cancellable = true)
    private void censorItemTooltip(CallbackInfoReturnable<Component> cir) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(this.getHoverName().getString())) {
            cir.setReturnValue(Component.empty().append(antiToS.censorText(this.getHoverName().getString()).formatted(this.getRarity().color())));
        }
    }

    // 26.1: ItemStack.contains(...) → has(...)；挂错方法名会 "Scanned 0 target(s)" 直接崩在 Bootstrap 阶段
    @Inject(method = "getStyledHoverName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z"))
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
