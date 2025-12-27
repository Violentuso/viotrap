package org.migrate1337.viotrap.conditions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.utils.ColorUtil;

public class ConditionManager {

    private final VioTrap plugin;

    public ConditionManager(VioTrap plugin) {
        this.plugin = plugin;
    }

    public boolean checkConditions(Player player, String sectionKey) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionKey + ".conditions");
        if (section == null) {
            return true;
        }

        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            try {
                ConditionType type = ConditionType.valueOf(key.toUpperCase());
                if (!checkSingleCondition(player, type, value)) {
                    sendFailureMessage(player, type);
                    return false;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Найден неизвестный тип условия в конфиге: " + key);
            }
        }
        return true;
    }

    private boolean checkSingleCondition(Player player, ConditionType type, String value) {
        switch (type) {
            case PERMISSION:
                return player.hasPermission(value);
            case BLOCK_BELOW:
                Material mat = Material.matchMaterial(value);
                return mat != null && player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == mat;
            case IS_SNEAKING:
                boolean shouldSneak = Boolean.parseBoolean(value);
                return player.isSneaking() == shouldSneak;
            case MIN_HEALTH:
                try {
                    double minHealth = Double.parseDouble(value);
                    return player.getHealth() >= minHealth;
                } catch (NumberFormatException e) {
                    return true;
                }
            case GAMEMODE:
                try {
                    GameMode gm = GameMode.valueOf(value.toUpperCase());
                    return player.getGameMode() == gm;
                } catch (IllegalArgumentException e) {
                    return true;
                }
            case ITEM_IN_OFFHAND:
                Material offhandMat = Material.matchMaterial(value);
                return offhandMat != null && player.getInventory().getItemInOffHand().getType() == offhandMat;
            case IN_REGION:
                return isPlayerInRegion(player, value);
            case NOT_IN_REGION:
                return !isPlayerInRegion(player, value);
            case IS_FLYING:
                return player.isFlying();
            case NOT_FLYING:
                return !player.isFlying();
            case HAS_EFFECT:
                return hasPotionEffect(player, value);
            case NO_EFFECT:
                return !hasPotionEffect(player, value);
            case IN_BIOME:
                try {
                    Biome biome = Biome.valueOf(value.toUpperCase());
                    return player.getLocation().getBlock().getBiome() == biome;
                } catch (IllegalArgumentException e) {
                    return true;
                }
            case NOT_IN_BIOME:
                try {
                    Biome biome = Biome.valueOf(value.toUpperCase());
                    return player.getLocation().getBlock().getBiome() != biome;
                } catch (IllegalArgumentException e) {
                    return true;
                }
            case IS_SWIMMING:
                return player.isSwimming();
            case NOT_SWIMMING:
                return !player.isSwimming();
            default:
                return true;
        }
    }

    private boolean isPlayerInRegion(Player player, String regionName) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            org.bukkit.World world = player.getWorld();
            com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(world);
            com.sk89q.worldguard.protection.managers.RegionManager regions = container.get(adaptedWorld);
            if (regions == null) {
                plugin.getLogger().warning("RegionManager not found for world: " + world.getName());
                return false;
            }
            org.bukkit.Location loc = player.getLocation();
            BlockVector3 position = BukkitAdapter.asBlockVector(loc);
            ApplicableRegionSet set = regions.getApplicableRegions(position);
            for (ProtectedRegion region : set) {
                if (region.getId().equalsIgnoreCase(regionName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking region: " + e.getMessage());
            return false;
        }
    }

    private boolean hasPotionEffect(Player player, String effectString) {

        String[] parts = effectString.split(":");
        if (parts.length != 2) {
            return true;
        }
        PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
        if (effectType == null) return true;
        try {
            int level = Integer.parseInt(parts[1]) - 1;
            for (PotionEffect effect : player.getActivePotionEffects()) {
                if (effect.getType().equals(effectType) && effect.getAmplifier() == level) {
                    return true;
                }
            }
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private void sendFailureMessage(Player player, ConditionType type) {
        String configKey = type.name().toLowerCase();
        String msg;
        String msgType = "chat";


        Object configValue = plugin.getConfig().get("condition_messages." + configKey);
        if (configValue instanceof String) {
            msg = (String) configValue;
        } else if (configValue instanceof ConfigurationSection) {
            ConfigurationSection msgSection = (ConfigurationSection) configValue;
            msg = msgSection.getString("message");
            msgType = msgSection.getString("type", "chat").toLowerCase();
        } else {

            configKey = "default";
            configValue = plugin.getConfig().get("condition_messages." + configKey);
            if (configValue instanceof String) {
                msg = (String) configValue;
            } else if (configValue instanceof ConfigurationSection) {
                ConfigurationSection msgSection = (ConfigurationSection) configValue;
                msg = msgSection.getString("message");
                msgType = msgSection.getString("type", "chat").toLowerCase();
            } else {
                msg = "Условие не выполнено!";
            }
        }

        String coloredMsg = ColorUtil.format("&#EB2D3A" + msg);

        switch (msgType) {
            case "actionbar":
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(coloredMsg));
                break;
            case "title":
                player.sendTitle(coloredMsg, null, 10, 70, 20);
                break;
            case "subtitle":
                player.sendTitle(null, coloredMsg, 10, 70, 20);
                break;
            case "chat":
            default:
                player.sendMessage(coloredMsg);
                break;
        }
    }
}