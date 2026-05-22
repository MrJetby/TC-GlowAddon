package org.jetby.clans.addon.glow;


import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Equipment {


    private enum Equip {
        HELMET(EquipmentSlot.HELMET, org.bukkit.inventory.EquipmentSlot.HEAD),
        CHEST_PLATE(EquipmentSlot.CHEST_PLATE, org.bukkit.inventory.EquipmentSlot.CHEST),
        LEGGINGS(EquipmentSlot.LEGGINGS, org.bukkit.inventory.EquipmentSlot.LEGS),
        BOOTS(EquipmentSlot.BOOTS, org.bukkit.inventory.EquipmentSlot.FEET);
//        OFFHAND(EquipmentSlot.OFF_HAND, org.bukkit.inventory.EquipmentSlot.OFF_HAND),
//        HAND(EquipmentSlot.MAIN_HAND, org.bukkit.inventory.EquipmentSlot.HAND);

        public static final Equip[] VALUES = values();

        public final EquipmentSlot packet;
        public final org.bukkit.inventory.EquipmentSlot bukkit;

        Equip(EquipmentSlot packet, org.bukkit.inventory.EquipmentSlot bukkit) {
            this.packet = packet;
            this.bukkit = bukkit;
        }
    }

    public static void sendDefaultEquipment(Player source, Player target) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(source, packetFromPlayer(target));
    }

    @Contract("_ -> new")
    public static @NotNull WrapperPlayServerEntityEquipment packetFromPlayer(Player source) {
        return new WrapperPlayServerEntityEquipment(source.getEntityId(), equipmentFromPlayer(source));
    }

    public static @NotNull List<com.github.retrooper.packetevents.protocol.player.Equipment> equipmentFromPlayer(Player player) {
        List<com.github.retrooper.packetevents.protocol.player.Equipment> list = new ArrayList<>();

        EntityEquipment equipment = player.getEquipment();
        for (var slot : Equip.VALUES) {
            ItemStack item = equipment.getItem(slot.bukkit);

            list.add(new com.github.retrooper.packetevents.protocol.player.Equipment(slot.packet, SpigotConversionUtil.fromBukkitItemStack(item)));
        }

        return list;
    }

    public static List<com.github.retrooper.packetevents.protocol.player.Equipment> getEquipment(Color color) {
        return Equipment.withItemStacks(
                color,
                new ItemStack(Material.LEATHER_HELMET),
                new ItemStack(Material.LEATHER_CHESTPLATE),
                new ItemStack(Material.LEATHER_LEGGINGS),
                new ItemStack(Material.LEATHER_BOOTS)
        );
    }
    /**
     * Creates a list of {@link com.github.retrooper.packetevents.protocol.player.Equipment} objects from the provided {@link ItemStack}s.
     * <p>
     * Only the first four elements are considered, corresponding to the equipment slots:
     * <b>helmet</b>, <b>chestplate</b>, <b>leggings</b>, and <b>boots</b>.
     * Any additional {@code ItemStack}s beyond the fourth one are ignored.
     * </p>
     *
     * @param itemStacks an array of {@link ItemStack}s representing the player's armor items
     *                   (in order: helmet, chestplate, leggings, boots)
     * @return a list of {@link com.github.retrooper.packetevents.protocol.player.Equipment} objects mapped to their respective {@link EquipmentSlot}s
     * @author Jodex
     */
    public static @NotNull List<com.github.retrooper.packetevents.protocol.player.Equipment> withItemStacks(Color color, ItemStack... itemStacks) {
        List<com.github.retrooper.packetevents.protocol.player.Equipment> list = new ArrayList<>();
        for (int i = 0; i < Math.min(itemStacks.length, 4); i++) {
            ItemStack itemStack = itemStacks[i];
            ItemMeta meta = itemStack.getItemMeta();
            if (meta instanceof LeatherArmorMeta lam) {
                lam.setColor(color);
            }
            itemStack.setItemMeta(meta);
            list.add(new com.github.retrooper.packetevents.protocol.player.Equipment(Equip.VALUES[i].packet, SpigotConversionUtil.fromBukkitItemStack(itemStack)));
        }
        return list;
    }

    public static @NotNull String getColorName(@NotNull Color color) {

        if (color.equals(Color.WHITE)) return "WHITE";
        if (color.equals(Color.SILVER)) return "SILVER";
        if (color.equals(Color.GRAY)) return "GRAY";
        if (color.equals(Color.BLACK)) return "BLACK";
        if (color.equals(Color.RED)) return "RED";
        if (color.equals(Color.MAROON)) return "MAROON";
        if (color.equals(Color.YELLOW)) return "YELLOW";
        if (color.equals(Color.OLIVE)) return "OLIVE";
        if (color.equals(Color.LIME)) return "LIME";
        if (color.equals(Color.GREEN)) return "GREEN";
        if (color.equals(Color.AQUA)) return "AQUA";
        if (color.equals(Color.TEAL)) return "TEAL";
        if (color.equals(Color.BLUE)) return "BLUE";
        if (color.equals(Color.NAVY)) return "NAVY";
        if (color.equals(Color.FUCHSIA)) return "FUCHSIA";
        if (color.equals(Color.PURPLE)) return "PURPLE";
        if (color.equals(Color.ORANGE)) return "ORANGE";

        return "UNKNOWN";
    }
    public static @NotNull Color getColorByName(@Nullable String name) {
        if (name==null) return Color.WHITE;
        try {
            Field field = Color.class.getDeclaredField(name.toUpperCase());

            Object o = field.get(null);
            if (o instanceof Color color) {
                return color;
            }
        } catch (Exception ignored) {
        }

        return Color.WHITE;
    }
}
