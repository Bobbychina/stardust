package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
import dev.stardust.util.StardustUtil;
import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.gui.layouts.GridLayout;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(PauseScreen.class)
public class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout;refreshPositions()V"))
    private void addIllegalDisconnectButton(CallbackInfo ci, @Local GridLayout.RowHelper adder) {
        if (StardustConfig.illegalDisconnectButtonSetting.get() && !mc.isLocalServer()) {
            adder.add(Button.builder(Component.literal("§cIllegal Disconnect"), button -> {
                button.active = false;
                StardustUtil.illegalDisconnect(false, StardustConfig.illegalDisconnectMethodSetting.get());
            }).width(204).build(), 2);
        }
    }
}
