package dev.stardust.mixin;

import java.util.Map;
import net.minecraft.network.chat.Component;
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
    @Shadow
    @Final
    private Map<SoundInstance, Channel.SourceManager> sources;

    @Unique
    @Mutable
    private int totalTicksPlaying;
    @Unique
    private boolean dirtyPitch = false;
    @Unique
    private boolean dirtyVolume = false;


    // See MusicTweaks.java
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void mixinTick(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null ) return;
        MusicTweaks tweaks = modules.get(MusicTweaks.class);

        boolean playing = false;
        String songID = null;
        for (SoundInstance instance : sources.keySet()) {
            Sound sound = instance.getSound();
            if (sound == null) continue;

            String location = sound.getIdentifier().toString();
            if (!location.startsWith("minecraft:sounds/music/") && !sound.toString().contains("minecraft:records/")) continue;
            Channel.SourceManager sourceManager = this.sources.get(instance);
            songID = location.substring(location.lastIndexOf('/') + 1);

            if (sourceManager == null) continue;
            Channel source = ((SourceManagerAccessor) sourceManager).getSource();
            if (source == null) continue;

            playing = true;
            tweaks.setCurrentSong(sound.toString());
            if (tweaks.isActive() && !tweaks.randomPitch()) {
                this.dirtyPitch = true;
                source.setXRot(1.0f + tweaks.getPitchAdjustment());
            } else if (tweaks.isActive() && tweaks.randomPitch() && tweaks.trippyPitch()) {
                this.dirtyPitch = true;
                source.setXRot(tweaks.getNextPitchStep(instance.getXRot())); // !!
            } else if (!tweaks.isActive() && this.dirtyPitch) {
                source.setXRot(1f);
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
