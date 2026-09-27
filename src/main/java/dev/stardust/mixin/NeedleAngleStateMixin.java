package dev.stardust.mixin;

import java.time.Instant;
import dev.stardust.Stardust;
import net.minecraft.world.item.Items;
import dev.stardust.util.TimeUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.renderer.item.properties.numeric.NeedleDirectionHelper;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     See also: ServerEntryMixin.java && EntryListWidgetMixin.java && stardust.accesswidener && TimeUtil.java
 **/
@Mixin(NeedleDirectionHelper.class)
public abstract class NeedleAngleStateMixin {

    @Shadow
    protected abstract float calculate(ItemStack stack, ClientLevel world, int seed, net.minecraft.world.entity.ItemOwner user);

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void getCustomClockAngle(ItemStack stack, ClientLevel world, net.minecraft.world.entity.ItemOwner user, int seed, CallbackInfoReturnable<Float> cir) {
        if (Stardust.TIME == null) return;
        if (!stack.is(Items.CLOCK)) return;
        if (!StardustConfig.serverListWorldTimeClockSetting.get()) return;

        cir.cancel();
        // 26.1: get(...) 的第 3 参是 ItemOwner（旧 Yarn 是 LivingEntity）；ItemStack#getEntityRepresentation() 已删除，
        // 时钟物品本就没有实体表现，所以只判断 user 本身是不是实体
        Entity entity = (user instanceof Entity owner) ? owner : null;

        if (!(entity instanceof LivingEntity)) {
            TimeUtil.TimeData timeData = Stardust.TIME.getTime();

            if (timeData == null) {
                cir.setReturnValue(0f);
            } else cir.setReturnValue(getCustomClockAngle(timeData.lastUpdated(), timeData.worldTime()));
        } else {
            if (world == null && entity.level() instanceof ClientLevel clientWorld) {
                world = clientWorld;
            }

            if (world == null) {
                TimeUtil.TimeData timeData = Stardust.TIME.getTime();

                if (timeData == null) {
                    cir.setReturnValue(0f);
                } else cir.setReturnValue(getCustomClockAngle(timeData.lastUpdated(), timeData.worldTime()));
            } else {
                cir.setReturnValue(this.calculate(stack, world, seed, user));
            }
        }
    }

    @Unique
    private float getCustomClockAngle(String lastUpdatedIso, long worldTime) {
        long lastUpdatedMillis = Instant.parse(lastUpdatedIso).toEpochMilli();
        long nowMillis = System.currentTimeMillis();

        long elapsedTicks = Math.floorDiv(nowMillis - lastUpdatedMillis, 50L);
        long currentWorldTime = Math.floorMod(worldTime + elapsedTicks, 24000L);

        return getCustomSkyAngle(currentWorldTime);
    }

    @Unique
    private float getCustomSkyAngle(long time) {
        double d = Mth.frac(time / 24000.0 - 0.25);
        double e = 0.5 - Math.cos(d * Math.PI) / 2.0;

        return (float) ((d * 2.0 + e) / 3.0);
    }
}
