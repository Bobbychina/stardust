package dev.stardust.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.sounds.ChannelAccess;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChannelAccess.ChannelHandle.class)
public interface SourceManagerAccessor {
    // 26.1: ChannelHandle 的字段名是 channel（Yarn 里叫 source），类型搬到 com.mojang.blaze3d.audio.Channel
    @Accessor("channel")
    com.mojang.blaze3d.audio.Channel getSource();
}
