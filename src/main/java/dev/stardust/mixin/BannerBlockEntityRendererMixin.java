package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.blockentity.BannerRenderer;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(BannerRenderer.class)
public class BannerBlockEntityRendererMixin {

    // See NoRenderMixin.java
    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/BannerBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"), cancellable = true)
    private void onRender(BannerBlockEntity bannerBlockEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, CallbackInfo ci) {
        if (bannerBlockEntity.getLevel() != null) {
            Modules mods = Modules.get();
            if (mods == null) return;
            NoRender noRender = mods.get(NoRender.class);
            if (!noRender.isActive()) return;

            Component bannerName = bannerBlockEntity.getCustomName();
            var bannerSetting = noRender.settings.get("cody-banners");

            if (bannerSetting == null || bannerName == null) return;
            if ((boolean) bannerSetting.get() && bannerName.getString().contains("codysmile11")) {
                ci.cancel();
            }
        }
    }
}
