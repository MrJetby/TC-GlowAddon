package org.jetby.clans.addon;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import lombok.Getter;
import org.jetby.clans.api.addons.JavaAddon;
import org.jetby.clans.api.addons.annotations.ClanAddon;
import org.jetby.clans.api.addons.service.ServiceManager;
import org.jetby.clans.api.gui.ClanGuiData;
import org.jetby.clans.api.service.clan.Clan;
import org.jetby.clans.api.service.clan.member.Member;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetby.clans.addon.cmd.GlowSubcommand;
import org.jetby.clans.addon.configuration.GlowStorage;
import org.jetby.clans.addon.glow.Glow;
import org.jetby.clans.addon.gui.SetColorGui;
import org.jetby.clans.addon.gui.ChooseGui;
import org.jetby.clans.addon.model.GlowMember;

@ClanAddon(
        id = "Glow",
        version = "1.0"
)
@Getter
public class ClanGlow extends JavaAddon {

    public static ServiceManager MANAGER;

    private Glow glow;

    public static GlowStorage storage;
    @Override
    public void onEnable() {

        saveDefaultConfig();

        ClanGuiData chooseGui = getServiceManager().getGuiFactory().parse(getConfiguration("gui/choose.yml"));
        ClanGuiData setcolorGui = getServiceManager().getGuiFactory().parse(getConfiguration("gui/setcolor.yml"));

        getServiceManager().getGuiFactory().add(chooseGui);
        getServiceManager().getGuiFactory().add(setcolorGui);


        MANAGER = getServiceManager();
        PacketEvents.getAPI().getEventManager()
                .registerListener(glow = new Glow(this), PacketListenerPriority.LOW);

        storage = new GlowStorage(getDataFolder(), getLogger());
        storage.load();

        getServiceManager().getGuiFactory().register("choose", ChooseGui::new);
        getServiceManager().getGuiFactory().register("setcolor", SetColorGui::new);

        getServiceManager().getCommandService().registerCommand("glow", new GlowSubcommand(this));
    }

    @Override
    public void onDisable() {
        storage.save();
        getServiceManager().getGuiFactory().unregister("players");
        disableGlowForAll();

    }

    private void disableGlowForAll() {
        for (Clan clan : getServiceManager().getClanManager().getClanList().values()) {
            for (Member member : clan.getMembersWithLeader()) {
                Player player = Bukkit.getPlayer(member.getUuid());
                if (player != null) {
                    GlowMember glowMember = Glow.getGlowMember(player.getUniqueId());
                    if (glowMember.isEnabled()) {
                        glow.removeObserver(player);
                    }
                }
            }
        }
        Glow.OBSERVERS.clear();
    }
}