package org.jetby.clans.addon.configuration;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetby.clans.addon.glow.Glow;
import org.jetby.clans.addon.model.GlowMember;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class GlowStorage {

    private final File file;
    private final Logger logger;

    public GlowStorage(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "glow-data.yml");
        this.logger = logger;
    }

    public void load() {
        Glow.OBSERVERS.clear();
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                GlowMember glow = new GlowMember(uuid);

                ConfigurationSection section = config.getConfigurationSection(key);
                if (section == null) continue;

                if (section.contains("default-color")) {
                    int defaultRgb = section.getInt("default-color");
                    glow.setDefaultColor(Color.fromRGB(defaultRgb), false);
                }

                ConfigurationSection colorsSection = section.getConfigurationSection("colors");
                if (colorsSection != null) {
                    for (String colorKey : colorsSection.getKeys(false)) {
                        List<String> uuidsStr = colorsSection.getStringList(colorKey);
                        Color color = Color.fromRGB(Integer.parseInt(colorKey));
                        for (String uuidStr : uuidsStr) {
                            UUID target = UUID.fromString(uuidStr);
                            glow.setColor(target, color);
                        }
                    }
                }

                Glow.OBSERVERS.put(uuid, glow);

            } catch (Exception ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, GlowMember> entry : Glow.OBSERVERS.entrySet()) {
            UUID observerUuid = entry.getKey();
            GlowMember glow = entry.getValue();

            ConfigurationSection section = config.createSection(observerUuid.toString());

            section.set("default-color", glow.getDefaultColor().asRGB());

            ConfigurationSection colorsSection = section.createSection("colors");
            for (Map.Entry<UUID, Color> glowEntry : glow.getColorMap().entrySet()) {
                UUID targetUuid = glowEntry.getKey();
                Color color = glowEntry.getValue();

                String colorKey = String.valueOf(color.asRGB());
                List<String> list = colorsSection.getStringList(colorKey);
                list.add(targetUuid.toString());
                colorsSection.set(colorKey, list);
            }
        }

        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException e) {
            logger.severe("Failed to save glow-data.yml: " + e.getMessage());
        }
    }
}