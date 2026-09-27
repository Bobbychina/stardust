package dev.stardust.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import dev.stardust.modules.RocketMan;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity
    implements Attackable {
    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Unique
    private RocketMan rm;

    // See RocketMan.java
    @Inject(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Ljava/lang/Math;sqrt(D)D"), require = 0)
    private void spoofPitchForSpeedCalcs(Vec3 oldVelocity, CallbackInfoReturnable<Vec3> cir, @Local(ordinal = 0) LocalFloatRef f, @Local(ordinal = 1)LocalRef<Vec3> rotationVec) {
        if (this.rm == null) {
            Modules modules = Modules.get();
            if (modules == null) return;
            rm = modules.get(RocketMan.class);
            // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
            if (rm == null) return;
        }

        if (!rm.isActive() || !rm.shouldLockYLevel()) return;
        if (!this.getUUID().equals(rm.getClientInstance().player.getUUID())) return;
        if (!rm.getClientInstance().player.isFallFlying()|| !rm.hasActiveRocket()) return;

        if (rm.getClientInstance().player.input.keyPresses.jump() && rm.verticalSpeed.get() > 0) {
            f.set(-45);
            rotationVec.set(net.minecraft.world.phys.Vec3.directionFromRotation(45.0f, this.getYRot()));
        } else if (rm.getClientInstance().player.input.keyPresses.shift() && rm.verticalSpeed.get() > 0) {
            f.set(45);
            rotationVec.set(net.minecraft.world.phys.Vec3.directionFromRotation(45.0f, this.getYRot()));
        } else {
            f.set(0);
            rotationVec.set(net.minecraft.world.phys.Vec3.directionFromRotation(0.0f, this.getYRot()));
        }
    }
}
