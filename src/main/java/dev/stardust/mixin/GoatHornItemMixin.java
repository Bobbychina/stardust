package dev.stardust.mixin;

import net.minecraft.world.item.Item;
import dev.stardust.modules.Honker;
import net.minecraft.world.item.InstrumentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(InstrumentItem.class)
public class GoatHornItemMixin extends Item {
    // See Honker.java
    public GoatHornItemMixin(Item.Properties settings) {
        super(settings);
    }

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private static void mixinPlaySound(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        Honker honker = modules.get(Honker.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (honker == null) return;
        if (honker.shouldMuteHorns()) ci.cancel();
    }
}
