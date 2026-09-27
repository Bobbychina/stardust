package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.ConnectScreen;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Unique
    private static final ServerData OLD_SERVER = new ServerData("2b2t", "2b2t.org", ServerData.Type.OTHER);

    @Shadow
    private SplashRenderer splashText;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private int timer = 0;
    @Unique
    private Minecraft mc = null;

    @Unique
    private void onClick2b2tButton(Button btn) {
        if (mc == null) mc = Minecraft.getInstance();
        ConnectScreen.connect(mc.screen, mc,
            ServerAddress.parseString(OLD_SERVER.ip), OLD_SERVER, true, null
        );
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void mixinInit(CallbackInfo ci) {
        if (StardustConfig.directConnectButtonSetting.get()) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("§c§l2§a§lB"), this::onClick2b2tButton)
                .bounds(this.width / 2 + 104, this.height / 4 + 72, 20, 20)
                .build()
            );
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void mixinTick(CallbackInfo ci) {
        if (mc == null) {
            mc = Minecraft.getInstance();
            return;
        }

        ++timer;
        if (timer >= 420 && StardustConfig.rotateSplashTextSetting.get()) {
            timer = 0;
            splashText = mc.getSplashManager().getSplash();
        }
    }
}
