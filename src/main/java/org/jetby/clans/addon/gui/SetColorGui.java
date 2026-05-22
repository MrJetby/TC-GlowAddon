package org.jetby.clans.addon.gui;


import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetby.clans.api.gui.Gui;
import org.jetby.clans.api.gui.GuiContext;
import org.jetby.clans.api.service.clan.member.Member;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.jetby.clans.addon.glow.Equipment;
import org.jetby.clans.addon.glow.Glow;
import org.jetby.clans.addon.model.GlowMember;
import org.jetby.libb.gui.parser.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SetColorGui extends Gui {

    private final UUID target;
    private final GlowMember glowMember;

    public SetColorGui(GuiContext ctx) {
        super(ctx);

        this.target = ctx.get(UUID.class);
        this.glowMember = Glow.getGlowMember(getViewer().getUniqueId());

        if (target==null) {
            setReplace("{glow_color}", Equipment.getColorName(glowMember.getDefaultColor()));
        } else {
            setReplace("{glow_color}", Equipment.getColorName(glowMember.getColor(target)));
        }

        addClickHandler("type", event -> {
            String type = event.getSection().getString("type");
            if (type == null || !type.startsWith("color-")) return;

            Color color = Equipment.getColorByName(type.replace("color-", ""));

            if (target != null) {
                glowMember.setColor(target, color);
                Player target = Bukkit.getPlayer(this.target);
                if (target != null) {
                    if (glowMember.isEnabled())
                        Glow.sendColorPacket(getViewer(), target, color);
                }
            } else {
                glowMember.setDefaultColor(color, true);
                for (Member member : getClan().getMembers()) {
                    Player target = Bukkit.getPlayer(member.getUuid());
                    if (target != null) {
                        if (glowMember.isEnabled())
                            Glow.sendColorPacket(getViewer(), target, color);
                    }
                }
            }
            getViewer().closeInventory();
        });
    }

    @Override
    public void buildItems(List<Item> items) {
        List<Item> result = new ArrayList<>();
        for (Item item : items) {
            if (item.section()!=null && item.section().contains("color")) {
                String colorName = item.section().getString("color");
                Color color = Equipment.getColorByName(colorName);

                ItemMeta meta = item.itemStack().getItemMeta();
                if (meta instanceof LeatherArmorMeta lam) {
                    lam.setColor(color);
                } else {
                    result.add(item);
                    continue;
                }
                item.itemStack().setItemMeta(meta);
                result.add(item);
                continue;
            }
            result.add(item);
        }
        super.buildItems(result);
    }

    @Override
    public void refresh() {
        if (target==null) {
            setReplace("{glow_color}", Equipment.getColorName(glowMember.getDefaultColor()));
        } else {
            setReplace("{glow_color}", Equipment.getColorName(glowMember.getColor(target)));
        }
        super.refresh();
    }
}