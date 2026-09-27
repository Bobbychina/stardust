package dev.stardust.mixin.meteor;

import java.util.Set;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import dev.stardust.mixin.accessor.ClientConnectionAccessor;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import meteordevelopment.meteorclient.systems.modules.misc.PacketCanceller;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(value = PacketCanceller.class, remap = false)
public class PacketCancellerMixin extends Module {
    @Shadow
    @Final
    private Setting<Set<Class<? extends Packet<?>>>> c2sPackets;

    public PacketCancellerMixin(Category category, String name, String description, String... aliases) {
        super(category, name, description, aliases);
    }

    @Inject(method = "onSendPacket", at = @At("HEAD"))
    private void silenceBoatPaddles(PacketEvent.Send event, CallbackInfo ci) {
        if (c2sPackets.get().contains(ServerboundPaddleBoatPacket.class) && event.packet instanceof ServerboundPaddleBoatPacket) {
            if (mc.player != null && mc.player.getControlledVehicle() instanceof AbstractBoat boat) {
                boat.setPaddlesMoving(false, false);
                if (mc.getConnection() != null) ((ClientConnectionAccessor) mc.getConnection().getConnection()).invokeSendImmediately(
                    new ServerboundPaddleBoatPacket(false, false), null, true
                );
            }
        }
    }
}
