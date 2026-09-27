package dev.stardust.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Connection.class)
public interface ClientConnectionAccessor {
    @Invoker("sendImmediately")
    void invokeSendImmediately(Packet<?> packet, PacketSendListener callbacks, boolean flush);
}
