package dev.stardust.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import dev.stardust.modules.RocketMan;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.entity.projectile.ItemSupplier;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.Constant;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(value = FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin implements ItemSupplier {

    @Shadow
    private LivingEntity attachedToEntity;

    @Unique
    private RocketMan rm;

    // See RocketMan.java
    @Inject(method = "tick", at = @At("HEAD"))
    private void createTrackedRocketEntity(CallbackInfo ci) {
        if (this.attachedToEntity == null) return;

        if (this.rm == null) {
            Modules modules = Modules.get();
            if (modules == null) return;
            rm = modules.get(RocketMan.class);
            // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
            if (rm == null) return;
        }

        if (!rm.getClientInstance().player.isFallFlying()) return;
        if (!this.attachedToEntity.getUUID().equals(rm.getClientInstance().player.getUUID())) return;
        if (!rm.isActive() || rm.currentRocket == (Object)this) return;

        LocalPlayer player = rm.getClientInstance().player;
        if (rm.currentRocket != null) {
            if (rm.currentRocket.getId() != ((FireworkRocketEntity)(Object)this).getId()) {
                rm.discardCurrentRocket("overwrite current");
                rm.currentRocket = (FireworkRocketEntity)(Object)this;
                rm.extensionStartPos = new BlockPos(player.getBlockX(), 0, player.getBlockZ());
            }
        } else {
            rm.currentRocket = (FireworkRocketEntity)(Object)this;
            rm.extensionStartPos = new BlockPos(player.getBlockX(), 0, player.getBlockZ());
            if (rm.debug.get()) player.sendSystemMessage(Component.literal("§7Created tracked rocket entity!"));
        }
    }

    // 26.1 共存：本侧的 @ModifyConstant 与 BepHax 争抢同一常量（mixin 报 @ModifyConstant conflict，本侧被跳过），
    // 已移除本侧实现；火箭加速由 BepHax 的 RocketMan 提供（见 docs/PORT-NOTES.md R9）

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
    private void spoofRotationVector(CallbackInfo ci, @Local(ordinal = 0) LocalRef<Vec3> rotationVec) {
        if (this.rm == null) {
            Modules modules = Modules.get();
            if (modules == null) return;
            rm = modules.get(RocketMan.class);
            // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
            if (rm == null) return;
        }
        if (!rm.isActive() || !rm.shouldLockYLevel()) return;
        if (!rm.getClientInstance().player.isFallFlying() || !rm.hasActiveRocket()) return;

        float g = -rm.getClientInstance().player.getYRot() * ((float)Math.PI / 180);
        float h = Mth.cos(g);
        float i = Mth.sin(g);

        rotationVec.set(new Vec3(i, -1, h));
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FireworkRocketEntity;explode(Lnet/minecraft/server/level/ServerLevel;)V"), cancellable = true)
    private void extendFireworkDuration(CallbackInfo ci) {
        if (this.rm == null) {
            Modules modules = Modules.get();
            if (modules == null) return;
            rm = modules.get(RocketMan.class);
            // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
            if (rm == null) return;
        }
        if (rm.currentRocket == null) return;
        if (!rm.isActive() || !rm.extendRockets.get()) return;
        if (rm.currentRocket.getId() != ((FireworkRocketEntity)(Object)this).getId()) return;
        if (rm.debug.get()) rm.getClientInstance().player.sendSystemMessage(Component.literal("§7Cancelling natural rocket expiration!"));
        ci.cancel();
    }
}
