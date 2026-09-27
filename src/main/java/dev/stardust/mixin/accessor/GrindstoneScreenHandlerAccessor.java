package dev.stardust.mixin.accessor;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.world.inventory.GrindstoneMenu;

@Mixin(GrindstoneMenu.class)
public interface GrindstoneScreenHandlerAccessor {
    // 26.1: GrindstoneMenu.grind(ItemStack) → removeNonCursesFrom(ItemStack)
    @Invoker("removeNonCursesFrom")
    ItemStack invokeGrind(ItemStack item);

    // 26.1: GrindstoneMenu.transferEnchantments(ItemStack,ItemStack) → mergeEnchantsFrom(...)
    @Invoker("mergeEnchantsFrom")
    void invokeTransferEnchantments(ItemStack target, ItemStack source);
}
