package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
import dev.stardust.util.TextUtil;
import dev.stardust.modules.AntiToS;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(ChatComponent.class)
public class ChatHudMixin {

    // 26.1: addMessage 多了 GuiMessageSource 参数，GuiMessageTag 也搬到 ...multiplayer.chat 包
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component censorChatMessage(Component message) {
        Modules modules = Modules.get();
        if (modules == null) return message;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (antiToS == null) return message;
        if (!antiToS.isActive() || antiToS.chatMode.get() == AntiToS.ChatMode.Remove) return message;

        if (antiToS.containsBlacklistedText(message.getString())) {
            return TextUtil.modifyWithStyle(message.copy(), antiToS::censorText);
        }

        return message;
    }

    // 26.1: 单参 addMessage(Component) 已不存在，改为挂 4 参版本
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"), cancellable = true)
    private void maybeCancelAddMessage(Component message, net.minecraft.network.chat.MessageSignature signature,
                                       net.minecraft.client.multiplayer.chat.GuiMessageSource source,
                                       net.minecraft.client.multiplayer.chat.GuiMessageTag tag, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (antiToS == null) return;
        if (!antiToS.isActive()) return;
        if (antiToS.chatMode.get() == AntiToS.ChatMode.Remove && antiToS.containsBlacklistedText(message.getString())) ci.cancel();
    }
}
