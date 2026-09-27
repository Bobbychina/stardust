package dev.stardust.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import dev.stardust.modules.RocketMan;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.player.LocalPlayer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Unique private long toggleTimestamp = System.currentTimeMillis();

    // See RocketMan.java
    @SuppressWarnings("UnnecessaryContinue")
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void allowRocketHover(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        RocketMan rm = modules.get(RocketMan.class);
        // 启动期模块可能未注册（get() 返回 null）→ 空守卫，避免在渲染/音效高频路径 NPE
        if (rm == null) return;
        if (!rm.isActive()) {
            while (rm.getClientInstance().options.keyDown.consumeClick()) { continue; }
            return;
        }

        Vec3 hoverVec = Vec3.ZERO;
        if (rm.getClientInstance().player == null) return;
        LocalPlayer player = rm.getClientInstance().player;
        if (rm.hoverMode.get().equals(RocketMan.HoverMode.Toggle) || rm.hoverMode.get().equals(RocketMan.HoverMode.Creative)) {
            if (player.input.keyPresses.left() && player.input.keyPresses.forward()) {
                Vec3 rotVec = Vec3.directionFromRotation(0, player.getYRot() - 45);
                hoverVec = rotVec.scale(rm.horizontalSpeed.get());
            } else if (player.input.keyPresses.right() && player.input.keyPresses.forward()) {
                Vec3 rotVec = Vec3.directionFromRotation(0, player.getYRot() + 45);
                hoverVec = rotVec.scale(rm.horizontalSpeed.get());
            } else if (player.input.keyPresses.left() && player.input.keyPresses.backward()) {
                Vec3 rotVec = Vec3.directionFromRotation(-180, player.getYRot() + 45);
                hoverVec = rotVec.scale(rm.horizontalSpeed.get());
            } else if (player.input.keyPresses.right() && player.input.keyPresses.backward()) {
                Vec3 rotVec = Vec3.directionFromRotation(-180, player.getYRot() - 45);
                hoverVec = rotVec.scale(rm.horizontalSpeed.get());
            } else if (player.input.keyPresses.left()) {
                Vec3 rotVec = Vec3.directionFromRotation(0, player.getYRot() - 90);
                hoverVec = rotVec.scale(rm.horizontalSpeed.get());
            } else if (player.input.keyPresses.right()) {
                Vec3 rotVec = Vec3.directionFromRotation(0, player.getYRot() + 90);
                hoverVec = rotVec.scale(rm.horizontalSpeed.get());
            } else if (player.input.keyPresses.forward()) {
                Vec3 rotVec = Vec3.directionFromRotation(0, player.getYRot());
                hoverVec = rotVec.scale(rm.horizontalSpeed.get());
            } else if (player.input.keyPresses.backward()) {
                Vec3 rotVec = Vec3.directionFromRotation(-180, player.getYRot());
                hoverVec = rotVec.scale(rm.horizontalSpeed.get());
            }

            if (rm.getClientInstance().player.input.keyPresses.jump() && rm.getClientInstance().player.input.keyPresses.shift()) {
                rm.getClientInstance().player.setShiftKeyDown(true);
            } else if (rm.getClientInstance().player.input.keyPresses.jump()) {
                rm.getClientInstance().player.setShiftKeyDown(false);
                hoverVec = hoverVec.add(0, rm.verticalSpeed.get(), 0);
            } else if (rm.getClientInstance().player.input.keyPresses.shift()) {
                rm.getClientInstance().player.setShiftKeyDown(false);
                hoverVec = hoverVec.add(0, -rm.verticalSpeed.get(), 0);
            }
        } else if (rm.hoverMode.get().equals(RocketMan.HoverMode.Hold)) {
            if (player.input.keyPresses.left()) {
                Vec3 rotVec = Vec3.directionFromRotation(0, player.getYRot() - 90);
                hoverVec = rotVec.scale(rm.horizontalSpeed.get());
            } else if (player.input.keyPresses.right()) {
                Vec3 rotVec = Vec3.directionFromRotation(0, player.getYRot() + 90);
                hoverVec = rotVec.scale(rm.horizontalSpeed.get());
            }
            if (rm.getClientInstance().player.input.keyPresses.jump()) {
                hoverVec = hoverVec.add(0, rm.verticalSpeed.get(), 0);
            } else if (rm.getClientInstance().player.input.keyPresses.shift()) {
                hoverVec = hoverVec.add(0, -rm.verticalSpeed.get(), 0);
            }
        } else if (rm.shouldLockYLevel()) {
            if (rm.getClientInstance().player.input.keyPresses.jump()) {
                hoverVec = hoverVec.add(0, rm.verticalSpeed.get(), 0);
            } else if (rm.getClientInstance().player.input.keyPresses.shift()) {
                hoverVec = hoverVec.add(0, -rm.verticalSpeed.get(), 0);
            }
        }

        if (!rm.hoverMode.get().equals(RocketMan.HoverMode.Off)) {
            switch (rm.hoverMode.get()) {
                case Hold -> {
                    if (rm.getClientInstance().player.isFallFlying()) {
                        if (rm.isHoverKeyPressed() && !rm.wasHovering) {
                            ci.cancel();
                            if (!rm.isHovering) rm.setIsHovering(true);
                            rm.getClientInstance().player.move(MoverType.SELF, hoverVec);
                        } else {
                            rm.setIsHovering(false);
                        }
                    }
                }
                case Toggle -> {
                    boolean processed = false;
                    boolean toggled = rm.isHovering;

                    long now = System.currentTimeMillis();
                    if (rm.isHoverKeyPressed() && now - toggleTimestamp >= 420) {
                        if (!toggled) {
                            toggled = true;
                            rm.setIsHovering(true);
                            if (rm.getClientInstance().player.isFallFlying()) {
                                ci.cancel();
                                rm.getClientInstance().player.move(MoverType.SELF, hoverVec);
                            }
                        } else {
                            toggled = false;
                            rm.setIsHovering(false);
                        }
                        processed = true;
                        toggleTimestamp = now;
                    }
                    if (!processed && toggled && rm.getClientInstance().player.isFallFlying()) {
                        ci.cancel();
                        rm.getClientInstance().player.move(MoverType.SELF, hoverVec);
                    }
                }
                case Creative -> {
                    if (rm.getClientInstance().player.isFallFlying()) {
                        ci.cancel();
                        if (!rm.isHovering) rm.setIsHovering(true);
                        if (player.input.keyPresses.forward() || player.input.keyPresses.right() || player.input.keyPresses.left()
                            || player.input.keyPresses.backward() || player.input.keyPresses.jump() || player.input.keyPresses.shift()) {
                            player.move(MoverType.SELF, hoverVec);
                        }

                        // consume key-presses for s, so we don't toggle hover mode off automatically if we switch back to toggle mode
                        while (rm.getClientInstance().options.keyDown.consumeClick()) {
                            continue;
                        }
                    }
                }
            } // consume irrelevant presses, so they don't trigger hover mode unintentionally
        } else while (rm.getClientInstance().options.keyDown.consumeClick()) { continue; }
    }
}
