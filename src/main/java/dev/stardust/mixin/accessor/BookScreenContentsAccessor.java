package dev.stardust.mixin.accessor;

import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * TODO(26.1): BookViewScreen.BookAccess 在 26.1 变成 record（pages() 只读），
 * 原 @Accessor("pages") setPages 无可行目标；BookScreenMixin 已改为整体替换 bookAccess，
 * 本接口仅留作编译期占位，并已从 stardust.mixins.json 摘除（继续注册会抛 InvalidAccessorException）。
 */
public interface BookScreenContentsAccessor {
    default List<Component> getPages() { return List.of(); }

    default void setPages(List<Component> pages) { }
}
