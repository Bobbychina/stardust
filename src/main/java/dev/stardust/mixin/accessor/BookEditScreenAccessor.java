package dev.stardust.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;

@Mixin(BookEditScreen.class)
public interface BookEditScreenAccessor {
    @Accessor
    TextFieldHelper getCurrentPageSelectionManager();

    @Accessor
    TextFieldHelper getBookTitleSelectionManager();
}
