package dev.stardust.mixin.meteor;

import java.time.Instant;
import org.joml.Vector3d;
import java.time.Duration;
import org.lwjgl.glfw.GLFW;
import dev.stardust.util.MsgUtil;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.injection.At;
import meteordevelopment.meteorclient.settings.*;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.level.pathfinder.PathComputationType;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 *     Adds "Click-to-come" functionality to Meteor's built-in Freecam module.
 **/
@Mixin(value = Freecam.class, remap = false)
public abstract class FreecamMixin {
    @Shadow
    @Final
    private SettingGroup sgGeneral;

    @Shadow
    private CameraType perspective;
    @Shadow
    private double speedValue;
    @Shadow
    @Final
    public Vector3d prevPos;
    @Shadow
    @Final
    public Vector3d pos;

    @Shadow
    protected abstract boolean checkGuiMove();

    @Shadow
    private boolean up;
    @Shadow
    private boolean down;
    @Unique
    private int timer = 0;
    @Unique
    private int clicks = 0;
    @Unique
    private Instant clickedAt = null;
    @Unique
    private Setting<Boolean> clickToCome = null;
    @Unique
    private Setting<Boolean> useBaritoneChat = null;
    @Unique
    private Setting<String> baritoneChatPrefix = null;
    @Unique
    private Setting<Boolean> doubleClickToCome = null;
    @Unique
    private Setting<Boolean> satelliteCameraMode = null;
    @Unique
    private Setting<Double> orbitHeight = null;

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/render/Freecam;rotate:Lmeteordevelopment/meteorclient/settings/Setting;"))
    private void addClickToComeSettings(CallbackInfo ci) {
        clickToCome = sgGeneral.add(
            new BoolSetting.Builder()
                .name("click-to-come")
                .description("Click on a block while in freecam to path there with Baritone.")
                .defaultValue(false)
                .build()
        );

        doubleClickToCome = sgGeneral.add(
            new BoolSetting.Builder()
                .name("double-click-only")
                .description("Require a double-click to Baritone path to your crosshair target.")
                .defaultValue(false)
                .visible(() -> clickToCome != null && clickToCome.get())
                .build()
        );

        useBaritoneChat = sgGeneral.add(
            new BoolSetting.Builder()
                .name("use-baritone-chat")
                .description("Use Baritone chat commands instead of the internal API. For compatibility with standalone Baritone versions.")
                .defaultValue(false)
                .visible(() -> clickToCome != null && clickToCome.get())
                .build()
        );

        baritoneChatPrefix = sgGeneral.add(
            new StringSetting.Builder()
                .name("baritone-command-prefix")
                .description("What prefix to use for Baritone chat commands.")
                .defaultValue("#")
                .visible(() -> clickToCome != null && clickToCome.get() && useBaritoneChat != null && useBaritoneChat.get())
                .build()
        );

        satelliteCameraMode = sgGeneral.add(
            new BoolSetting.Builder()
                .name("satellite-camera")
                .description("Lock Freecam to the player's position sans the y-value, which you'll set yourself.")
                .defaultValue(false)
                .build()
        );
        orbitHeight = sgGeneral.add(
            new DoubleSetting.Builder()
                .name("orbit-height")
                .description("Height for the satellite camera to orbit at.")
                .defaultValue(169.0).min(-69.0).sliderMax(420.0)
                .build()
        );
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void handleClickToCome(CallbackInfo ci) {
        if (mc.player == null || mc.level == null) return;
        if (mc.screen == null && ((doubleClickToCome != null && !doubleClickToCome.get() && clicks >= 1) || clicks >= 2)) {
            clicks = 0;
            Direction side = null;
            BlockPos crosshairPos;
            if (mc.hitResult instanceof EntityHitResult) {
                crosshairPos = ((EntityHitResult) mc.hitResult).getEntity().blockPosition();
            } else {
                BlockHitResult result = ((BlockHitResult) mc.hitResult);
                if (mc.level.getBlockState(result.getBlockPos()).getBlock() instanceof AirBlock) {
                    Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
                    float pitch = mc.gameRenderer.getMainCamera().xRot();
                    float yaw = mc.gameRenderer.getMainCamera().yRot();

                    Vec3 direction = getRotationVector(pitch, yaw);
                    ClipContext context = new ClipContext(
                        cameraPos, cameraPos.add(direction.scale(256)),
                        ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.getCameraEntity()
                    );

                    BlockHitResult rayCast = mc.level.clip(context);
                    if (rayCast != null && !(mc.level.getBlockState(rayCast.getBlockPos()).getBlock() instanceof AirBlock)) {
                        crosshairPos = rayCast.getBlockPos();
                        side = rayCast.getDirection();
                    } else {
                        crosshairPos = result.getBlockPos();
                        side = result.getDirection();
                    }
                } else {
                    crosshairPos = result.getBlockPos();
                    side = result.getDirection();
                }
            }

            if (side != null) {
                // Try not to mine the block we clicked on
                if (side == Direction.DOWN) {
                    crosshairPos = crosshairPos.relative(side, 2);
                }else if (side == Direction.UP) {
                    crosshairPos = crosshairPos.relative(side);
                } else {
                    crosshairPos = crosshairPos.relative(side);
                    if (mc.level.getBlockState(crosshairPos.relative(Direction.DOWN)).isPathfindable(PathComputationType.LAND)) {
                        crosshairPos = crosshairPos.relative(Direction.DOWN);
                    }
                }
            }

            if (useBaritoneChat != null && useBaritoneChat.get() && baritoneChatPrefix != null && !baritoneChatPrefix.get().isBlank()) {
                mc.getConnection().sendChat(baritoneChatPrefix.get() + "goto "
                    + crosshairPos.getX() + " "
                    + crosshairPos.getY() + " "
                    + crosshairPos.getZ()
                );
            } else if (BaritoneUtils.IS_AVAILABLE) {
                PathManagers.get().stop();
                PathManagers.get().moveTo(crosshairPos);
                if (Modules.get().get(Freecam.class).chatFeedback) MsgUtil.sendModuleMsg("Baritone pathing to destination§a..!", "freecam");
            } else {
                MsgUtil.sendMsg("Baritone was not found to be installed. If this is a mistake, please enable the \"Use Baritone Chat\" setting and try again.");
            }
        }

        if (mc.screen == null && clicks > 0) {
            ++timer;
            if (timer >= 10) {
                timer = 0;
                clicks = 0;
                clickedAt = null;
            }
        }

        if (satelliteCameraMode == null || !satelliteCameraMode.get()) return;
        if (mc.getCameraEntity() == null || mc.getCameraEntity() == null) return;
        ci.cancel();

        if (mc.getCameraEntity().isInWall()) mc.getCameraEntity().noPhysics = true;
        if (!perspective.isFirstPerson()) mc.options.setCameraType(CameraType.FIRST_PERSON);

        double s = 0.5;
        double velY = 0;
        if (mc.options.keySprint.isDown()) s = 1;

        if (this.up) {
            velY += s * speedValue;
        }
        if (this.down) {
            velY -= s * speedValue;
        }

        Vec3 orbitPos = getOrbitPos(velY);

        prevPos.set(pos);
        pos.set(orbitPos.x, orbitPos.y, orbitPos.z);
    }

    // Allow RocketMan keyboard control to work & only cancel the shift/space preses for satellite cam
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void hijackOnKey(KeyInputEvent event, CallbackInfo ci) {
        if (Input.isKeyPressed(GLFW.GLFW_KEY_F3)) return;
        if (checkGuiMove()) return;
        if (satelliteCameraMode == null || !satelliteCameraMode.get()) return;

        ci.cancel();
        boolean cancel = true;
        if (mc.options.keyJump.matches(event.input)) {
            up = event.action != KeyAction.Release;
            mc.options.keyJump.setDown(false);
        }
        else if (mc.options.keyShift.matches(event.input)) {
            down = event.action != KeyAction.Release;
            mc.options.keyShift.setDown(false);
        } else {
            cancel = false;
        }

        if (cancel) event.cancel();
    }

    @Inject(method = "onMouseClick", at = @At("TAIL"))
    private void handleMouseClicks(MouseClickEvent event, CallbackInfo ci) {
        if (mc.screen != null) return;
        if (clickToCome == null || !clickToCome.get()) return;
        if (mc.options.keyAttack.matchesMouse(event.click)) {
            Instant now = Instant.now();
            if (clickedAt == null || Duration.between(clickedAt, now).toMillis() > 100) {
                clicks++;
                clickedAt = now;
            }
        }
    }

    @Inject(method = "onDeactivate", at = @At("TAIL"))
    private void resetCounters(CallbackInfo ci) {
        timer = 0;
        clicks = 0;
        clickedAt = null;
    }

    @Unique
    private Vec3 getRotationVector(float pitch, float yaw) {
        float f = pitch * ((float)Math.PI / 180);
        float g = -yaw * ((float)Math.PI / 180);
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    @Unique
    private Vec3 getOrbitPos(double velY) {
        if (orbitHeight != null) {
            orbitHeight.set(orbitHeight.get() + velY);
        }
        if (mc.player == null || orbitHeight == null) return new Vec3(0, orbitHeight == null ? velY : orbitHeight.get(), 0);
        return new Vec3(mc.player.getX(), orbitHeight.get(), mc.player.getZ());
    }
}
