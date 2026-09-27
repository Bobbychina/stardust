package dev.stardust.mixin.accessor;

import java.util.List;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;

@Mixin(BookViewScreen.BookAccess.class)
public interface BookScreenContentsAccessor {
    @Accessor
    List<Component> getPages();

    @Mutable
    @Accessor("pages")
    void setPages(List<Component> pages);
}
