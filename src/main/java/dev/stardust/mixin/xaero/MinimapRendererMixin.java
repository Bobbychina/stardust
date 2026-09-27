package dev.stardust.mixin.xaero;

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
import xaero.hud.minimap.render.MinimapPipRenderState;
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
            // 26.1: 旧 MinimapProcessor#onRender 新签名末位要求 xaerolib 的 XaeroBufferProvider
            //       （xaero.lib.* 不在编译类路径，无法具名传参），改用 Xaero 自己在
            //       MinimapRenderer#render 尾部调用的直绘路径 renderOutsidePip(...)，参数语义一致。
            // TODO(26.1): 若直绘路径漏画 pip 贴图/雷达层，需把 onRender 用反射或补 xaerolib 编译依赖重接。
            MinimapPipRenderState renderState = session.getProcessor().getRenderState();
            // renderState 在 Xaero 自己的 render() 里由 MinimapPipRenderState#update 填充（本帧尚未执行），
            // 取上一帧的值即可；未初始化时字段为 0，退化成 1.0 的等比缩放以免画出 0 尺寸地图。
            float minimapScale = renderState != null && renderState.getMinimapScale() > 0f
                ? renderState.getMinimapScale() : 1.0f;
            session.getProcessor().getRenderer().renderOutsidePip(
                session, c.x, c.y, c.screenWidth, c.screenHeight, c.screenScale,
                minimapScale, session.getConfiguredWidth(), partialTicks,
                guiGraphics
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
