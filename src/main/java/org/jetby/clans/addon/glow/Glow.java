package org.jetby.clans.addon.glow;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.jetby.clans.api.service.clan.Clan;
import org.jetby.clans.api.service.clan.member.Member;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetby.clans.addon.ClanGlow;
import org.jetby.clans.addon.model.GlowMember;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Glow implements PacketListener {

    private final ClanGlow addon;
    private final JavaPlugin plugin;

    public Glow(ClanGlow addon) {
        this.addon = addon;
        this.plugin = addon.getServiceManager().getPlugin();
    }

    public static final Map<UUID, GlowMember> OBSERVERS = new HashMap<>();

    @NotNull
    public static GlowMember getGlowMember(UUID uuid) {
        return OBSERVERS.computeIfAbsent(uuid, GlowMember::new);
    }

    public void addObserver(Player observer, Set<Member> targets) {
        if (!observer.isOnline()) return;
        Clan clan = addon.getServiceManager().getClanManager().lookup().getClanByMember(observer.getUniqueId());
        if (clan == null) return;

        GlowMember glow = getGlowMember(observer.getUniqueId());
        if (glow.isEnabled()) return;

        for (Member teamMember : targets) {
            UUID targetUuid = teamMember.getUuid();
            if (!glow.getColorMap().containsKey(targetUuid)) {
                glow.setColor(targetUuid, Color.RED);
            }
        }

        for (Member teamMember : targets) {
            Player target = Bukkit.getPlayer(teamMember.getUuid());
            if (target != null) {
                Color color = glow.getColor(target.getUniqueId());
                WrapperPlayServerEntityEquipment packet = createPacket(target.getEntityId(), color);
                PacketEvents.getAPI().getPlayerManager().sendPacket(observer, packet);
            }
        }
        glow.setEnabled(true);
    }

    public void removeObserver(Player observer) {
        GlowMember glow = OBSERVERS.get(observer.getUniqueId());
        if (glow==null) return;
        glow.setEnabled(false);
        for (UUID targetUuid : glow.getTargets()) {
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null) {
                Equipment.sendDefaultEquipment(observer, target);
            }
        }
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        PacketTypeCommon packetCommon = event.getPacketType();

        if (!(packetCommon instanceof Server type)) return;

        if (type != Server.SPAWN_ENTITY && type != Server.SPAWN_PLAYER && type != Server.ENTITY_EQUIPMENT) {
            return;
        }

        UUID observerUUID = event.getUser().getUUID();
        GlowMember glow = OBSERVERS.get(observerUUID);
        if (glow == null) return;
        if (!glow.isEnabled()) return;

        int entityId = getEntityId(packetCommon, event);
        if (entityId == -1) return;

        Player player = event.getPlayer();
        if (player == null) return;

        Entity entity = SpigotConversionUtil.getEntityById(player.getWorld(), entityId);
        if (!(entity instanceof Player target)) return;

        UUID targetUUID = target.getUniqueId();
        if (!glow.getTargets().contains(targetUUID)) return;

        Clan clan = addon.getServiceManager().getClanManager().lookup().getClanByMember(observerUUID);
        if (clan == null) return;

        Color color = glow.getColor(targetUUID);

        if (type == Server.ENTITY_EQUIPMENT) {
            Object buffer = createBuffer(event, color);
            event.setByteBuf(buffer);
        } else {
            WrapperPlayServerEntityEquipment packet = createPacket(entityId, color);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                    event.getUser().sendPacket(packet));
        }
    }

    private int getEntityId(@NotNull PacketTypeCommon common, @NotNull PacketSendEvent event) {
        Class<? extends PacketWrapper<?>> wrapperClass = common.getWrapperClass();
        if (wrapperClass == null) return -1;

        try {
            Constructor<? extends PacketWrapper<?>> constructor = wrapperClass.getDeclaredConstructor(PacketSendEvent.class);
            PacketWrapper<?> packetWrapper = constructor.newInstance(event);
            Method getEntityId = wrapperClass.getDeclaredMethod("getEntityId");
            return (int) getEntityId.invoke(packetWrapper);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error with getting entity id", e);
            return -1;
        }
    }

    private @NotNull WrapperPlayServerEntityEquipment createPacket(int entityId, Color color) {
        return new WrapperPlayServerEntityEquipment(entityId, Equipment.getEquipment(color));
    }

    private Object createBuffer(PacketSendEvent event, Color color) {
        WrapperPlayServerEntityEquipment wrapper = new WrapperPlayServerEntityEquipment(event);

        List<com.github.retrooper.packetevents.protocol.player.Equipment> original = wrapper.getEquipment();
        List<com.github.retrooper.packetevents.protocol.player.Equipment> merged = new ArrayList<>(original);

        Set<EquipmentSlot> armorSlots = Set.of(
                EquipmentSlot.HELMET,
                EquipmentSlot.CHEST_PLATE,
                EquipmentSlot.LEGGINGS,
                EquipmentSlot.BOOTS
        );

        List<com.github.retrooper.packetevents.protocol.player.Equipment> glowArmor = Equipment.getEquipment(color);
        Map<EquipmentSlot, com.github.retrooper.packetevents.protocol.player.Equipment> glowMap = new EnumMap<>(EquipmentSlot.class);
        for (var e : glowArmor) glowMap.put(e.getSlot(), e);

        merged.replaceAll(e -> armorSlots.contains(e.getSlot()) && glowMap.containsKey(e.getSlot())
                ? glowMap.get(e.getSlot())
                : e);

        wrapper.setEquipment(merged);
        wrapper.write();
        return wrapper.buffer;
    }
    public static void sendColorPacket(Player observer, Player target, Color color) {
        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(
                target.getEntityId(),
                Equipment.getEquipment(color)
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(observer, packet);
    }
}