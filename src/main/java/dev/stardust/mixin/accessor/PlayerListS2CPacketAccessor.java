package dev.stardust.mixin.accessor;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

@Mixin(ClientboundPlayerInfoUpdatePacket.Entry.class)
public interface PlayerListS2CPacketAccessor {
    @Mutable
    @Accessor("profile")
    void setProfile(GameProfile profile);
}
