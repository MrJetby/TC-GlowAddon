package org.jetby.clans.addon.gui;

import org.jetby.clans.api.gui.ClanGuiData;
import org.jetby.clans.api.gui.Gui;
import org.jetby.clans.api.gui.GuiContext;
import org.jetby.clans.api.service.clan.member.Member;
import org.jetby.libb.gui.item.ItemWrapper;
import org.jetby.libb.gui.parser.Item;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetby.clans.addon.ClanGlow;

import java.util.*;

public class ChooseGui extends Gui {

    public ChooseGui(@NotNull GuiContext ctx) {
        super(ctx);
        addClickHandler("type", event -> {

            String type = event.getItem().type();
            if (type==null) return;
            type = type.replace("player-", "");

            UUID uuid = UUID.fromString(type);

            ClanGuiData gui = ClanGlow.MANAGER.getGuiFactory().find(g -> "glow:setcolor".equalsIgnoreCase(g.getRenderer()));
            if (gui==null) {
                throw new RuntimeException("Can't find any gui with model glow:setcolor");
            }
            ClanGlow.MANAGER.getGuiFactory()
                    .create(GuiContext.of(
                                    getPlugin(),
                                    gui,
                                    getViewer(),
                                    getClan())
                            .with(uuid))
                    .open(player);
        });
    }

    @Override
    public void buildItems(List<Item> items) {
        if (getClan() == null) return;
        List<Item> result = new ArrayList<>();

        Item membersItem = null;

        for (Item item : items) {
            if ("players".equalsIgnoreCase(item.type())) {
                membersItem = item;
                continue;
            }
            result.add(item);
        }

        if (membersItem != null) {
            List<Integer> memberSlots = membersItem.slots();
            contentSlots(memberSlots.toArray(new Integer[0]));

            for (Member member : getClan().getMembersWithLeader()) {
                if (member==getClan().getMember(getViewer().getUniqueId())) continue;
                Item cloned = cloneItemForRank(membersItem, memberSlots, member);
                ItemWrapper wrapper = buildItemWrapper(cloned);
                addItem(wrapper);
            }
        }
        super.buildItems(result);
    }

    private Item cloneItemForRank(Item item, List<Integer> slots, Member member) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(member.getUuid());
        ItemStack clone = item.itemStack().clone();
        if (item.itemStack().getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clone.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(player);
            }
            clone.setItemMeta(meta);
        }

        Item copy = new Item(clone);
        copy.type("player-" + member.getUuid());
        copy.slots(slots);
        copy.flags(item.flags());
        copy.enchantments(item.enchantments());
        copy.enchanted(item.enchanted());
        copy.customModelData(item.customModelData());
        copy.onClick(item.onClick());
        copy.section(item.section());
        copy.viewRequirements(item.viewRequirements());
        copy.priority(item.priority());
        copy.displayName(item.displayName() == null ? null : applyRankPlaceholders(
                applyPlaceholders(item.displayName()), member));
        copy.lore(item.lore() == null ? null : item.lore()
                .stream()
                .map(line -> applyRankPlaceholders(applyPlaceholders(line), member))
                .toList());
        return copy;
    }

    private static String applyRankPlaceholders(String text, Member member) {
        for (Map.Entry<String, String> entry : placeholders(member).entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    private static Map<String, String> placeholders(Member member) {
        Map<String, String> placeholders = new HashMap<>();

        OfflinePlayer player = Bukkit.getOfflinePlayer(member.getUuid());
        placeholders.put("{name}", player.getName());
        placeholders.put("{target}", player.getName());

        return placeholders;
    }
}
