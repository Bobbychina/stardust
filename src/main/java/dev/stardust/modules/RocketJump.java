package dev.stardust.modules;

import dev.stardust.Stardust;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import dev.stardust.util.MsgUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.tags.ItemTags;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class RocketJump extends Module {
    public RocketJump() {super(Stardust.CATEGORY, "RocketJump", "Rocket-boosted jumps (requires an elytra)."); }

    private final Setting<Boolean> preferChestplate = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("prefer-chestplate")
            .description("If enabled, will always try to re-equip a chestplate no matter what you were wearing when the module was enabled.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> swapBackTicks = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("swap-delay-ticks")
            .min(0).sliderRange(0, 20)
            .defaultValue(7)
            .build()
    );

    private int timer = -1;
    private int jumpTimer = -1;
    private int swapSlot = -1;
    private boolean jumped = false;
    private boolean jumping = false;
    private Item chestplate = null;

    private void starFlying() {
        if (mc.player == null || mc.getConnection() == null) return;
        mc.player.startFallFlying();
        mc.options.keyJump.setDown(true);
        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
    }

    private void useRocket() {
        if (mc.player == null) return;
        int rocketSlot = getRocketSlot();
        if (rocketSlot == -1) {
            MsgUtil.sendModuleMsg("No rockets found§c..!", this.name);
            toggle();
            sendToggledMsg();
            return;
        }
        if (rocketSlot != mc.player.getInventory().getSelectedSlot()) {
            if (rocketSlot < 9) InvUtils.swap(rocketSlot, true);
            else {
                InvUtils.move().from(rocketSlot).to(mc.player.getInventory().getSelectedSlot());
            }
        }
        if (mc.gameMode != null) mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        if (rocketSlot < 9) InvUtils.swapBack();
        else InvUtils.move().from(mc.player.getInventory().getSelectedSlot()).to(rocketSlot);
    }

    private int getRocketSlot() {
        FindItemResult rockets = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!rockets.found()) {
            rockets = InvUtils.find(Items.FIREWORK_ROCKET);
            if (!rockets.found()) {
                return -1;
            }
        }

        return rockets.slot();
    }

    private boolean hasActiveRocket() {
        if (mc.level == null) return false;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof FireworkRocketEntity r && r.getOwner() != null && r.getOwner().equals(mc.player)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onActivate() {
        if (!Utils.canUpdate()) {
            toggle();
            return;
        }
        if (getRocketSlot() == -1) {
            MsgUtil.sendModuleMsg("No rockets found§c..!", this.name);
            toggle();
            sendToggledMsg();
            return;
        }

        jumping = true;
    }

    @Override
    public void onDeactivate() {
        timer = -1;
        swapSlot = -1;
        jumpTimer = -1;
        jumped = false;
        jumping = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || !jumping) return;
        boolean wearingSomething = !mc.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty();

        if (!jumped && wearingSomething) {
            boolean wearingElytra = mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                && mc.player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() < Items.ELYTRA.getDefaultInstance().getMaxDamage();

            if (wearingElytra) {
                if (mc.player.isFallFlying()) {
                    jumped = true;
                    swapSlot = -69;
                    mc.options.keyJump.setDown(false);
                    if (!hasActiveRocket()) useRocket();
                    if (timer == -1) {
                        timer = swapBackTicks.get();
                    }
                } else if (mc.player.onGround()) {
                    mc.options.keyJump.setDown(true);
                    return;
                } else {
                    ++jumpTimer;
                    if (jumpTimer == 0) {
                        mc.options.keyJump.setDown(false);
                    }else if (jumpTimer > 0) {
                        jumpTimer = -1;
                        starFlying();
                    }

                    return;
                }
            } else {
                chestplate = mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem();
                FindItemResult elytra = InvUtils.find(stack -> stack.is(Items.ELYTRA) && stack.getDamageValue() < stack.getMaxDamage());
                if (!elytra.found()) {
                    MsgUtil.sendModuleMsg("No good elytra found§c..!", this.name);
                    toggle();
                    sendToggledMsg();
                    return;
                }

                swapSlot = elytra.slot();
                InvUtils.move().from(elytra.slot()).toArmor(2);
                return;
            }
        } else if (!jumped) {
            FindItemResult elytra = InvUtils.find(stack -> stack.is(Items.ELYTRA) && stack.getDamageValue() < stack.getMaxDamage());
            if (!elytra.found()) {
                MsgUtil.sendModuleMsg("No good elytra found§c..!", this.name);
                toggle();
                sendToggledMsg();
                return;
            }

            if (preferChestplate.get()) {
                boolean found = false;
                if (chestplate != null) for (int n = 0; n < mc.player.getInventory().getNonEquipmentItems().size(); n++) {
                    ItemStack stack = mc.player.getInventory().getItem(n);
                    if (stack.is(chestplate)) {
                        swapSlot = n;
                        found = true;
                        break;
                    }
                }
                if (!found) for (int n = 0; n < mc.player.getInventory().getNonEquipmentItems().size(); n++) {
                    ItemStack stack = mc.player.getInventory().getItem(n);
                    if (stack.is(ItemTags.CHEST_ARMOR)) {
                        swapSlot = n;
                        found = true;
                        break;
                    }
                }
                if (!found) swapSlot = elytra.slot();
            } else {
                swapSlot = elytra.slot();
            }
            InvUtils.move().from(elytra.slot()).toArmor(2);
            return;
        }

        if (swapSlot != -1) {
            --timer;
            if (timer <= 0) {
                if (swapSlot == -69) {
                    if (preferChestplate.get()) {
                        if (chestplate != null) for (int n = 0; n < mc.player.getInventory().getNonEquipmentItems().size(); n++) {
                            ItemStack stack = mc.player.getInventory().getItem(n);
                            if (stack.is(chestplate)) {
                                InvUtils.move().fromArmor(2).to(n);
                                toggle();
                                sendToggledMsg();
                                return;
                            }
                        }
                        for (int n = 0; n < mc.player.getInventory().getNonEquipmentItems().size(); n++) {
                            ItemStack stack = mc.player.getInventory().getItem(n);
                            if (stack.is(ItemTags.CHEST_ARMOR)) {
                                InvUtils.move().fromArmor(2).to(n);
                                toggle();
                                sendToggledMsg();
                                return;
                            }
                        }
                    }

                    swapSlot = -420;
                    InvUtils.click().slotArmor(2);
                } else if (swapSlot == -420) {
                    InvUtils.click().slotArmor(2);
                    toggle();
                    sendToggledMsg();
                } else {
                    InvUtils.move().fromArmor(2).to(swapSlot);
                    toggle();
                    sendToggledMsg();
                }
            }
        }
    }
}
