package dev.stardust.mixin.accessor;

import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 26.1: OnlineServerEntry.swap(int,int) 为 private，Java 无法用 @Shadow private abstract 声明，改用 @Invoker 暴露。 */
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public interface ServerEntrySwapInvoker {
    @Invoker("swap")
    void stardust$swap(int i, int j);
}
