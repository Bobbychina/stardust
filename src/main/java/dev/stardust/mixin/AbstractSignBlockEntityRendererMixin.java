package dev.stardust.mixin;

import java.util.Arrays;
import net.minecraft.network.chat.Component;
import java.util.stream.Collectors;
import dev.stardust.modules.AntiToS;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.injection.At;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignBlockEntityRendererMixin implements BlockEntityRenderer<SignBlockEntity, SignRenderState> {

    // See AntiToS.java
    @ModifyVariable(method = "submitSignText", at = @At("HEAD"), argsOnly = true)
    private SignText modifyRenderedText(SignText signText) {
        Modules modules = Modules.get();
        if (modules == null ) return signText;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (antiToS == null) return signText;
        if (!antiToS.isActive()) return signText;

        String testText = Arrays.stream(signText.getMessages(false))
            .map(Component::getString)
            .collect(Collectors.joining(" "))
            .trim();
        return antiToS.containsBlacklistedText(testText) ? antiToS.familyFriendlySignText(signText) : signText;
    }

    // See NoRenderMixin.java
    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRender(SignBlockEntity signBlockEntity, float f, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, int j, CallbackInfo ci) {
        Modules mods = Modules.get();
        if (mods == null) return;
        AntiToS antiToS = mods.get(AntiToS.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (antiToS == null) return;
        NoRender noRender = mods.get(NoRender.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (noRender == null) return;
        var signSetting = noRender.settings.get("cody-signs");
        if (signSetting == null) return;
        if (noRender.isActive() && (boolean) signSetting.get() && isCodySign(signBlockEntity)) {
            ci.cancel();
        }

        if (antiToS.isActive() && antiToS.signMode.get().equals(AntiToS.SignMode.NoRender)) {
            if (antiToS.containsBlacklistedText(Arrays.stream(signBlockEntity.getFrontText().getMessages(false)).map(Component::getString).collect(Collectors.joining()))) {
                ci.cancel();
            }
        }
    }

    @Unique
    private boolean isCodySign(SignBlockEntity sbe) {
        SignText frontText = sbe.getFrontText();
        return Arrays.stream(frontText.getMessages(false)).anyMatch(msg -> msg.getString().contains("codysmile11") || msg.getString().toLowerCase().contains("has been here :)"));
    }
}
