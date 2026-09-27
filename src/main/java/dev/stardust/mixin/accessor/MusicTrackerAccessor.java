package dev.stardust.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MusicManager.class)
public interface MusicTrackerAccessor {
    // 26.1: MusicManager 的字段改名为 nextSongDelay / currentMusic
    @Accessor("nextSongDelay")
    void setTimeUntilNextSong(int time);

    @Accessor("currentMusic")
    SoundInstance getCurrent();
}
