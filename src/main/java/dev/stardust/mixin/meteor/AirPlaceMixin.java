package dev.stardust.mixin.meteor;

import org.lwjgl.glfw.GLFW;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.Vec3;
import dev.stardust.modules.RocketMan;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.injection.At;
import meteordevelopment.meteorclient.MeteorClient;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.player.AirPlace;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Adds an offhand bypass setting to Meteor's AirPlace, & allows holding ctrl+scroll-wheel to alter placement range.
 **/
@Mixin(value = AirPlace.class, remap = false)
public abstract class AirPlaceMixin extends Module {
    @Shadow
    @Final
    private SettingGroup sgGeneral;

    @Shadow
    @Final
    private Setting<Boolean> customRange;

    @Shadow
    @Final
    private Setting<Double> range;

    @Shadow
    private HitResult hitResult;

    public AirPlaceMixin(Category category, String name, String description) {
        super(category, name, description);
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Unique
    private int timer = 0;
    @Unique
    private boolean justUsed = false;
    @Unique
    private Setting<Boolean> bypass = null;

    @Unique
    @Override
    public void onDeactivate() {
        timer = 0;
        justUsed = false;
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/world/AirPlace;sgRange:Lmeteordevelopment/meteorclient/settings/SettingGroup;"))
    private void addSettings(CallbackInfo ci) {
        if (bypass == null) bypass = sgGeneral.add(
            new BoolSetting.Builder()
                .name("offhand-bypass")
                .description("Use a bypass for AirPlace on GrimAC.")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void hijackOnTick(CallbackInfo ci) {
        if (bypass == null || !bypass.get()) return;
        if (mc.getConnection() == null || mc.gameMode == null) return;
        if (mc.player == null || mc.getCameraEntity() == null || mc.level == null) return;

        if (justUsed) {
            ++timer;
            if (timer >= 5) {
                timer = 0;
                justUsed = false;
            }
        } else {
            if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return;

            double r = customRange.get() ? range.get() : mc.player.blockInteractionRange();
            hitResult = mc.getCameraEntity().pick(r, 0, false);

            if (!(hitResult instanceof BlockHitResult blockHit) || !mc.level.getBlockState(blockHit.getBlockPos()).isAir()) return;

            ci.cancel();
            if (mc.options.keyUse.isDown() && !justUsed) {
                justUsed = true;
                BlockPos pos = blockHit.getBlockPos();
                mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.OFF_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false), 0));

                mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.OFF_HAND));
                mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
            }
        }
    }

    @Unique
    @EventHandler
    private void onBlockInteract(InteractBlockEvent event) {
        if (bypass == null || !bypass.get() || hitResult == null) return;
        if (event.result.getBlockPos().closerThan(((BlockHitResult) hitResult).getBlockPos(), 1) && justUsed) {
            event.cancel();
        }
    }

    @Unique
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMouseScroll(MouseScrollEvent event) {
        if (mc.player == null) return;
        Modules mods = Modules.get();
        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return;
        if (mc.screen != null || mods == null || mods.get(Freecam.class).isActive()) return;
        if (mods.get(RocketMan.class).isActive() && mods.get(RocketMan.class).boostSpeed.get()) return;
        if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL)) {
            event.cancel();
            range.set(Mth.clamp(range.get() + event.value, 1, 6));
        }
    }
}
