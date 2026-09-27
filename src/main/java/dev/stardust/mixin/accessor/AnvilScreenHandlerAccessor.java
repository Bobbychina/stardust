package dev.stardust.mixin.accessor;

import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AnvilMenu.class)
public interface AnvilScreenHandlerAccessor {
    // 26.1: AnvilMenu 的字段由 levelCost 改名为 cost（裸 @Accessor 会按方法名推导出 levelCost，故显式指定）
    @Accessor("cost")
    DataSlot getLevelCost();
}
