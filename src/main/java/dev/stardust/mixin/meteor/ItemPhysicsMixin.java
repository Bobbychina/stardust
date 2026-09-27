package dev.stardust.mixin.meteor;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.injection.Inject;
import dev.stardust.mixininterface.IItemEntityMixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.render.ItemPhysics;
import meteordevelopment.meteorclient.events.render.RenderItemEntityEvent;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Fixes a rare NPE with Item Physics, and adds item model tumbling to the random rotation feature.
 *     See also: ItemEntityMixin.java
 **/
@Mixin(value = ItemPhysics.class, remap = false)
public abstract class ItemPhysicsMixin {
    @Inject(method = "offsetInWater", at = @At("HEAD"), cancellable = true)
    private void fixCrashNPE(PoseStack matrices, ItemEntity entity, CallbackInfo ci) {
        if (entity == null)
            ci.cancel();
    }

    @Inject(
        method = "onRenderItemEntity",
        at = @At(
            value = "INVOKE",
            // 26.1: PoseStack#multiply(Quaternionf) -> mulPose(Quaternionfc)
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V",
            ordinal = 1,
            shift = At.Shift.AFTER
        ),
        // remap must be true to properly target an invocation of a remapped class/method like PoseStack#mulPose (I think)
        remap = true
    )
    private void addItemTumble(RenderItemEntityEvent event, CallbackInfo ci) {
        ItemEntity entity = event.itemEntity;

        if (entity == null) return;
        if (!(entity instanceof IItemEntityMixin itemTumble)) return;

        float tickDelta = event.tickDelta;
        event.matrixStack.mulPose(itemTumble.stardust$getRenderQuaternion(tickDelta));
    }
}
