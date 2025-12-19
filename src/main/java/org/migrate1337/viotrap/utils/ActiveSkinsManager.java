package org.migrate1337.viotrap.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.migrate1337.viotrap.VioTrap;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ActiveSkinsManager {
    private final VioTrap plugin;
    private File activeSkinsFile;
    private FileConfiguration activeSkins; // ← вот это было пропущено!

    public ActiveSkinsManager(VioTrap plugin) {
        this.plugin = plugin;
        this.activeSkinsFile = new File(plugin.getDataFolder(), "active_skins.yml");
        this.loadConfig();
    }

    private void loadConfig() {
        if (!activeSkinsFile.exists()) {
            try {
                activeSkinsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать active_skins.yml: " + e.getMessage());
            }
        }
        this.activeSkins = YamlConfiguration.loadConfiguration(activeSkinsFile);
    }

    /** Установить активный скин трапки (заменяет старый) */
    public void setActiveTrapSkin(UUID playerUUID, String skin) {
        String path = "players." + playerUUID + ".trap_skin";
        String old = activeSkins.getString(path);
        activeSkins.set(path, skin);
        saveConfig();
        if (old != null && !old.equals(skin)) {
            plugin.getLogger().info("[VioTrap] Трап-скин игрока " + playerUUID + " изменён: " + old + " → " + skin);
        }
    }

    /** Установить активный скин пласта (заменяет старый) */
    public void setActivePlateSkin(UUID playerUUID, String skin) {
        String path = "players." + playerUUID + ".plate_skin";
        String old = activeSkins.getString(path);
        activeSkins.set(path, skin);
        saveConfig();
        if (old != null && !old.equals(skin)) {
            plugin.getLogger().info("[VioTrap] Пласт-скин игрока " + playerUUID + " изменён: " + old + " → " + skin);
        }
    }

    public String getActiveTrapSkin(UUID playerUUID) {
        return activeSkins.getString("players." + playerUUID + ".trap_skin", "default");
    }

    public String getActivePlateSkin(UUID playerUUID) {
        return activeSkins.getString("players." + playerUUID + ".plate_skin", "default");
    }

    /** Удалить активные скины у игрока (на всякий случай) */
    public void clearActiveSkins(UUID playerUUID) {
        String base = "players." + playerUUID;
        activeSkins.set(base + ".trap_skin", null);
        activeSkins.set(base + ".plate_skin", null);
        saveConfig();
    }

    private void saveConfig() {
        try {
            activeSkins.save(activeSkinsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить active_skins.yml: " + e.getMessage());
        }
    }
}