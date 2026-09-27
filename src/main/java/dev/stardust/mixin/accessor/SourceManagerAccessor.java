package dev.stardust.mixin.accessor;

import net.minecraft.client.sounds.ChannelAccess;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.sounds.ChannelAccess;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChannelAccess.ChannelHandle.class)
public interface SourceManagerAccessor {
    @Accessor("source")
    ChannelAccess getSource();
}
