package org.migrate1337.viotrap.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.migrate1337.viotrap.VioTrap;

public class SkinPointsPlaceholder extends PlaceholderExpansion {

    private final VioTrap plugin;
    private final SkinPointsManager pointsManager;
    private final ActiveSkinsManager activeSkinsManager;

    public SkinPointsPlaceholder(VioTrap plugin, SkinPointsManager pointsManager, ActiveSkinsManager activeSkinsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
        this.activeSkinsManager = activeSkinsManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "viotrap";
    }

    @Override
    public @NotNull String getAuthor() {
        return "migrate1337";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // %viotrap_trap_active_skin%
        if (params.equalsIgnoreCase("trap_active_skin")) {
            String skin = activeSkinsManager.getActiveTrapSkin(player.getUniqueId());
            return (skin == null || skin.equals("default") || skin.isEmpty())
                    ? "Нет активного скина"
                    : skin;
        }

        // %viotrap_plate_active_skin%
        if (params.equalsIgnoreCase("plate_active_skin")) {
            String skin = activeSkinsManager.getActivePlateSkin(player.getUniqueId());
            return (skin == null || skin.equals("default") || skin.isEmpty())
                    ? "Нет активного скина"
                    : skin;
        }

        // %viotrap_points_название_скина%
        if (params.startsWith("points_")) {
            String skin = params.substring(7);
            if (plugin.getSkinNames().contains(skin) || plugin.getPlateSkinNames().contains(skin)) {
                int points = pointsManager.getPoints(player.getUniqueId(), skin);
                return String.valueOf(points);
            }
            return "0";
        }

        // Поддержка старых плейсхолдеров (можно потом убрать)
        if (params.equals("currently_activeskin_trap")) {
            String skin = activeSkinsManager.getActiveTrapSkin(player.getUniqueId());
            return (skin == null || skin.equals("default")) ? "Нет активного скина" : skin;
        }
        if (params.equals("currently_activeskin_plate")) {
            String skin = activeSkinsManager.getActivePlateSkin(player.getUniqueId());
            return (skin == null || skin.equals("default")) ? "Нет активного скина" : skin;
        }

        return null;
    }
}