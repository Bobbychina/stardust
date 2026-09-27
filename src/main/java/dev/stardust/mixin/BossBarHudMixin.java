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
    @Inject(method = "renderBossBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At("HEAD"))
    private void censorBossBar(GuiGraphicsExtractor context, int x, int y, BossEvent bossBar, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(bossBar.getName().getString())) {
            bossBar.setName(Component.literal(antiToS.censorText(bossBar.getName().getString()).formatted(bossBar.getName().getStyle())));
        }
    }
}
