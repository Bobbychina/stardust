package dev.stardust.mixin;

import net.minecraft.util.Util;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import dev.stardust.mixin.accessor.ServerEntrySwapInvoker;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.resources.Identifier;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     See also: EntryListWidgetMixin.java && NeedleAngleStateMixin.java && stardust.accesswidener
 **/
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class ServerEntryMixin extends ServerSelectionList.Entry {
    // 26.1: ServerSelectionList 的这些 sprite 常量仍是 private（access widener 里是旧命名空间的旧描述符），
    // 所以按 1.21.4 的原值在本地重建，既不依赖 AW 也保证 javac 过。
    @Unique private static final Identifier JOIN_SPRITE = Identifier.withDefaultNamespace("server_list/join");
    @Unique private static final Identifier JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/join_highlighted");
    @Unique private static final Identifier MOVE_UP_SPRITE = Identifier.withDefaultNamespace("server_list/move_up");
    @Unique private static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_up_highlighted");
    @Unique private static final Identifier MOVE_DOWN_SPRITE = Identifier.withDefaultNamespace("server_list/move_down");
    @Unique private static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_down_highlighted");

    // 26.1 字段改名：server→serverData、client→minecraft、field_19117（父列表）→list
    @Shadow
    @Final
    private ServerData serverData;

    @Shadow
    @Final
    private JoinMultiplayerScreen screen;

    @Shadow
    @Final
    private Minecraft minecraft;

    // 26.1: OnlineServerEntry 不再持有父列表字段，父列表由内部类的合成字段 this$0 指向
    @Shadow
    @Final
    private ServerSelectionList this$0;

    // TODO(26.1): 26.1 的 OnlineServerEntry 已无 canConnect()，这里保留同名 shadow 以免误删逻辑，
    // 但注入目标需人工核对（verify_mixins 已列为待查项）；语义近似于“该条目当前可连接”。
    @Unique
    private boolean canConnect() {
        return true;
    }

    // 26.1 方法名 swapEntries → swap（private，AA 里仍是 abstract shadow，运行期需核对可见性）

    // 26.1 的 OnlineServerEntry 不再持有双击计时字段，改为在 mixin 本地保留（等价实现）
    @Unique
    private long time;

    // 26.1: 渲染入口由 render(...) 重做为 extractContent(GuiGraphicsExtractor, int mouseX, int mouseY, boolean hovered, float delta)，
    // 原几何参数改为从父列表取（getRowLeft/getRowWidth + Entry.getY/getHeight）。
    @Inject(method = "extractContent", at = @At("HEAD"), cancellable = true)
    private void render2b2tClock(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        if (!StardustConfig.serverListWorldTimeClockSetting.get()) return;
        int index = this.this$0.children().indexOf(this);
        int x = this.this$0.getRowLeft();
        int y = this.getY();
        int entryWidth = this.this$0.getRowWidth();
        int entryHeight = this.getHeight();

        String name = this.serverData.name;
        String address = this.serverData.ip;
        if (name.toLowerCase().contains("2b2t") || address.equalsIgnoreCase("2b2t.org") || address.equalsIgnoreCase("connect.2b2t.org")) {
            ci.cancel();

            // Prevent the reorder buttons from highlighting when hovering over the extended (clock) part of the widget by checking that o > 0
            if (this.minecraft.options.touchscreen().get() || hovered) {
                context.fill(x, y, x + 32, y + 32, -1601138544);

                int o = mouseX - x;
                int p = mouseY - y;
                if (this.canConnect()) {
                    if (o < 32 && o > 16) {
                        context.blitSprite(RenderPipelines.GUI_TEXTURED, JOIN_HIGHLIGHTED_SPRITE, x, y, 32, 32);
                    } else {
                        context.blitSprite(RenderPipelines.GUI_TEXTURED, JOIN_SPRITE, x, y, 32, 32);
                    }
                }

                if (index > 0) {
                    if (o < 16 && o > 0 && p < 16) {
                        context.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_UP_HIGHLIGHTED_SPRITE, x, y, 32, 32);
                    } else {
                        context.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_UP_SPRITE, x, y, 32, 32);
                    }
                }

                if (index < this.screen.getServers().size() - 1) {
                    if (o < 16 && o > 0 && p > 16) {
                        context.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_HIGHLIGHTED_SPRITE, x, y, 32, 32);
                    } else {
                        context.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_SPRITE, x, y, 32, 32);
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
    // 26.1: 鼠标回调改为 mouseClicked(MouseButtonEvent, boolean)
    @Inject(method = "mouseClicked", at = @At(value = "HEAD"), cancellable = true)
    private void hijackMouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (!StardustConfig.serverListWorldTimeClockSetting.get()) return;

        String name = this.serverData.name;
        String address = this.serverData.ip;
        if (name.toLowerCase().contains("2b2t") || address.equalsIgnoreCase("2b2t.org") || address.equalsIgnoreCase("connect.2b2t.org")) {
            cir.cancel();
            double d = mouseX - (double) this.this$0.getRowLeft();
            double e = mouseY - (double) this.this$0.getRowTop(this.this$0.children().indexOf(this));

            if (d <= 32.0) {
                if (d < 32.0 && d > 16.0 && this.canConnect()) {
                    this.this$0.setSelected(this);
                    this.join();
                    cir.setReturnValue(true);
                }

                int i = this.this$0.children().indexOf(this);
                if (d < 16.0 && d > 0.0 && e < 16.0 && i > 0) {
                    ((ServerEntrySwapInvoker)(Object)this).stardust$swap(i, i - 1);
                    cir.setReturnValue(true);
                }

                if (d < 16.0 && d > 0.0 && e > 16.0 && i < this.screen.getServers().size() - 1) {
                    ((ServerEntrySwapInvoker)(Object)this).stardust$swap(i, i + 1);
                    cir.setReturnValue(true);
                }
            }

            this.this$0.setSelected(this);
            if (Util.getMillis() - this.time < 250L) {
                this.join();
            }

            this.time = Util.getMillis();
            cir.setReturnValue(true);
        }
    }
}
