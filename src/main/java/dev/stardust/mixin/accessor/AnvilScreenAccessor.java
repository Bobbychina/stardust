package dev.stardust.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;

@Mixin(AnvilScreen.class)
public interface AnvilScreenAccessor {
    // 26.1: AnvilScreen 字段 nameField → name（裸 @Accessor 会按方法名推导，必须显式指定）
    @Accessor("name")
    EditBox getNameField();
}
