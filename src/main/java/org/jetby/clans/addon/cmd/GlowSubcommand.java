package org.jetby.clans.addon.cmd;


import org.jetby.clans.api.addons.commands.CommandService;
import org.jetby.clans.api.command.Subcommand;
import org.jetby.clans.api.service.clan.Clan;
import org.jetby.clans.api.service.clan.member.Member;
import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionExecute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetby.clans.addon.ClanGlow;
import org.jetby.clans.addon.glow.Glow;
import org.jetby.clans.addon.model.GlowMember;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class GlowSubcommand implements Subcommand {

    private final ClanGlow addon;

    public GlowSubcommand(ClanGlow addon) {
        this.addon = addon;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (!addon.getServiceManager().getClanManager().lookup().isInClan(player.getUniqueId())) {
                return true;
            }
            Clan clan = addon.getServiceManager().getClanManager().lookup().getClanByMember(player.getUniqueId());
            GlowMember glow = Glow.getGlowMember(player.getUniqueId());
            if (glow.isEnabled()) {
                addon.getGlow().removeObserver(player);
                ActionExecute.run(ActionContext.of(player, addon.getServiceManager().getPlugin()), addon.getConfig().getStringList("glow-off"));
            } else {
                Set<Member> members = new HashSet<>(clan.getMembers());
                if (clan.getMember(player.getUniqueId()) != clan.getLeader()) {
                    members.add(clan.getLeader());
                }
                members.remove(clan.getMember(player.getUniqueId()));
                addon.getGlow().addObserver(player, members);
                ActionExecute.run(ActionContext.of(player, addon.getServiceManager().getPlugin()), addon.getConfig().getStringList("glow-on"));
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabCompleter(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        return List.of();
    }

    @Override
    public CommandService.CommandType type() {
        return CommandService.CommandType.CLAN;
    }
}
