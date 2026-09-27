package dev.stardust.mixin;

import java.util.List;
import java.util.Arrays;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.modules.SignHistorian;
import dev.stardust.modules.SignatureSign;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.client.gui.font.TextFieldHelper;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import dev.stardust.mixin.accessor.AbstractSignEditScreenAccessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen {

    @Shadow
    private int currentRow;
    @Shadow
    public abstract void close();
    @Shadow
    @Final
    protected SignBlockEntity blockEntity;
    @Shadow
    private TextFieldHelper selectionManager;
    @Shadow
    protected abstract void setCurrentRowMessage(String message);

    protected AbstractSignEditScreenMixin(Component title) { super(title); }

    // See SignatureSign.java && SignHistorian.java
    @Inject(method = "init", at = @At("TAIL"))
    public void stardustMixinInit(CallbackInfo ci) {
        if (this.client == null) return;
        Modules modules = Modules.get();

        if (modules == null) return;
        SignHistorian signHistorian = modules.get(SignHistorian.class);
        SignatureSign signatureSign = modules.get(SignatureSign.class);
        if (!signatureSign.isActive() && !signHistorian.isActive()) return;

        if (signatureSign.getAutoConfirm()) return;
        SignText restoration = signHistorian.getRestoration(this.blockEntity);
        if ((!signHistorian.isActive() || restoration == null) && signatureSign.isActive()) {
            SignText signature = signatureSign.getSignature(this.blockEntity);
            List<String> msgs = Arrays.stream(signature.getMessages(false)).map(Component::getString).toList();
            String[] messages = new String[msgs.size()];
            messages = msgs.toArray(messages);

            ((AbstractSignEditScreenAccessor) this).setText(signature);
            ((AbstractSignEditScreenAccessor) this).setMessages(messages);
            if ((signatureSign.isActive() && signatureSign.signFreedom.get())) {
                // bypass client-side length limits for sign text by using a truthy predicate in the TextFieldHelper
                AbstractSignEditScreenAccessor accessor = ((AbstractSignEditScreenAccessor) this);
                this.selectionManager = new TextFieldHelper(
                    () -> accessor.getMessages()[this.currentRow], this::setCurrentRowMessage,
                    TextFieldHelper.makeClipboardGetter(this.client), TextFieldHelper.makeClipboardSetter(this.client),
                    string -> true
                );
            }

            if (signatureSign.needsDisabling()) {
                signatureSign.disable();
            }
        }
    }
}
