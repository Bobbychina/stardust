package dev.stardust.mixin;

import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import dev.stardust.util.StardustUtil;
import dev.stardust.modules.BookTools;
import net.minecraft.client.gui.screens.Screen;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.gui.components.Tooltip;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    // TODO(26.1): BookEditScreen 已无 dirty/signing 字段（书本编辑体系重做），改为 mixin 本地状态
    @Unique
    private boolean dirty;
    @Unique
    private boolean signing;

    // See BookTools.java
    protected BookEditScreenMixin(Component title) { super(title); }

    @Unique
    private boolean rainbowMode = false;
    @Unique
    private boolean didFormatPage = false;
    @Unique
    private String activeFormatting = "";
    @Unique
    private StardustUtil.RainbowColor lastCC = null;
    @Unique
    private final ArrayList<Button> buttons = new ArrayList<>();

    @Unique
    private void onClickColorButton(Button btn) {
        String color = btn.getMessage().getString().substring(0, 2);

        if (this.signing) {
            stardust$insert(true, color);
        } else {
            this.didFormatPage = true;
            stardust$insert(false, color);
        }
    }

    @Unique
    private void onClickFormatButton(Button btn) {
        String format = btn.getMessage().getString().substring(0, 2);

        if (rainbowMode) {
            activeFormatting = format;
        }else if (this.signing) {
            stardust$insert(true, format);
        } else {
            this.didFormatPage = true;
            stardust$insert(false, format);
        }
    }

    @Unique
    private void onClickRainbowButton(Button btn) {
        rainbowMode = !rainbowMode;
        if (rainbowMode) {
            btn.setMessage(Component.literal(uCC()+"🌈"));
            btn.setTooltip(Tooltip.create(Component.literal(uCC()+"R"+uCC()+"a"+uCC()+"i"+uCC()+"n"+uCC()+"b"+uCC()+"o"+uCC()+"w "+uCC()+"M"+uCC()+"o"+uCC()+"d"+uCC()+"e"+" §2On")));
        } else {
            btn.setMessage(Component.literal("🌈"));
            btn.setTooltip(Tooltip.create(Component.literal(uCC()+"R"+uCC()+"a"+uCC()+"i"+uCC()+"n"+uCC()+"b"+uCC()+"o"+uCC()+"w "+uCC()+"M"+uCC()+"o"+uCC()+"d"+uCC()+"e"+" §4Off")));
        }
    }

    @Unique
    private String uCC() {
        // Return a random color code that follows the pattern of the rainbow.
        if (lastCC == null) {
            lastCC = StardustUtil.RainbowColor.getFirst();
        } else {
            lastCC = StardustUtil.RainbowColor.getNext(lastCC);
        }
        return lastCC.labels[ThreadLocalRandom.current().nextInt(lastCC.labels.length)];
    }

    /** TODO(26.1): BookEditScreen 已无 TextFieldHelper 字段（见 PORT-NOTES R1），这里退化为安全空操作。 */
    @Unique
    private void stardust$insert(boolean title, String text) {
        // no-op: 26.1 无对应 TextFieldHelper，写入无法落到书页/标题上
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void mixinInit(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        BookTools bookTools = modules.get(BookTools.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (bookTools == null) return;

        if (bookTools.skipFormatting()) return;

        int offset = 0;
        boolean odd = false;
        for (StardustUtil.TextColor color : StardustUtil.TextColor.values()) {
            if (color.label.isEmpty()) continue;

            this.buttons.add(
                this.addRenderableWidget(
                    Button.builder(
                            Component.literal(color.label+"§l◼"),
                            this::onClickColorButton
                        )
                        .bounds(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 10, 10)
                        .tooltip(Tooltip.create(Component.literal("§7"+color.name().replace("_", " "))))
                        .build())
            );

            if (odd) offset += 12;
            odd = !odd;
        }

        for (StardustUtil.TextFormat format : StardustUtil.TextFormat.values()) {
            if (format.label.isEmpty()) continue;

            this.buttons.add(
                this.addRenderableWidget(
                    Button.builder(
                            Component.literal(format.label+"A"),
                            this::onClickFormatButton
                        )
                        .bounds(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 10, 10)
                        .tooltip(Tooltip.create(Component.literal("§7"+format.name())))
                        .build())
            );

            if (odd) offset += 12;
            odd = !odd;
        }

        this.buttons.add(
            this.addRenderableWidget(
                Button.builder(
                        Component.literal("§rA"),
                        this::onClickFormatButton
                    )
                    .bounds(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 10, 10)
                    .tooltip(Tooltip.create(Component.literal("§7Reset Formatting")))
                    .build()
            )
        );

        if (odd) offset += 12;
        odd = !odd;
        this.buttons.add(
            this.addRenderableWidget(
                Button.builder(
                        Component.literal("🌈"),
                        this::onClickRainbowButton
                    )
                    .bounds(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 22, 10)
                    .tooltip(Tooltip.create(Component.literal(uCC()+"R"+uCC()+"a"+uCC()+"i"+uCC()+"n"+uCC()+"b"+uCC()+"o"+uCC()+"w "+uCC()+"M"+uCC()+"o"+uCC()+"d"+uCC()+"e"+" §4Off")))
                    .build()
            )
        );
    }

    @Inject(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/TextFieldHelper;insertText(Ljava/lang/String;)V"), require = 0)
    private void mixinCharTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!rainbowMode || signing) return;
        didFormatPage = true;
        if (activeFormatting.equals("§r")) {
            activeFormatting = "";
            stardust$insert(false, "§r" + uCC());
        } else {
            stardust$insert(false, uCC() + activeFormatting);
        }
    }

    @Inject(method = "finalizeBook", at = @At("HEAD"), require = 0)
    private void mixinFinalizeBook(CallbackInfo ci) {
        if (this.dirty && this.didFormatPage) {
            stardust$insert(false, "§r");
        }
    }

    @Inject(method = "changePage", at = @At("HEAD"), require = 0)
    private void mixinChangePage(CallbackInfo ci) {
        this.didFormatPage = false;
    }

    @Inject(method = "updateButtonVisibility", at = @At("TAIL"))
    private void mixinUpdateButtons(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        BookTools bookTools = modules.get(BookTools.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (bookTools == null) return;
        if (bookTools.skipFormatting()) return;

        for (Button btn : this.buttons) {
            btn.visible = !signing || bookTools.shouldFormatTitles();
        }

        if (this.signing && !bookTools.autoTitles.get().trim().isEmpty()) {
            stardust$insert(true, bookTools.autoTitles.get());
        }
    }
}
