package dev.stardust.mixin;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Nameable;
import net.minecraft.world.phys.Vec3;
import dev.stardust.modules.RocketMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.commands.CommandSource;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import meteordevelopment.meteorclient.systems.modules.Modules;

@Mixin(Entity.class)
public abstract class EntityMixin
    implements Nameable, EntityAccess, CommandSource {

    @Shadow
    public abstract UUID getUUID();

    // See RocketMan.java
    @ModifyVariable(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), argsOnly = true)
    private Vec3 spoofYMovement(Vec3 velocity) {
        Modules modules = Modules.get();
        if (modules == null) return velocity;
        RocketMan rm = modules.get(RocketMan.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫
        if (rm == null) return velocity;
        if (!rm.isActive() || !rm.shouldLockYLevel()) return velocity;
        if (!this.getUUID().equals(rm.getClientInstance().player.getUUID())) return velocity;
        if (!rm.getClientInstance().player.isFallFlying() || !rm.hasActiveRocket()) return velocity;

        Vec3 spoofVec;
        if (rm.getClientInstance().player.input.keyPresses.jump()) {
            spoofVec = new Vec3(velocity.x, rm.verticalSpeed.get(), velocity.z);
        } else if (rm.getClientInstance().player.input.keyPresses.shift()) {
            spoofVec = new Vec3(velocity.x, -rm.verticalSpeed.get(), velocity.z);
        } else spoofVec = new Vec3(velocity.x, 0, velocity.z);

        return spoofVec;
    }
}
