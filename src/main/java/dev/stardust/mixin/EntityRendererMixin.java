package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
import dev.stardust.util.TextUtil;
import net.minecraft.world.entity.Entity;
import dev.stardust.modules.AntiToS;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.renderer.entity.EntityRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    // See AntiToS.java
    // 26.1: 名字显示改为 EntityRenderer.getNameTag(T) 产出 Component；原来的 @ModifyVariable 挂在
    // 重载的 submitNameDisplay 上既歧义、又没有 Component 参数（26.1 的参数是 RenderState/PoseStack/
    // SubmitNodeCollector/CameraRenderState）→ 改在 getNameTag 的 RETURN 上改写返回值。
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void censorEntityName(Entity entity, CallbackInfoReturnable<Component> cir) {
        Component name = cir.getReturnValue();
        if (name == null) return;
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (antiToS == null) return;
        if (!antiToS.isActive()) return;

        if (!antiToS.containsBlacklistedText(name.getString())) return;
        cir.setReturnValue(TextUtil.modifyWithStyle(name.copy(), antiToS::censorText));
    }

    // See NoRenderMixin.java
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Player player)) return;

        Modules mods = Modules.get();
        if (mods == null) return;
        NoRender noRender = mods.get(NoRender.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (noRender == null) return;
        if (!noRender.isActive()) return;

        var codySetting = noRender.settings.get("cody");
        if (codySetting != null && (boolean) codySetting.get() && player.getGameProfile().name().equals("codysmile11")) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
