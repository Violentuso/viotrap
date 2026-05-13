package org.migrate1337.viotrap.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.migrate1337.viotrap.VioTrap;

public class SkinPointsManager {
    private final VioTrap plugin;
    private final File pointsFile;
    private FileConfiguration pointsConfig;
    private final Map<UUID, Map<String, Integer>> pointsCache = new HashMap();

    public SkinPointsManager(VioTrap plugin) {
        this.plugin = plugin;
        this.pointsFile = new File(plugin.getDataFolder(), "skin_points.yml");
        this.loadConfig();
    }

    private void loadConfig() {
        if (!this.pointsFile.exists()) {
            try {
                this.pointsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Не удалось создать skin_points.yml: " + e.getMessage());
            }
        }

        this.pointsConfig = YamlConfiguration.loadConfiguration(this.pointsFile);
        this.loadPointsToCache();
    }

    private void loadPointsToCache() {
        for(String uuid : this.pointsConfig.getKeys(false)) {
            if (!this.pointsConfig.isConfigurationSection(uuid)) {
                continue;
            }

            UUID playerId;
            try {
                playerId = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Пропущен некорректный UUID в skin_points.yml: " + uuid);
                continue;
            }

            Map<String, Integer> skinPoints = new HashMap();
            org.bukkit.configuration.ConfigurationSection playerSection = this.pointsConfig.getConfigurationSection(uuid);

            for(String skin : playerSection.getKeys(false)) {
                int points = this.pointsConfig.getInt(uuid + "." + skin);
                skinPoints.put(skin, points);
            }

            this.pointsCache.put(playerId, skinPoints);
        }

    }

    public void addPoints(UUID playerUUID, String skin, int amount) {
        if (amount <= 0) {
            return;
        }
        Map<String, Integer> skinPoints = (Map)this.pointsCache.computeIfAbsent(playerUUID, (k) -> new HashMap());
        int newPoints = (Integer)skinPoints.getOrDefault(skin, 0) + amount;
        skinPoints.put(skin, newPoints);
        this.pointsConfig.set(playerUUID.toString() + "." + skin, newPoints);
        this.saveConfig();
    }

    public void removePoints(UUID playerUUID, String skin, int amount) {
        if (amount <= 0) {
            return;
        }
        Map<String, Integer> skinPoints = (Map)this.pointsCache.computeIfAbsent(playerUUID, (k) -> new HashMap());
        int currentPoints = (Integer)skinPoints.getOrDefault(skin, 0);
        if (currentPoints < amount) {
        } else {
            int newPoints = currentPoints - amount;
            if (newPoints <= 0) {
                skinPoints.remove(skin);
                this.pointsConfig.set(playerUUID.toString() + "." + skin, (Object)null);
            } else {
                skinPoints.put(skin, newPoints);
                this.pointsConfig.set(playerUUID.toString() + "." + skin, newPoints);
            }

            this.saveConfig();
        }
    }

    public int getPoints(UUID playerUUID, String skin) {
        return (Integer)((Map)this.pointsCache.getOrDefault(playerUUID, new HashMap())).getOrDefault(skin, 0);
    }

    private void saveConfig() {
        try {
            this.pointsConfig.save(this.pointsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить skin_points.yml: " + e.getMessage());
        }

    }
}
