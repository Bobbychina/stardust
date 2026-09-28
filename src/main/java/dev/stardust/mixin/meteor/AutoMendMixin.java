package dev.stardust.mixin.meteor;

import net.minecraft.world.item.Items;
import dev.stardust.util.MsgUtil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.injection.At;
import meteordevelopment.meteorclient.utils.Utils;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.player.AutoMend;
import meteordevelopment.meteorclient.systems.modules.player.EXPThrower;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 *     Adds wearable elytra mending to Meteor's built-in AutoMend module.
 **/
@Mixin(value = AutoMend.class, remap = false)
public abstract class AutoMendMixin extends Module {
    public AutoMendMixin(Category category, String name, String description) {
        super(category, name, description);
    }

    @Shadow
    private boolean didMove;
    @Shadow
    @Final
    private SettingGroup sgGeneral;
    @Shadow
    @Final
    private Setting<Boolean> autoDisable;
    @Shadow
    protected abstract int getEmptySlot();

    @Unique
    private boolean notified = false;
    @Unique
    private boolean didWearMending = false;
    @Unique
    private Setting<Boolean> auto = null;
    @Unique
    private Setting<Boolean> wearMendElytras = null;
    @Unique
    private Setting<Boolean> mendElytrasOnly = null;
    @Unique
    private Setting<Boolean> ignoreOffhand = null;

    @Unique
    private int timer = 0;

    @Unique
    private void stardust$replaceElytra() {
        if (mc.player == null) return;
        for (int n = 0; n < mc.player.getInventory().getNonEquipmentItems().size(); n++) {
            ItemStack stack = mc.player.getInventory().getItem(n);
            if (stack.getItem() == Items.ELYTRA) {
                if (Utils.hasEnchantment(stack, Enchantments.MENDING) && stack.getDamageValue() > 0) {
                    InvUtils.move().from(n).toArmor(2);
                    didWearMending = true;
                    return;
                }
            }
        }

        if (!notified) {
            if (mendElytrasOnly != null && mendElytrasOnly.get()
                && ignoreOffhand != null && ignoreOffhand.get() && autoDisable.get()) {
                if (stardust$getDamagedElytraSlot() == -1) {
                    toggle();
                    sendToggledMsg();
                    if (didWearMending) {
                        MsgUtil.sendModuleMsg("Repaired all elytras, disabling§a..§e!", this.name);
                    } else {
                        MsgUtil.sendModuleMsg("No repairable elytras left in inventory, disabling§3..§5!", this.name);
                    }
                }
            }
            notified = true;
        }
    }

    @Unique
    private int stardust$getDamagedElytraSlot() {
        if (mc.player == null) return -1;
        for (int n = 0; n < mc.player.getInventory().getNonEquipmentItems().size(); n++) {
            ItemStack stack = mc.player.getInventory().getItem(n);
            if (stack.getItem() == Items.ELYTRA) {
                if (Utils.hasEnchantment(stack, Enchantments.MENDING) && stack.getDamageValue() > 0) {
                    return n;
                }
            }
        }
        return -1;
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/player/AutoMend;autoDisable:Lmeteordevelopment/meteorclient/settings/Setting;"))
    private void addElytraMendSettings(CallbackInfo ci) {
        wearMendElytras = sgGeneral.add(
            new BoolSetting.Builder()
                .name("wear-mend-elytras")
                .description("Wear damaged mending elytras to mend them more efficiently.")
                .defaultValue(false)
                .build()
        );
        mendElytrasOnly = sgGeneral.add(
            new BoolSetting.Builder()
                .name("mend-elytras-only")
                .description("Only mend elytras, ignore other items.")
                .defaultValue(false)
                .build()
        );
        auto = sgGeneral.add(
            new BoolSetting.Builder()
                .name("auto-use-xp")
                .description("Uses ExpThrower to automatically throw bottles for you.")
                .defaultValue(false)
                .build()
        );
        ignoreOffhand = sgGeneral.add(
            new BoolSetting.Builder()
                .name("ignore-offhand")
                .description("Do not swap items into offhand for mending.")
                .defaultValue(false)
                .visible(() -> mendElytrasOnly != null && mendElytrasOnly.get())
                .build()
        );
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void hijackOnTick(CallbackInfo ci) {
        if (mc.player == null) return;
        if (wearMendElytras == null || !wearMendElytras.get()) return;
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || chest.getItem() != Items.ELYTRA || !Utils.hasEnchantment(chest, Enchantments.MENDING) || chest.getDamageValue() <= 0) {
            // momentarily pause EXPThrower to prevent inventory thrashing
            if (auto != null && auto.get() && Modules.get().isActive(EXPThrower.class)) Modules.get().get(EXPThrower.class).toggle();
            stardust$replaceElytra();
        }

        if (auto != null && auto.get() && !Modules.get().isActive(EXPThrower.class)) {
            ++timer;
            if (timer >= 20) {
                timer = 0;
                Modules.get().get(EXPThrower.class).toggle();
            }
        }

        if (ignoreOffhand != null && ignoreOffhand.get()) {
            ci.cancel();
        } else if (mendElytrasOnly != null && mendElytrasOnly.get()) {
            ci.cancel();
            ItemStack offhand = mc.player.getOffhandItem();
            if (offhand.isEmpty() || !Utils.hasEnchantment(offhand, Enchantments.MENDING) || offhand.getDamageValue() <= 0) {
                int slot = stardust$getDamagedElytraSlot();
                if (slot == -1) {
                    if (autoDisable.get()) {
                        if (didMove) {
                            MsgUtil.sendModuleMsg("Repaired all elytras, disabling§a..§e!", this.name);
                            int emptySlot = getEmptySlot();
                            if (emptySlot != -1) {
                                InvUtils.move().fromOffhand().to(emptySlot);
                            }
                        } else {
                            MsgUtil.sendModuleMsg("No repairable elytras left in inventory, disabling§3..§5!", this.name);
                        }
                        toggle();
                        sendToggledMsg();
                    }
                } else {
                    InvUtils.move().from(slot).toOffhand();
                    didMove = true;
                }
            }
        }
    }

    @Inject(method = "onActivate", at = @At("TAIL"))
    private void resetNotified(CallbackInfo ci) {
        notified = false;
        didWearMending = false;
    }
}
