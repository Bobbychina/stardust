package dev.stardust.mixin;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import dev.stardust.modules.AntiToS;
import dev.stardust.util.StardustUtil;
import dev.stardust.modules.BookTools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
@Mixin(BookViewScreen.class)
public abstract class BookScreenMixin extends Screen {
    @Shadow private int currentPage;
    @Shadow
    @Mutable private int cachedPage;
        // 26.1: BookViewScreen 字段 contents → bookAccess
    @Shadow
    @Mutable
    private BookViewScreen.BookAccess bookAccess;

    // See BookTools.java && AntiToS.java
    protected BookScreenMixin(Component title) { super(title); }


    @Unique
    private boolean deobfuscated = false;
    @Unique
    private Button deobfuscateButton;
    @Unique
    private List<Component> obfuscatedPages = new ArrayList<>();

    @Unique
    private void stardust$deobfuscateBook(Button btn) {
        if (this.deobfuscated) {
            stardust$reobfuscateBook(btn);
            return;
        }

        if (this.bookAccess != null) {
            List<Component> pages = this.bookAccess.pages();
            List<Component> deobfuscatedPages = new java.util.ArrayList<>(List.of());
            for (Component page : pages) {
                deobfuscatedPages.add(Component.literal(page.getString().replace("§k", "")));
            }

            // 26.1: BookAccess 变成 record（不可变），改为整体替换一个新记录
            this.bookAccess = new BookViewScreen.BookAccess(deobfuscatedPages);

            btn.setAlpha(0.5f);
            btn.setTooltip(Tooltip.create(Component.literal("§8Restore this tome's secrets..")));
            this.cachedPage = -1;
            btn.setMessage(
                Component.literal("§0<"+StardustUtil.rCC()+"§o✨§r§0> "+StardustUtil.rCC()+"§o§kReobfuscate "+"§0<"
                    +StardustUtil.rCC()+"§o✨§r§0> ")
            );
            this.deobfuscated = true;
        }
    }

    @Unique
    private void stardust$reobfuscateBook(Button btn) {
        if (this.bookAccess != null) {
            btn.setAlpha(1f);
            btn.setTooltip(Tooltip.create(Component.literal("§8Reveal this tome's secrets..")));
            btn.setMessage(Component.literal("§0<§b§o✨§r§0> "+StardustUtil.rCC()+"§oDeobfuscate "+"§0<§a§o✨§r§0> "));

            this.bookAccess = new BookViewScreen.BookAccess(this.obfuscatedPages);
            if (!this.obfuscatedPages.get(this.cachedPage).getString().contains("§k")) {
                btn.visible = false;
            }

            this.cachedPage = -1;
            this.deobfuscated = false;
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void mixinInit(CallbackInfo ci) {
        if (!(this.bookAccess instanceof BookViewScreen.BookAccess)) return;

        Modules modules = Modules.get();
        if (modules == null) return;

        List<Component> pages = this.bookAccess.pages();
        AntiToS antiToS = modules.get(AntiToS.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (antiToS == null) return;
        BookTools bookTools = modules.get(BookTools.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (bookTools == null) return;
        if (antiToS.isActive()) {
            List<Component> filtered = new ArrayList<>();
            for (Component page : pages) {
                if (antiToS.containsBlacklistedText(page.getString())) {
                    filtered.add(Component.literal(antiToS.censorText(page.getString())));
                } else filtered.add(page);
            }
            this.bookAccess = new BookViewScreen.BookAccess(filtered);
            this.cachedPage = -1;
        } else if (bookTools.skipDeobfuscation()) return;

        this.deobfuscateButton = this.addRenderableWidget(
            Button.builder(
                    Component.literal("§0<§b§o✨§r§0> "+StardustUtil.rCC()+"§oDeobfuscate "+"§0<§a§o✨§r§0> "),
                    this::stardust$deobfuscateBook)
                .bounds(this.width / 2 - 59, 217, 120, 20)
                .tooltip(Tooltip.create(Component.literal("§8Reveal this tome's secrets..")))
                .build());

        if (!pages.isEmpty()) {
            this.deobfuscateButton.visible = pages.get(this.currentPage).getString().contains("§k");
        } else {
            this.deobfuscateButton.visible = false;
        }
        if (pages.stream().anyMatch(page -> page.getString().contains("§k"))) {
            this.obfuscatedPages = this.bookAccess.pages();
        }
    }

    @Inject(method = "updateButtonVisibility", at = @At("TAIL"))
    private void mixinUpdatePageButtons(CallbackInfo ci) {
        if (this.deobfuscated) return;
        if (!(this.bookAccess instanceof BookViewScreen.BookAccess)) return;

        Modules mods = Modules.get();
        if (mods == null) return;
        BookTools bookTools = mods.get(BookTools.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (bookTools == null) return;
        if (bookTools.skipDeobfuscation()) return;

        List<Component> pages = this.bookAccess.pages();
        if (!pages.isEmpty()) {
            this.deobfuscateButton.visible = pages.get(this.currentPage).getString().contains("§k");
        } else {
            this.deobfuscateButton.visible = false;
        }
    }
}
