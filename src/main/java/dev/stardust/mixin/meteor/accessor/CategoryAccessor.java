package dev.stardust.mixin.meteor.accessor;

import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import meteordevelopment.meteorclient.systems.modules.Category;

@Mixin(value = Category.class, remap = false)
public interface CategoryAccessor {
    // 26.1: Meteor 的 Category.icon 类型是 Supplier<ItemStack>（不是 ItemStack）
    @Mutable
    @Accessor("icon")
    void setIcon(Supplier<ItemStack> icon);
}
