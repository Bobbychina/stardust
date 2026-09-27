package dev.stardust.mixin;

import dev.stardust.modules.RocketMan;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.player.LocalPlayer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {

    // See RocketMan.java
    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void mixinPlaySound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        RocketMan rocketMan = modules.get(RocketMan.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (rocketMan == null) return;
        if (rocketMan.isActive() && sound == SoundEvents.ELYTRA_FLYING) {
            if (rocketMan.shouldMuteElytra()) ci.cancel();
        }
    }
}
