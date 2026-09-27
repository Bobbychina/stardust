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

    // 26.1: PauseScreen 不再有 initWidgets()，GridLayout/RowHelper 的装配被内联进 init()；
    //       原锚点 GridLayout#refreshPositions 也已移除，改为挂在最后一次 RowHelper#addChild(LayoutElement,int) 之后
    //       （此处 RowHelper 局部变量必定存活，且仍在 arrangeElements() 布局之前）。
    // TODO(26.1): 若运行期 @Local 取不到 RowHelper，需改用 @At("TAIL") + 自行从 GridLayout 取行。
    @Inject(method = "init", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;I)Lnet/minecraft/client/gui/layouts/LayoutElement;"),
        require = 0)  // 26.1 实测该锚点扫不到（0/1），先把游戏能起，功能见 PORT-NOTES R1
    private void addIllegalDisconnectButton(CallbackInfo ci, @Local GridLayout.RowHelper adder) {
        if (StardustConfig.illegalDisconnectButtonSetting.get() && !mc.isLocalServer()) {
            adder.addChild(Button.builder(Component.literal("§cIllegal Disconnect"), button -> {
                button.active = false;
                StardustUtil.illegalDisconnect(false, StardustConfig.illegalDisconnectMethodSetting.get());
            }).width(204).build(), 2);
        }
    }
}
