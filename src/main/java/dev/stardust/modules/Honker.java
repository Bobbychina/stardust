package dev.stardust.modules;

import java.util.Collection;
import dev.stardust.Stardust;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Instrument;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.sounds.SoundEvents;
import meteordevelopment.orbit.EventHandler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.multiplayer.PlayerInfo;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.player.LocalPlayer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.settings.ProvidedStringSetting;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Honker extends Module {
    public Honker() {
        super(Stardust.CATEGORY, "Honker", "Automatically use goat horns when a player enters your render distance.");
    }

    public static String[] horns = {"Admire", "Call", "Dream", "Feel", "Ponder", "Seek", "Sing", "Yearn", "Random"};

    private final Setting<String> desiredCall = settings.getDefaultGroup().add(
        new ProvidedStringSetting.Builder()
            .name("horn-preference")
            .description("Which horn to prefer using")
            .supplier(() -> horns)
            .defaultValue("Random")
            .build()
    );

    private final Setting<Boolean> ignoreFakes = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("ignore-fakes")
            .description("Ignore fake players created by modules like Blink.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> hornSpam = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("horn-spam")
            .description("Spam the desired horn as soon as it's done cooling down (every 7 seconds.)")
            .defaultValue(false)
            .onChanged(it -> { if (it) this.ticksSinceUsedHorn = 420; })
            .build()
    );

    private final Setting<Boolean> hornSpamAlone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("when-alone")
            .description("If you really want to, I guess..")
            .defaultValue(false)
            .visible(hornSpam::get)
            .onChanged(it -> { if (it) this.ticksSinceUsedHorn = 420; })
            .build()
    );

    private final Setting<Boolean> muteHorns = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("mute-horns")
            .description("Clientside mute for your own goat horns.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> muteAllHorns = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("mute-all-horns")
            .description("Mute everybody's horns, not just your own.")
            .visible(muteHorns::get)
            .defaultValue(false)
            .build()
    );

    private int ticksSinceUsedHorn = 0;
    private boolean needsMuting = false;

    private void honkHorn(int hornSlot, int activeSlot) {
        if (mc.gameMode == null) return;

        needsMuting = true;
        InvUtils.move().from(hornSlot).to(activeSlot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        InvUtils.move().from(activeSlot).to(hornSlot);
    }

    private void honkDesiredHorn() {
        if (mc.player == null) return;
        if ("Random".equals(desiredCall.get())) {
            IntArrayList hornSlots = new IntArrayList();
            for (int n = 0; n < mc.player.getInventory().getNonEquipmentItems().size(); n++) {
                ItemStack stack = mc.player.getInventory().getItem(n);
                if (stack.getItem() instanceof InstrumentItem) hornSlots.add(n);
            }
            if (hornSlots.isEmpty()) return;
            if (hornSlots.size() == 1) {
                honkHorn(hornSlots.getInt(0), mc.player.getInventory().getSelectedSlot());
            } else {
                int luckyIndex = (int) (Math.random() * hornSlots.size());
                honkHorn(hornSlots.getInt(luckyIndex), mc.player.getInventory().getSelectedSlot());
            }
        } else {
            String desiredCallId = desiredCall.get().toLowerCase() + "_goat_horn";

            int hornIndex = -1;
            for (int n = 0; n < mc.player.getInventory().getNonEquipmentItems().size(); n++) {
                ItemStack stack = mc.player.getInventory().getItem(n);
                if (!(stack.getItem() instanceof InstrumentItem)) continue;
                if (!stack.has(DataComponents.INSTRUMENT)) continue;
                Holder<Instrument> instrument = stack.get(DataComponents.INSTRUMENT);
                String id = instrument.value().soundEvent().value().location().toUnderscoreSeparatedString();
                if (id == null) continue;

                hornIndex = n;
                if (id.equals("minecraft:"+desiredCallId)) break;
            }

            if (hornIndex != -1) {
                honkHorn(hornIndex, mc.player.getInventory().getSelectedSlot());
            }
        }
    }

    // See GoatHornItemMixin.java
    public boolean shouldMuteHorns() {
        return muteHorns.get() && needsMuting || muteHorns.get() && muteAllHorns.get();
    }

    @Override
    public void onActivate() {
        if (mc.level == null) return;
        if (hornSpam.get()) {
            ticksSinceUsedHorn = 0;
            return;
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player && !(entity instanceof LocalPlayer)) {
                if (ignoreFakes.get()) {
                    Collection<PlayerInfo> players = mc.player.connection.getOnlinePlayers();
                    if (players.stream().noneMatch(entry -> entry.getProfile().getId().equals(entity.getUUID()))) continue;
                }
                honkDesiredHorn();
                break;
            }
        }
        needsMuting = false;
        ticksSinceUsedHorn = 0;
    }

    @EventHandler
    private void onEntityAdd(EntityAddedEvent event) {
        if (hornSpam.get() || mc.player == null) return;
        if (!(event.entity instanceof Player player)) return;
        if (player instanceof LocalPlayer) return;

        if (ignoreFakes.get()) {
            Collection<PlayerInfo> players = mc.player.connection.getOnlinePlayers();
            if (players.stream().noneMatch(entry -> entry.getProfile().getId().equals(player.getUUID()))) return;
        }

        if (!hornSpam.get()) {
            honkDesiredHorn();
            needsMuting = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!hornSpam.get() || mc.player == null || mc.level == null) return;

        boolean playerNearby = false;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player && !(entity instanceof LocalPlayer)) {
                if (ignoreFakes.get()) {
                    Collection<PlayerInfo> players = mc.player.connection.getOnlinePlayers();
                    if (players.stream().noneMatch(entry -> entry.getProfile().getId().equals(entity.getUUID()))) continue;
                }
                playerNearby = true;
                break;
            }
        }

        if (!playerNearby && !hornSpamAlone.get()) return;
        ItemStack activeItem = mc.player.getActiveItem();
        if (activeItem.has(DataComponents.FOOD) || Utils.isThrowable(activeItem.getItem()) && mc.player.getTicksUsingItem() > 0) return;

        ++ticksSinceUsedHorn;
        if (ticksSinceUsedHorn > 150) {
            honkDesiredHorn();
            needsMuting = false;
            ticksSinceUsedHorn = 0;
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundSoundEntityPacket) || !shouldMuteHorns()) return;

        SoundEvent soundEvent = ((ClientboundSoundEntityPacket) event.packet).getSound().value();

        for (int n = 0; n < SoundEvents.GOAT_HORN_VARIANT_COUNT; n++) {
            if (soundEvent == SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(n).value()) {
                event.cancel();
                break;
            }
        }
    }
}
