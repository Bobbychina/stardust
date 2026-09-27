package dev.stardust.mixin.accessor;

import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AnvilMenu.class)
public interface AnvilScreenHandlerAccessor {
    @Accessor
    DataSlot getLevelCost();
}
