package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
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

    // 26.1: SplashRenderer.extractRenderState 的第 4 参是 float alpha（旧 Yarn 是 int）
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/client/gui/Font;F)V", at = @At("HEAD"))
    private void mixinRender(GuiGraphicsExtractor context, int width, Font textRenderer, float alpha, CallbackInfo ci) {
        this.trackAlpha = (int) alpha;
    }

    // 26.1: 渲染改走 ActiveTextCollector.accept(TextAlignment,int,int,Parameters,Component)，**不再有颜色 int 参数**
    // （原实现是改 drawCenteredTextWithShadow 的第 5 个 arg）→ 改成改写传入的 Component 样式，效果等价于「绿色 splash 文本」。
    // TODO(26.1): Style 的颜色不含 alpha，原实现里叠加的淡出 alpha（0x54FB54 | trackAlpha）无法保留，仅影响淡出过渡的透明度。
    @ModifyArg(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/client/gui/Font;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/network/chat/Component;)V"),
        index = 4
    )
    private Component modifySplashComponent(Component splash) {
        if (!StardustConfig.greenSplashTextSetting.get()) return splash;
        return splash.copy().withStyle(s -> s.withColor(0x54FB54));
    }
}
