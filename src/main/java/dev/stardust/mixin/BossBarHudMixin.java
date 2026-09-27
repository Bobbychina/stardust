package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
import dev.stardust.modules.AntiToS;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(BossHealthOverlay.class)
public class BossBarHudMixin {

    // See AntiToS.java
    // 26.1: extractBar 有 2 个重载，裸名会歧义 -> 写全描述符
    @Inject(method = "extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At("HEAD"))
    private void censorBossBar(GuiGraphicsExtractor context, int x, int y, BossEvent bossBar, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (antiToS == null) return;
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(bossBar.getName().getString())) {
            bossBar.setName(Component.literal(antiToS.censorText(bossBar.getName().getString()).formatted(bossBar.getName().getStyle())));
        }
    }
}
