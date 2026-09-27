package dev.stardust.mixin.xaero;

import xaero.common.HudMod;
import xaero.common.misc.Misc;
import dev.stardust.util.LogUtil;
import xaero.common.effect.Effects;
import dev.stardust.modules.Solitaire;
import dev.stardust.modules.Meteorites;
import dev.stardust.modules.Minesweeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import xaero.hud.minimap.module.MinimapSession;
import dev.stardust.gui.screens.SolitaireScreen;
import org.spongepowered.asm.mixin.injection.At;
import xaero.hud.minimap.module.MinimapRenderer;
import dev.stardust.gui.screens.MeteoritesScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import dev.stardust.gui.screens.MinesweeperScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import xaero.hud.render.module.ModuleRenderContext;
import org.spongepowered.asm.mixin.injection.Inject;
import xaero.common.minimap.render.MinimapRendererHelper;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Forces the minimap to remain rendered while playing in-client minigames.
 **/
@Mixin(value = MinimapRenderer.class, remap = false)
public class MinimapRendererMixin {
    @Unique
    private Solitaire solitaire = null;

    @Unique
    private Meteorites meteorites = null;

    @Unique
    private Minesweeper minesweeper = null;

    @Unique
    private static volatile Class<?> ISCREENBASE_CLASS_HANDLE = null;

    @Inject(
        method = "render(Lxaero/hud/minimap/module/MinimapSession;Lxaero/hud/render/module/ModuleRenderContext;Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V",
        at = @At("HEAD"), cancellable = true, remap = true
    )
    private void forceRenderMinimapDuringMinigames(MinimapSession session, ModuleRenderContext c, GuiGraphicsExtractor guiGraphics, float partialTicks, CallbackInfo ci) {
        if (mc == null) return;
        if (session.getProcessor().getNoMinimapMessageReceived()) return;
        if (Misc.hasEffect(mc.player, Effects.NO_MINIMAP) && Misc.hasEffect(mc.player, Effects.NO_MINIMAP_HARMFUL)) return;
        if (meteorites == null || minesweeper == null || solitaire == null) {
            Modules mods = Modules.get();
            if (mods == null) return;

            solitaire = mods.get(Solitaire.class);
            meteorites = mods.get(Meteorites.class);
            minesweeper = mods.get(Minesweeper.class);
            if (meteorites == null || minesweeper == null || solitaire == null) return;
        }

        boolean allowedByDefault = (!session.getHideMinimapUnderF3() || !mc.getDebugOverlay().showDebugScreen())
            && (!session.getHideMinimapUnderScreen() || mc.screen == null || isIScreenBaseInstance(mc.screen)
            || mc.screen instanceof ChatScreen || mc.screen instanceof DeathScreen);

        if (allowedByDefault) return;
        boolean force = mc.screen instanceof MeteoritesScreen && meteorites.renderMap.get();
        if (mc.screen instanceof SolitaireScreen && solitaire.renderMap.get()) force = true;
        if (mc.screen instanceof MinesweeperScreen && minesweeper.renderMap.get()) force = true;

        if (force) {
            ci.cancel();
            MinimapRendererHelper.restoreDefaultShaderBlendState();
            session.getProcessor().onRender(
                guiGraphics, c.x, c.y,c.screenWidth, c.screenHeight, c.screenScale,
                session.getConfiguredWidth(), c.w, partialTicks,
                HudMod.INSTANCE.getHudRenderer().getCustomVertexConsumers()
            );
            MinimapRendererHelper.restoreDefaultShaderBlendState();
        }
    }

    @Unique
    private static boolean isIScreenBaseInstance(Object screen) {
        if (screen == null) return false;
        Class<?> clazz = getIScreenBase();
        return clazz != null && clazz.isInstance(screen);
    }

    @Unique
    private static Class<?> getIScreenBase() {
        Class<?> cached = ISCREENBASE_CLASS_HANDLE;
        if (cached != null) return cached;

        String[] paths = {
            "xaero.common.gui.IScreenBase",
            "xaero.lib.client.gui.IScreenBase"
        };

        for (String name : paths) {
            try {
                Class<?> loaded = Class.forName(name, false, MinimapRendererMixin.class.getClassLoader());

                ISCREENBASE_CLASS_HANDLE = loaded;
                LogUtil.warn("Reflection attempt for " + name + " succeeded..!");

                return loaded;
            } catch (ClassNotFoundException ignored) {}
        }

        return null;
    }
}
