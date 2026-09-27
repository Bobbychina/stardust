package dev.stardust.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Options.class)
public interface GameOptionsAccessor {
    @Mutable
    @Accessor("viewDistance")
    void setViewDistance(OptionInstance<Integer> viewDistance);
}
