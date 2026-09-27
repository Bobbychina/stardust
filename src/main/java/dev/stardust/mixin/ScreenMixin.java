package dev.stardust.mixin;

import java.util.Arrays;
import net.minecraft.network.chat.*;
import dev.stardust.util.LogUtil;
import dev.stardust.modules.AntiToS;
import dev.stardust.modules.ChatSigns;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.Renderable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.injection.At;
import dev.stardust.mixin.accessor.StyleAccessor;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler implements Renderable {

    @Shadow
    @Final
    @Mutable
    protected Component title;

    // See AntiToS.java
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void censorScreenTitles(CallbackInfo ci) {
        Modules mods = Modules.get();
        if (mods == null) return;
        AntiToS tos = mods.get(AntiToS.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (tos == null) return;
        if (!tos.isActive() || !tos.containsBlacklistedText(this.title.getString())) return;
        MutableComponent txt = Component.literal(tos.censorText(this.title.getString()));
        this.title = txt.setStyle(this.title.getStyle());
    }

    // See ChatSigns.java
    @Inject(method = "handleTextClick", at = @At("HEAD"), cancellable = true, require = 0)
    private void handleClickESP(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (style == null) return;
        ClickEvent event = style.getClickEvent();
        // 26.1: ClickEvent 变成接口 + RunCommand record，动作/命令都要走记录访问器
        if (!(event instanceof ClickEvent.RunCommand runCommand)) return;

        String command = runCommand.command();
        if (command.startsWith("clickESP~")) {
            String[] args = command.split("~");

            String mod;
            BlockPos pos;
            try {
                mod = args[1];
                String posStr = args[2];
                long packedPos = Long.parseLong(posStr);
                pos = BlockPos.of(packedPos);
            } catch (Exception err) {
                LogUtil.error("Invalid custom ClickEvent syntax: "+Arrays.toString(args)+"\n"+err, "ScreenMixin");
                return;
            }
            cir.cancel();
            cir.setReturnValue(true);
            long now = System.currentTimeMillis();

            switch (mod) {
                case "chatSigns" -> {
                    Modules mods = Modules.get();
                    if (mods == null) return;
                    ChatSigns chatSigns = mods.get(ChatSigns.class);
                    // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
                    if (chatSigns == null) return;
                    if (chatSigns.toggleClickESP(pos, now)) {
                        ((StyleAccessor) (Object) style).setHoverEvent(
                            new HoverEvent.ShowText(
                                Component.literal("§4§oDisable §7§oESP for this sign.")
                            )
                        );
                    } else {
                        ((StyleAccessor) (Object) style).setHoverEvent(
                            new HoverEvent.ShowText(
                                Component.literal("§2§oEnable §7§oESP for this sign.")
                            )
                        );
                    }
                }
                case "reserved" -> {}
            }
        }
    }
}
