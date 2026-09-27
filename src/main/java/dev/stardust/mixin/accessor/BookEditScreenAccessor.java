package dev.stardust.mixin.accessor;

import net.minecraft.client.gui.font.TextFieldHelper;

/**
 * TODO(26.1): BookEditScreen 在 26.1 已无 TextFieldHelper 字段（改为 pages:List&lt;String&gt; / signField 体系），
 * 原 currentPageSelectionManager / bookTitleSelectionManager 两个 @Accessor 没有对应目标，
 * 继续注册会在启动时抛 InvalidAccessorException。本接口已从 stardust.mixins.json 摘除，
 * 仅作为编译期占位（返回 null），调用点改成安全空操作；书本彩虹格式化功能见 PORT-NOTES 遗留风险 R1。
 */
public interface BookEditScreenAccessor {
    default TextFieldHelper getCurrentPageSelectionManager() { return null; }

    default TextFieldHelper getBookTitleSelectionManager() { return null; }
}
