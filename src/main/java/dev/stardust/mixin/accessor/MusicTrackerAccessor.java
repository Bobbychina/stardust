package dev.stardust.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MusicManager.class)
public interface MusicTrackerAccessor {
    @Accessor("timeUntilNextSong")
    void setTimeUntilNextSong(int time);

    @Accessor("current")
    SoundInstance getCurrent();
}
