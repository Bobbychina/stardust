package dev.stardust.mixin;

import dev.stardust.modules.RocketMan;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(ElytraOnPlayerSoundInstance.class)
public abstract class ElytraSoundInstanceMixin extends AbstractTickableSoundInstance {
    protected ElytraSoundInstanceMixin(SoundEvent soundEvent, SoundSource soundCategory, RandomSource random) {
        super(soundEvent, soundCategory, random);
    }

    // See RocketMan.java
    @Inject(method = "tick", at = @At("HEAD"))
    private void mixinTick(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        RocketMan rocketMan = modules.get(RocketMan.class);
        if (rocketMan.isActive() && rocketMan.shouldMuteElytra()) this.stop();
    }
}
