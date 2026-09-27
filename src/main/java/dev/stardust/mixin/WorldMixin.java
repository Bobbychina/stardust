package dev.stardust.mixin;

import net.minecraft.world.level.Level;
import dev.stardust.modules.AutoSmith;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.sounds.SoundSource;
import dev.stardust.modules.StashBrander;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(Level.class)
public abstract class WorldMixin implements LevelAccessor, AutoCloseable {
    // See StashBrander.java && AutoSmith.java
    @Inject(method = "playLocalSound", at = @At("HEAD"), cancellable = true)
    private void mixinPlaySoundAtBlockCenter(BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch, boolean useDistance, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AutoSmith smith = modules.get(AutoSmith.class);
        StashBrander brander = modules.get(StashBrander.class);
        if (brander.isActive() && brander.shouldMute()) {
            if (sound == SoundEvents.ANVIL_USE || sound == SoundEvents.ANVIL_BREAK) ci.cancel();
        }
        if (smith.isActive() && smith.muteSmithy.get()) {
            if (sound == SoundEvents.SMITHING_TABLE_USE) ci.cancel();
        }
    }
}
