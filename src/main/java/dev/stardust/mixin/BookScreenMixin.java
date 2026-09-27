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
import dev.stardust.mixin.accessor.BookScreenContentsAccessor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
@Mixin(BookViewScreen.class)
public abstract class BookScreenMixin extends Screen {
    @Shadow private int pageIndex;
    @Shadow
    @Mutable private int cachedPageIndex;
    @Shadow
    private BookViewScreen.BookAccess contents;

    // See BookTools.java && AntiToS.java
    protected BookScreenMixin(Component title) { super(title); }


    @Unique
    private boolean deobfuscated = false;
    @Unique
    private Button deobfuscateButton;
    @Unique
    private List<Component> obfuscatedPages = new ArrayList<>();

    @Unique
    private void deobfuscateBook(Button btn) {
        if (this.deobfuscated) {
            reobfuscateBook(btn);
            return;
        }

        if (contents instanceof BookViewScreen.BookAccess) {
            List<Component> pages = ((BookScreenContentsAccessor)(Object) contents).getPages();
            List<Component> deobfuscatedPages = new java.util.ArrayList<>(List.of());
            for (Component page : pages) {
                deobfuscatedPages.add(Component.literal(page.getString().replace("§k", "")));
            }

            ((BookScreenContentsAccessor)(Object) contents).setPages(deobfuscatedPages);

            btn.setAlpha(0.5f);
            btn.setTooltip(Tooltip.create(Component.literal("§8Restore this tome's secrets..")));
            this.cachedPageIndex = -1;
            btn.setMessage(
                Component.literal("§0<"+StardustUtil.rCC()+"§o✨§r§0> "+StardustUtil.rCC()+"§o§kReobfuscate "+"§0<"
                    +StardustUtil.rCC()+"§o✨§r§0> ")
            );
            this.deobfuscated = true;
        }
    }

    @Unique
    private void reobfuscateBook(Button btn) {
        if (contents instanceof BookViewScreen.BookAccess) {
            btn.setAlpha(1f);
            btn.setTooltip(Tooltip.create(Component.literal("§8Reveal this tome's secrets..")));
            btn.setMessage(Component.literal("§0<§b§o✨§r§0> "+StardustUtil.rCC()+"§oDeobfuscate "+"§0<§a§o✨§r§0> "));

            ((BookScreenContentsAccessor)(Object) contents).setPages(this.obfuscatedPages);
            if (!this.obfuscatedPages.get(this.cachedPageIndex).getString().contains("§k")) {
                btn.visible = false;
            }

            this.cachedPageIndex = -1;
            this.deobfuscated = false;
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void mixinInit(CallbackInfo ci) {
        if (!(this.contents instanceof BookViewScreen.BookAccess)) return;

        Modules modules = Modules.get();
        if (modules == null) return;

        List<Component> pages = ((BookScreenContentsAccessor)(Object) this.contents).getPages();
        AntiToS antiToS = modules.get(AntiToS.class);
        BookTools bookTools = modules.get(BookTools.class);
        if (antiToS.isActive()) {
            List<Component> filtered = new ArrayList<>();
            for (Component page : pages) {
                if (antiToS.containsBlacklistedText(page.getString())) {
                    filtered.add(Component.literal(antiToS.censorText(page.getString())));
                } else filtered.add(page);
            }
            ((BookScreenContentsAccessor)(Object) this.contents).setPages(filtered);
            this.cachedPageIndex = -1;
        } else if (bookTools.skipDeobfuscation()) return;

        this.deobfuscateButton = this.addDrawableChild(
            Button.builder(
                    Component.literal("§0<§b§o✨§r§0> "+StardustUtil.rCC()+"§oDeobfuscate "+"§0<§a§o✨§r§0> "),
                    this::deobfuscateBook)
                .bounds(this.width / 2 - 59, 217, 120, 20)
                .tooltip(Tooltip.create(Component.literal("§8Reveal this tome's secrets..")))
                .build());

        if (!pages.isEmpty()) {
            this.deobfuscateButton.visible = pages.get(this.pageIndex).getString().contains("§k");
        } else {
            this.deobfuscateButton.visible = false;
        }
        if (pages.stream().anyMatch(page -> page.getString().contains("§k"))) {
            this.obfuscatedPages = ((BookScreenContentsAccessor)(Object) this.contents).getPages();
        }
    }

    @Inject(method = "updatePageButtons", at = @At("TAIL"))
    private void mixinUpdatePageButtons(CallbackInfo ci) {
        if (this.deobfuscated) return;
        if (!(this.contents instanceof BookViewScreen.BookAccess)) return;

        Modules mods = Modules.get();
        if (mods == null) return;
        BookTools bookTools = mods.get(BookTools.class);
        if (bookTools.skipDeobfuscation()) return;

        List<Component> pages = ((BookScreenContentsAccessor)(Object) contents).getPages();
        if (!pages.isEmpty()) {
            this.deobfuscateButton.visible = pages.get(this.pageIndex).getString().contains("§k");
        } else {
            this.deobfuscateButton.visible = false;
        }
    }
}
