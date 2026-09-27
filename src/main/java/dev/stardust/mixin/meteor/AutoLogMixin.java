package dev.stardust.mixin.meteor;

import net.minecraft.network.chat.Component;
import dev.stardust.util.LogUtil;
import dev.stardust.util.StardustUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import meteordevelopment.orbit.EventHandler;
import org.spongepowered.asm.mixin.injection.At;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.MeteorClient;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import dev.stardust.mixin.accessor.DisconnectS2CPacketAccessor;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.combat.AutoLog;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 *     Adds "Illegal Disconnect" functionality to Meteor's built-in AutoLog module.
 **/
@Mixin(value = AutoLog.class, remap = false)
public abstract class AutoLogMixin extends Module {
    public AutoLogMixin(Category category, String name, String description) {
        super(category, name, description);
    }

    @Shadow
    @Final
    private SettingGroup sgGeneral;
    @Shadow
    @Final
    private Setting<Boolean> toggleOff;
    @Shadow
    @Final
    private Setting<Boolean> smartToggle;

    @Unique
    private boolean didLog = false;
    @Unique
    private long requestedDcAt = 0L;
    @Unique
    private Component disconnectReason = null;
    @Unique
    private Setting<Boolean> forceKick = null;

    @Override
    public void onDeactivate() {
        if (toggleOff.get() || smartToggle.get() && didLog) {
            MeteorClient.EVENT_BUS.subscribe(this);
        }
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/misc/AutoLog;entities:Lmeteordevelopment/meteorclient/settings/Setting;"))
    private void addIllegalDisconnectSetting(CallbackInfo ci) {
        forceKick = sgGeneral.add(
            new BoolSetting.Builder()
                .name("illegal-disconnect")
                .description("Tip: Change the illegal disconnect method in your Meteor config settings (Stardust category.)")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method = "onTick",at = @At("HEAD"), cancellable = true)
    private void mixinOnTick(CallbackInfo ci) {
        if (!Utils.canUpdate() || !isActive()) ci.cancel();
        if (didLog && System.currentTimeMillis() - requestedDcAt >= 1337) {
            LogUtil.warn("Detected illegal disconnect failure, falling back on regular disconnect (try adjusting your illegal disconnect method config setting).");
            if (mc.getConnection() != null) mc.getConnection().onDisconnect(new DisconnectionDetails(disconnectReason));
            disconnectReason = null;
            didLog = false;
            requestedDcAt = 0L;
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true, remap = true)
    private void maybeIllegalDisconnect(Component reason, CallbackInfo ci) {
        if (forceKick != null && forceKick.get()) {
            ci.cancel();
            didLog = true;
            requestedDcAt = System.currentTimeMillis();
            disconnectReason = Component.literal("§8[§a§oAutoLog§8] §f" + reason.getString());
            StardustUtil.illegalDisconnect(true, StardustConfig.illegalDisconnectMethodSetting.get());
        }
    }

    @Unique
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (disconnectReason == null || !(event.packet instanceof ClientboundDisconnectPacket packet))  return;
        if (didLog) {
            ((DisconnectS2CPacketAccessor)(Object) packet).setReason(disconnectReason);
            if (!isActive()) MeteorClient.EVENT_BUS.unsubscribe(this);
            disconnectReason = null;
            didLog = false;
        }
    }
}
