package dev.stardust.mixin.accessor;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;

@Mixin(ClientboundSetEntityDataPacket.class)
public interface EntityTrackerUpdateS2CPacketAccessor {
    @Mutable
    @Accessor("packedItems")
    void setTrackedValues(List<SynchedEntityData.DataValue<?>> trackedValues);
}
