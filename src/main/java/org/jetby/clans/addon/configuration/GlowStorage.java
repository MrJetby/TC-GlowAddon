package org.jetby.clans.addon.configuration;

import org.bukkit.Color;
import org.jetby.clans.addon.glow.Glow;
import org.jetby.clans.addon.model.GlowMember;
import org.jetby.clans.api.TreexClansAPI;
import org.jetby.clans.api.storage.Storage;
import org.jetby.clans.api.storage.base.BaseSection;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class GlowStorage {

    private final Storage storage;
    private final Logger logger;

    public GlowStorage(Logger logger) {
        this.storage = TreexClansAPI.get().getStorage();
        this.logger = logger;
    }

    public void load() {
        Glow.OBSERVERS.clear();

        BaseSection base = storage.getSection().section("Glow");
        if (base == null) return;

        for (String key : base.keys().join()) {
            try {
                UUID uuid = UUID.fromString(key);
                GlowMember glow = new GlowMember(uuid);

                BaseSection section = base.section(key);

                Object defaultColor = section.get("default-color").join();
                if (defaultColor != null) {
                    int defaultRgb = section.getInt("default-color").join();
                    glow.setDefaultColor(Color.fromRGB(defaultRgb), false);
                }

                BaseSection colorsSection = section.section("colors");
                if (colorsSection != null) {
                    for (String colorKey : colorsSection.keys().join()) {
                        List<String> uuidsStr = colorsSection.getStringList(colorKey).join();
                        Color color = Color.fromRGB(Integer.parseInt(colorKey));
                        for (String uuidStr : uuidsStr) {
                            UUID target = UUID.fromString(uuidStr);
                            glow.setColor(target, color);
                        }
                    }
                }

                Glow.OBSERVERS.put(uuid, glow);

            } catch (Exception e) {
                logger.warning("Failed to load glow data for key '" + key + "': " + e.getMessage());
            }
        }
    }

    public void save() {
        BaseSection base = storage.getSection().section("Glow");

        for (Map.Entry<UUID, GlowMember> entry : Glow.OBSERVERS.entrySet()) {
            UUID observerUuid = entry.getKey();
            GlowMember glow = entry.getValue();

            BaseSection section = base.section(observerUuid.toString());

            section.set("default-color", glow.getDefaultColor().asRGB());

            BaseSection colorsSection = section.section("colors");
            Map<String, List<String>> grouped = new java.util.HashMap<>();
            for (Map.Entry<UUID, Color> glowEntry : glow.getColorMap().entrySet()) {
                UUID targetUuid = glowEntry.getKey();
                Color color = glowEntry.getValue();

                String colorKey = String.valueOf(color.asRGB());
                grouped.computeIfAbsent(colorKey, k -> new java.util.ArrayList<>())
                        .add(targetUuid.toString());
            }

            for (Map.Entry<String, List<String>> colorGroup : grouped.entrySet()) {
                colorsSection.set(colorGroup.getKey(), colorGroup.getValue());
            }
        }
    }
}