package dev.stardust.mixin.accessor;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;

@Mixin(ClientboundDisconnectPacket.class)
public interface DisconnectS2CPacketAccessor {
    @Mutable
    @Accessor("reason")
    void setReason(Component reason);
}
