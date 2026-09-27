package dev.stardust.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Connection.class)
public interface ClientConnectionAccessor {
    // 26.1: Connection.sendImmediately(Packet,PacketSendListener,boolean) → sendPacket(Packet,ChannelFutureListener,boolean)
    @Invoker("sendPacket")
    void invokeSendImmediately(Packet<?> packet, io.netty.channel.ChannelFutureListener callbacks, boolean flush);
}
