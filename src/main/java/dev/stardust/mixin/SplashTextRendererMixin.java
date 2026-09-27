package dev.stardust.mixin;

import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(SplashRenderer.class)
public class SplashTextRendererMixin {
    @Unique private int trackAlpha = 0;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    // 26.1: SplashRenderer.extractRenderState 的第 4 参是 float alpha（旧 Yarn 是 int）
    private void mixinRender(GuiGraphicsExtractor context, int width, Font textRenderer, float alpha, CallbackInfo ci) {
        this.trackAlpha = (int) alpha;
    }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 4)
    private int modifyRenderArg(int color) {
        return StardustConfig.greenSplashTextSetting.get() ? 0x54FB54 | this.trackAlpha : color;
    }
}
