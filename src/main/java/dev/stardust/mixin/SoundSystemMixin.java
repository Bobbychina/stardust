package dev.stardust.mixin;

import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.*;
import org.spongepowered.asm.mixin.*;
import dev.stardust.modules.MusicTweaks;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import dev.stardust.mixin.accessor.SourceManagerAccessor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(SoundEngine.class)
public class SoundSystemMixin {
    // 26.1: Yarn 的 SoundSystem.sources 改名 instanceToChannel，元素类型是 ChannelAccess.ChannelHandle
    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Unique
    @Mutable
    private int totalTicksPlaying;
    @Unique
    private boolean dirtyPitch = false;
    @Unique
    private boolean dirtyVolume = false;


    // See MusicTweaks.java
    @Inject(method = "tick(Z)V", at = @At("TAIL"))
    private void mixinTick(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null ) return;
        MusicTweaks tweaks = modules.get(MusicTweaks.class);
        // 启动期模块可能还没注册完 → get() 返回 null，直接解引用会在 SoundEngine.tick 里炸整局
        if (tweaks == null) return;

        boolean playing = false;
        String songID = null;
        for (SoundInstance instance : instanceToChannel.keySet()) {
            Sound sound = instance.getSound();
            if (sound == null) continue;

            String location = sound.getLocation().toString();
            if (!location.startsWith("minecraft:sounds/music/") && !sound.toString().contains("minecraft:records/")) continue;
            ChannelAccess.ChannelHandle sourceManager = this.instanceToChannel.get(instance);
            songID = location.substring(location.lastIndexOf('/') + 1);

            if (sourceManager == null) continue;
            com.mojang.blaze3d.audio.Channel source = ((SourceManagerAccessor) sourceManager).getSource();
            if (source == null) continue;

            playing = true;
            tweaks.setCurrentSong(sound.toString());
            if (tweaks.isActive() && !tweaks.randomPitch()) {
                this.dirtyPitch = true;
                source.setPitch(1.0f + tweaks.getPitchAdjustment());
            } else if (tweaks.isActive() && tweaks.randomPitch() && tweaks.trippyPitch()) {
                this.dirtyPitch = true;
                source.setPitch(tweaks.getNextPitchStep(instance.getPitch())); // !!
            } else if (!tweaks.isActive() && this.dirtyPitch) {
                source.setPitch(1f);
                this.dirtyPitch = false;
            }
            if (tweaks.isActive()) {
                this.dirtyVolume = true;
                source.setVolume(Mth.clamp(tweaks.getClient().options.getSoundSourceVolume(instance.getSource()) + tweaks.getVolumeAdjustment(), 0.0f, 4.0f));
            } else if (this.dirtyVolume) {
                this.dirtyVolume = false;
                source.setVolume(tweaks.getClient().options.getSoundSourceVolume(instance.getSource()));
            }
        }
        if (playing) {
            ++this.totalTicksPlaying;
        } else {
            this.totalTicksPlaying = 0;
        }

        if (tweaks.isActive() && this.totalTicksPlaying % 30 == 0 && tweaks.shouldDisplayNowPlaying() && songID != null) {
            if (this.totalTicksPlaying <= 90 || !tweaks.shouldFadeOut()) {
                String songName = tweaks.getSongName(songID);

                // See NarratorManagerMixin.java lol
                switch (tweaks.getDisplayMode()) {
                    case Chat -> tweaks.sendNowPlayingMessage(songName);
                    case Record -> tweaks.getClient().gui.setNowPlaying(Component.literal(songName));
                }
            }
        }
    }
}
