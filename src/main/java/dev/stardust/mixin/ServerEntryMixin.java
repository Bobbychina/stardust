package dev.stardust.mixin;

import net.minecraft.util.Util;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

import com.mojang.realmsclient.RealmsMainScreen.ServerEntry;
/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     See also: EntryListWidgetMixin.java && NeedleAngleStateMixin.java && stardust.accesswidener
 **/
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class ServerEntryMixin extends ServerSelectionList.Entry {
    @Shadow
    @Final
    private ServerData server;

    @Shadow
    protected abstract boolean canConnect();

    @Shadow
    @Final
    private JoinMultiplayerScreen screen;

    @Shadow
    protected abstract void swapEntries(int i, int j);

    @Shadow
    private long time;

    @Shadow
    @Final
    private Minecraft client;

    @Shadow
    @Final
    ServerSelectionList field_19117;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;getValue()Ljava/lang/Object;"), cancellable = true)
    private void render2b2tClock(GuiGraphicsExtractor context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        if (!StardustConfig.serverListWorldTimeClockSetting.get()) return;

        String name = this.server.name;
        String address = this.server.ip;
        if (name.toLowerCase().contains("2b2t") || address.equalsIgnoreCase("2b2t.org") || address.equalsIgnoreCase("connect.2b2t.org")) {
            ci.cancel();

            // Prevent the reorder buttons from highlighting when hovering over the extended (clock) part of the widget by checking that o > 0
            if (this.client.options.touchscreen().get() || hovered) {
                context.fill(x, y, x + 32, y + 32, -1601138544);

                int o = mouseX - x;
                int p = mouseY - y;
                if (this.canConnect()) {
                    if (o < 32 && o > 16) {
                        context.drawGuiTexture(RenderType::getGuiTextured, ServerSelectionList.JOIN_HIGHLIGHTED_SPRITE, x, y, 32, 32);
                    } else {
                        context.drawGuiTexture(RenderType::getGuiTextured, ServerSelectionList.JOIN_SPRITE, x, y, 32, 32);
                    }
                }

                if (index > 0) {
                    if (o < 16 && o > 0 && p < 16) {
                        context.drawGuiTexture(RenderType::getGuiTextured, ServerSelectionList.MOVE_UP_HIGHLIGHTED_SPRITE, x, y, 32, 32);
                    } else {
                        context.drawGuiTexture(RenderType::getGuiTextured, ServerSelectionList.MOVE_UP_SPRITE, x, y, 32, 32);
                    }
                }

                if (index < this.screen.getServers().size() - 1) {
                    if (o < 16 && o > 0 && p > 16) {
                        context.drawGuiTexture(RenderType::getGuiTextured, ServerSelectionList.MOVE_DOWN_HIGHLIGHTED_SPRITE, x, y, 32, 32);
                    } else {
                        context.drawGuiTexture(RenderType::getGuiTextured, ServerSelectionList.MOVE_DOWN_SPRITE, x, y, 32, 32);
                    }
                }
            }

            // See NeedleAngleStateMixin.java for custom clock time source logic
            RenderUtils.drawItem(
                context, Items.CLOCK.getDefaultInstance(),
                x - 34, y, 2.0f, false
            );
        }
    }

    // Prevent the reorder buttons from activating when clicking on the extended (clock) part of the widget by checking that d > 0.0
    @Inject(method = "mouseClicked", at = @At(value = "HEAD"), cancellable = true)
    private void hijackMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!StardustConfig.serverListWorldTimeClockSetting.get()) return;

        String name = this.server.name;
        String address = this.server.ip;
        if (name.toLowerCase().contains("2b2t") || address.equalsIgnoreCase("2b2t.org") || address.equalsIgnoreCase("connect.2b2t.org")) {
            cir.cancel();
            double d = mouseX - (double) this.field_19117.getRowLeft();
            double e = mouseY - (double) this.field_19117.getRowTop(this.field_19117.children().indexOf(this));

            if (d <= 32.0) {
                if (d < 32.0 && d > 16.0 && this.canConnect()) {
                    this.screen.setSelected(this);
                    this.screen.connect();
                    cir.setReturnValue(true);
                }

                int i = this.screen.serverSelectionList.children().indexOf(this);
                if (d < 16.0 && d > 0.0 && e < 16.0 && i > 0) {
                    this.swapEntries(i, i - 1);
                    cir.setReturnValue(true);
                }

                if (d < 16.0 && d > 0.0 && e > 16.0 && i < this.screen.getServers().size() - 1) {
                    this.swapEntries(i, i + 1);
                    cir.setReturnValue(true);
                }
            }

            this.screen.setSelected(this);
            if (Util.getMeasuringTimeMs() - this.time < 250L) {
                this.screen.connect();
            }

            this.time = Util.getMeasuringTimeMs();
            cir.setReturnValue(true);
        }
    }
}
