package dev.stardust.mixin;

import dev.stardust.modules.RapidFire;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {

    // See RapidFire.java
    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void preventCrossbowUseReset(CallbackInfo ci) {
        Modules mods = Modules.get();
        if (mods == null) return;
        RapidFire rf = mods.get(RapidFire.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (rf == null) return;
        if (!rf.isActive() || !rf.charging) return;
        ci.cancel();
    }
}
