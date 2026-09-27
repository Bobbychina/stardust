package dev.stardust.mixin;

import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     See also: ServerEntryMixin.java && NeedleAngleStateMixin.java && stardust.accesswidener
 **/
@Mixin(AbstractSelectionList.class)
public abstract class EntryListWidgetMixin<E extends ObjectSelectionList.Entry<E>> extends AbstractContainerWidget {
    // 26.1: AbstractContainerWidget 构造器多了 ScrollbarSettings 参数（原 5 参版本已不存在）
    private EntryListWidgetMixin(int i, int j, int k, int l, Component text) {
        super(0, j, k, l, text, AbstractScrollArea.defaultSettings(Math.max(1, l) / 2));
    }

    @Shadow
    public abstract int getRowWidth();

    // TODO(26.1): AbstractSelectionList 已无 headerHeight 字段，本 mixin 只服务服务端列表，取 0 等价
    @Unique
    protected int headerHeight = 0;

    @Shadow
    @Final
    protected int defaultEntryHeight;

    @Shadow
    @Final
    private List<E> children;

    // 26.1: renderEntry(...) 重做为 extractItem(GuiGraphicsExtractor, mouseX, mouseY, delta, E entry)。
    // TODO(26.1)【功能已停用，勿静默忽略】：extractItem 的第 5 参擦除成 **protected** 的 AbstractSelectionList$Entry，
    // 而 mixin 的泛型上界只能写成公开的 ObjectSelectionList$Entry（Java 源码无法命名 protected 嵌套类型），
    // 描述符对不上 → 挂不上该注入（require=0 也无效，因为是描述符不匹配而非找不到目标）。
    // 影响：服务端列表里 2b2t 条目的「加宽可选区/时钟区可点」失效。要恢复需改用 access widener + 重写类型，
    // 见 PORT-NOTES 遗留风险 R1。下面保留原实现体作为 @Unique 草案（当前不被调用）。
    @Unique
    private void stardust$extendSelectionBoxFor2b2tClock(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, E entry) {
        if (!StardustConfig.serverListWorldTimeClockSetting.get()) return;

        int entryWidth0 = ((AbstractSelectionList<?>) (Object) this).getRowWidth();
        int entryHeight0 = entry.getHeight();
        if (entry instanceof ServerSelectionList.OnlineServerEntry) {
            ServerData info0 = ((ServerSelectionList.OnlineServerEntry) entry).getServerData();
            String name0 = info0.name;
            String address0 = info0.ip;
            if (!name0.toLowerCase().contains("2b2t") && !address0.equalsIgnoreCase("2b2t.org") && !address0.equalsIgnoreCase("connect.2b2t.org")) return;
            if (Objects.equals(((AbstractSelectionList<?>) (Object) this).getSelected(), entry)) {
                int x0 = ((AbstractSelectionList<?>) (Object) this).getRowLeft();
                int y0 = entry.getY();
                int i0 = this.isFocused() ? -1 : -8355712;
                int l0 = (((AbstractSelectionList<?>)(Object) this).getX() + (this.width - (entryWidth0 + 32)) / 2) - 16;
                int j0 = (((AbstractSelectionList<?>)(Object) this).getX() + (this.width + (entryWidth0 + 32)) / 2) - 16;

                context.outline(x0, y0, x0 + entryWidth0, y0 + entryHeight0, i0);
                context.fill(l0, y0 - 2, j0, y0 + entryHeight0 + 2, i0);
                context.fill(l0 + 1, y0 - 1, j0 - 1, y0 + entryHeight0 + 1, -16777216);
                ((ServerSelectionList.OnlineServerEntry) entry).extractContent(context, mouseX, mouseY, entry.isMouseOver(mouseX, mouseY), delta);
            }
        }
    }

    @Unique
    @SuppressWarnings("unused")
    private void unusedKeepOldSignature() {
    }

    // Make the extended section of the entry which houses the clock hoverable and clickable
    @Inject(method = "getEntryAtPosition", at = @At("HEAD"), cancellable = true)
    private void extendSelectionSpaceFor2b2tClock(double x, double y, CallbackInfoReturnable<E> cir) {
        if (!StardustConfig.serverListWorldTimeClockSetting.get()) return;

        int i = this.getRowWidth() / 2;
        int j = ((AbstractSelectionList<?>)(Object) this).getX() + this.width / 2;
        int k = j - i;
        int l = j + i;
        int m = Mth.floor(y - (double) ((AbstractSelectionList<?>)(Object) this).getY()) - this.headerHeight + (int) this.scrollAmount() - 4;
        int n = m / this.defaultEntryHeight;

        E entry = (x >= (double) k && x <= (double) l && n >= 0 && m >= 0 && n < this.children.size())
            ? this.children.get(n) : null;

        if (entry != null) cir.setReturnValue(entry);
        else {
            x = x + 34;
            entry = (x >= (double) k && x <= (double) l && n >= 0 && m >= 0 && n < this.children.size())
                ? this.children.get(n) : null;

            if (entry instanceof ServerSelectionList.OnlineServerEntry) {
                ServerData info = ((ServerSelectionList.OnlineServerEntry) entry).getServerData();
                String name = info.name;
                String address = info.ip;
                if (name.toLowerCase().contains("2b2t") || address.equalsIgnoreCase("2b2t.org") || address.equalsIgnoreCase("connect.2b2t.org")) {
                    cir.setReturnValue(entry);
                } else {
                    cir.setReturnValue(null);
                }
            } else {
                cir.setReturnValue(null);
            }
        }
    }
}
