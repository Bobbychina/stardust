package dev.stardust.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import dev.stardust.modules.LoreLocator;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(GuiGraphicsExtractor.class)
public abstract class DrawContextMixin {
    @Shadow
    public abstract void fill(int x1, int y1, int x2, int y2, int color);

    // See LoreLocator.java
    // 26.1: 必须写全描述符（item 有 3/4/5 参重载，裸名会被 Mixin 选错）；实体版是私有 6 参
    @Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At(value = "HEAD"))
    private void highlightNamedItems(LivingEntity entity, Level world, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        LoreLocator ll = modules.get(LoreLocator.class);
        if (!ll.isActive() || !ll.shouldHighlightSlot(stack)) return;
        this.fill(x, y, x + 16, y + 16, ll.color.get().getPacked());
    }

    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
    private void highlightNamedItemsNoEntity(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        LoreLocator ll = modules.get(LoreLocator.class);
        if (!ll.isActive() || !ll.shouldHighlightSlot(stack)) return;
        this.fill(x, y, x + 16, y + 16, ll.color.get().getPacked());
    }
}
