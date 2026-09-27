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
    // 26.1: playLocalSound 有 3 个重载，写全描述符避免歧义
    @Inject(method = "playLocalSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V", at = @At("HEAD"), cancellable = true)
    private void mixinPlaySoundAtBlockCenter(BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch, boolean useDistance, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AutoSmith smith = modules.get(AutoSmith.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (smith == null) return;
        StashBrander brander = modules.get(StashBrander.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (brander == null) return;
        if (brander.isActive() && brander.shouldMute()) {
            if (sound == SoundEvents.ANVIL_USE || sound == SoundEvents.ANVIL_BREAK) ci.cancel();
        }
        if (smith.isActive() && smith.muteSmithy.get()) {
            if (sound == SoundEvents.SMITHING_TABLE_USE) ci.cancel();
        }
    }
}
