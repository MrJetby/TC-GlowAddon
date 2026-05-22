package org.jetby.clans.addon.model;

import lombok.Getter;
import lombok.Setter;
import org.jetby.clans.api.service.clan.member.Member;
import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
public class GlowMember {
    private final UUID observer;
    @Setter
    private boolean enabled = false;

    private final Map<UUID, Color> colorMap = new HashMap<>();

    private Color defaultColor = Color.WHITE;

    public GlowMember(UUID observer) {
        this.observer = observer;
    }

    public void setColor(UUID target, Color color) {
        colorMap.put(target, color);

    }

    public void setDefaultColor(Color defaultColor, boolean clean) {
        if (clean) colorMap.clear();
        this.defaultColor = defaultColor;
    }

    public void setColor(Set<UUID> targets, Color color) {
        for (UUID target : targets) {
            colorMap.put(target, color);
        }
    }

    public void setColors(Set<Member> members, Color color) {
        for (Member target : members) {
            colorMap.put(target.getUuid(), color);
        }
    }

    public @NotNull Color getColor(@NotNull UUID target) {
        return colorMap.getOrDefault(target, Color.RED);
    }

    public Set<UUID> getTargets() {
        return colorMap.keySet();
    }
}
