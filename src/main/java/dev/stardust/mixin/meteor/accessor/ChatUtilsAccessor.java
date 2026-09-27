package dev.stardust.mixin.meteor.accessor;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

@Mixin(value = ChatUtils.class, remap = false)
public interface ChatUtilsAccessor {
    @Mutable
    @Accessor("PREFIX")
    static void setPrefix(Component prefix) {
        throw new AssertionError();
    }
}
