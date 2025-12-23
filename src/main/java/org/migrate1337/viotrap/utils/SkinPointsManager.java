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
            }
        }

        this.pointsConfig = YamlConfiguration.loadConfiguration(this.pointsFile);
        this.loadPointsToCache();
    }

    private void loadPointsToCache() {
        for(String uuid : this.pointsConfig.getKeys(false)) {
            Map<String, Integer> skinPoints = new HashMap();

            for(String skin : this.pointsConfig.getConfigurationSection(uuid).getKeys(false)) {
                int points = this.pointsConfig.getInt(uuid + "." + skin);
                skinPoints.put(skin, points);
            }

            this.pointsCache.put(UUID.fromString(uuid), skinPoints);
        }

    }

    public void addPoints(UUID playerUUID, String skin, int amount) {
        Map<String, Integer> skinPoints = (Map)this.pointsCache.computeIfAbsent(playerUUID, (k) -> new HashMap());
        int newPoints = (Integer)skinPoints.getOrDefault(skin, 0) + amount;
        skinPoints.put(skin, newPoints);
        this.pointsConfig.set(playerUUID.toString() + "." + skin, newPoints);
        this.saveConfig();
    }

    public void removePoints(UUID playerUUID, String skin, int amount) {
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
        }

    }
}
