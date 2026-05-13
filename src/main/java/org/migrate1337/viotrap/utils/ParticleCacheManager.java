package org.migrate1337.viotrap.utils;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.migrate1337.viotrap.VioTrap;

import java.util.*;

public class ParticleCacheManager {
    private final VioTrap plugin;

     
    private final Map<String, List<CachedPoint>> patternCache = new HashMap<>();
    private final Map<String, List<String>> animationCache = new HashMap<>();
    private final Map<UUID, String> activePlayerEffects = new HashMap<>();

    public ParticleCacheManager(VioTrap plugin) {
        this.plugin = plugin;
        reloadCache();
    }

    public void reloadCache() {
        patternCache.clear();
        animationCache.clear();
        activePlayerEffects.clear();

         
        ConfigurationSection patSec = plugin.getConfig().getConfigurationSection("custom_patterns");
        if (patSec != null) {
            for (String key : patSec.getKeys(false)) {
                List<String> pointsStr = patSec.getStringList(key);
                List<CachedPoint> points = new ArrayList<>();

                for (String p : pointsStr) {
                    try {
                        String[] data = p.split(":");
                        String[] coords = data[0].split(",");
                        double x = Double.parseDouble(coords[0]);
                        double y = Double.parseDouble(coords[1]);
                        double z = Double.parseDouble(coords[2]);

                         
                        String[] rgb = data.length > 1 ? data[1].split(",") : new String[]{"0", "255", "0"};
                        Color color = Color.fromRGB(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));

                        points.add(new CachedPoint(x, y, z, color));
                    } catch (Exception ignored) {}
                }
                patternCache.put(key, points);
            }
        }

         
        ConfigurationSection animSec = plugin.getConfig().getConfigurationSection("custom_animations");
        if (animSec != null) {
            for (String key : animSec.getKeys(false)) {
                animationCache.put(key, animSec.getStringList(key));
            }
        }

         
        ConfigurationSection playerSec = plugin.getConfig().getConfigurationSection("active_player_patterns");
        if (playerSec != null) {
            for (String uuidStr : playerSec.getKeys(false)) {
                activePlayerEffects.put(UUID.fromString(uuidStr), playerSec.getString(uuidStr));
            }
        }
    }

     
    public List<CachedPoint> getPattern(String name) { return patternCache.get(name); }
    public List<String> getAnimation(String name) { return animationCache.get(name); }
    public String getPlayerEffect(UUID uuid) { return activePlayerEffects.get(uuid); }
    public List<String> getAnimationNames() { return new ArrayList<>(animationCache.keySet()); }
}