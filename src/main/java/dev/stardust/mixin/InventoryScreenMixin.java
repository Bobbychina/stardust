package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
import dev.stardust.modules.Loadouts;
import dev.stardust.util.StardustUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.gui.components.Tooltip;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu>
    implements RecipeUpdateListener {

    public InventoryScreenMixin(InventoryMenu handler, RecipeBookComponent<?> recipeBook, Inventory inventory, Component title) {
        super(handler, recipeBook, inventory, title);
    }

    @Unique private Loadouts loadouts = null;

    @Unique private Button saveLoadoutButton = null;

    @Unique private Button loadLoadoutButton = null;

    @Unique
    private void onSaveLoadoutButtonPress(Button btn) {
        if (loadouts == null) {
            Modules modules = Modules.get();
            if (modules == null ) return;
            loadouts = modules.get(Loadouts.class);

            if (loadouts == null) return;
        }
        loadouts.saveLoadout("quicksave");
        btn.setMessage(Component.literal(StardustUtil.rCC()+"§o✨§fSave"));
    }

    @Unique
    private void onLoadLoadoutButtonPress(Button btn) {
        if (loadouts == null) {
            Modules modules = Modules.get();
            if (modules == null ) return;
            loadouts = modules.get(Loadouts.class);

            if (loadouts == null) return;
        }
        loadouts.loadLoadout("quicksave");
        btn.setMessage(Component.literal("Load"+StardustUtil.rCC()+"§o✨"));
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void mixinInit(CallbackInfo ci) {
        if (loadouts == null) {
            Modules modules = Modules.get();
            if (modules == null ) return;
            loadouts = modules.get(Loadouts.class);

            if (loadouts == null) return;
        }

        if (!loadouts.quickLoadout.get()) return;
        saveLoadoutButton = this.addRenderableWidget(
            Button.builder(
                    Component.literal(StardustUtil.rCC()+"§o✨§fSave"),
                    this::onSaveLoadoutButtonPress
                )
                .bounds(this.width / 2 - 42, this.height / 2 + 83, 42, 16)
                .tooltip(Tooltip.create(Component.literal("§7§oSave your current inventory to Loadouts.")))
                .build()
        );

        loadLoadoutButton = this.addRenderableWidget(
            Button.builder(
                    Component.literal("Load"+StardustUtil.rCC()+"§o✨"),
                    this::onLoadLoadoutButtonPress
                )
                .bounds(this.width / 2, this.height / 2 + 83, 42, 16)
                .tooltip(Tooltip.create(Component.literal("§7§oLoad your quicksave loadout.")))
                .build()
        );

        if (saveLoadoutButton != null) saveLoadoutButton.visible = loadouts.isActive();
        if (loadLoadoutButton != null) loadLoadoutButton.visible = loadouts.isActive();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void mixinRender(CallbackInfo ci) {
        if (loadouts == null) {
            Modules modules = Modules.get();
            if (modules == null ) return;
            loadouts = modules.get(Loadouts.class);

            if (loadouts == null) return;
        }

        if (!loadouts.quickLoadout.get()) return;
        if (saveLoadoutButton != null) {
            saveLoadoutButton.visible = loadouts.isActive();
        }
        if (loadLoadoutButton != null) {
            loadLoadoutButton.visible = loadouts.isActive();
        }
    }

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void animateButtons(CallbackInfo ci) {
        if (loadouts == null) {
            Modules modules = Modules.get();
            if (modules == null ) return;
            loadouts = modules.get(Loadouts.class);

            if (loadouts == null) return;
        }

        if (!loadouts.quickLoadout.get()) return;
        if (loadouts.isActive() && !loadouts.isSorted) {
            if (saveLoadoutButton != null) saveLoadoutButton.setMessage(Component.literal(StardustUtil.rCC()+"§o✨§fSave"));
            if (loadLoadoutButton != null) loadLoadoutButton.setMessage(Component.literal("Load"+StardustUtil.rCC()+"§o✨"));
        }
    }
}
