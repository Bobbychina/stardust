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

    // See AntiToS.java
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component censorChatMessage(Component message) {
        Modules modules = Modules.get();
        if (modules == null) return message;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive() || antiToS.chatMode.get() == AntiToS.ChatMode.Remove) return message;

        if (antiToS.containsBlacklistedText(message.getString())) {
            return TextUtil.modifyWithStyle(message.copy(), antiToS::censorText);
        }

        return message;
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void maybeCancelAddMessage(Component message, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;
        if (antiToS.chatMode.get() == AntiToS.ChatMode.Remove && antiToS.containsBlacklistedText(message.getString())) ci.cancel();
    }
}
