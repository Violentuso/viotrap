package org.migrate1337.viotrap.actions;

import java.util.*;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;

public class CustomActionFactory {
    private static final Random random = new Random();

    public static List<CustomAction> loadActions(String skinName, VioTrap plugin) {
        List<CustomAction> actions = new ArrayList<>();
        ConfigurationSection actionsSection = plugin.getConfig().getConfigurationSection("skins." + skinName + ".actions");
        if (actionsSection == null) {
            return actions;
        }


        for (String actionKey : actionsSection.getKeys(false)) {
            ConfigurationSection actionConfig = actionsSection.getConfigurationSection(actionKey);
            if (actionConfig == null) {
                continue;
            }

            String type = actionConfig.getString("type");
            if (type == null) {
                continue;
            }

            switch (type.toLowerCase()) {
                case "effect":
                    loadEffectAction(actionConfig, actionKey, skinName, actions, plugin);
                    break;
                case "command":
                    loadCommandAction(actionConfig, actionKey, skinName, actions, plugin);
                    break;
                case "teleportout":
                    loadTeleportOutAction(actionConfig, actionKey, skinName, actions, plugin);
                    break;
                case "particlehitbox":
                    loadParticleHitboxAction(actionConfig, actionKey, skinName, actions, plugin);
                    break;
                case "cooldownitem":
                    loadCooldownItemAction(actionConfig, actionKey, skinName, actions, plugin);
                    break;
                case "denyitemuse":
                    loadDenyItemUseAction(actionConfig, actionKey, skinName, actions, plugin);
                break;
                case "launch": // <--- Новое
                    loadLaunchAction(actionConfig, actionKey, skinName, actions, plugin);
                    break;
                case "scrambleinventory": // <--- Новое
                    loadScrambleInventoryAction(actionConfig, actionKey, skinName, actions, plugin);
                break;
                case "blockspread":
                    loadBlockSpreadAction(actionConfig, actionKey, skinName, actions, plugin);
                    break;
                    default:
            }
        }

        return actions;
    }
    private static void loadCooldownItemAction(ConfigurationSection cfg, String key, String skin, List<CustomAction> actions, VioTrap plugin) {
        String target = cfg.getString("target", "p");
        List<String> itemNames = cfg.getStringList("items");
        int seconds = cfg.getInt("seconds", 30);
        double radius = cfg.getDouble("radius", 5.0);
        if (itemNames.isEmpty() || seconds <= 0 || !isValidTarget(target)) {
            return;
        }

        Set<Material> materials = new HashSet<>();
        for (String name : itemNames) {
            Material mat = Material.matchMaterial(name.toUpperCase());
            if (mat != null) materials.add(mat);
        }

        if (!materials.isEmpty()) {
            actions.add(new CooldownItemCustomAction(target, materials, seconds, radius));
        }
    }

    private static void loadDenyItemUseAction(ConfigurationSection cfg, String key, String skin, List<CustomAction> actions, VioTrap plugin) {
        String target = cfg.getString("target", "p");
        List<String> itemNames = cfg.getStringList("items");
        double radius = cfg.getDouble("radius", 5.0);
        if (itemNames.isEmpty() || !isValidTarget(target)) {
            return;
        }

        Set<Material> materials = new HashSet<>();
        for (String name : itemNames) {
            Material mat = Material.matchMaterial(name.toUpperCase());
            if (mat != null) materials.add(mat);
        }

        if (!materials.isEmpty()) {
            DenyItemUseCustomAction action = new DenyItemUseCustomAction(target, materials, radius);
            actions.add(action);

            plugin.getServer().getPluginManager().registerEvents(action, plugin);

        }
    }
    private static void loadLaunchAction(ConfigurationSection cfg, String key, String skin, List<CustomAction> actions, VioTrap plugin) {
        String target = cfg.getString("target", "not-in");
        double radius = cfg.getDouble("radius", 5.0);
        double upwardForce = cfg.getDouble("upward-force", 1.2);
        double horizontalForce = cfg.getDouble("horizontal-force", 1.5);
        if (isValidTarget(target)) {
            actions.add(new LaunchCustomAction(target, radius, upwardForce, horizontalForce));
        }
    }

    private static void loadScrambleInventoryAction(ConfigurationSection cfg, String key, String skin, List<CustomAction> actions, VioTrap plugin) {
        String target = cfg.getString("target", "p");
        double radius = cfg.getDouble("radius", 5.0);
        if (isValidTarget(target)) {
            actions.add(new ScrambleInventoryCustomAction(target, radius));
        }
    }
    private static void loadEffectAction(ConfigurationSection actionConfig, String actionKey, String skinName,
                                         List<CustomAction> actions, VioTrap plugin) {
        String target = actionConfig.getString("target", "p");
        String effectName = actionConfig.getString("effect");
        double radius = actionConfig.getDouble("radius", 5.0);
        if (effectName != null) {
            if (actionConfig.contains("amplifier") && actionConfig.contains("duration")) {
                try {
                    int amplifier = actionConfig.getInt("amplifier");
                    int duration = actionConfig.getInt("duration");

                    if (!isValidTarget(target)) {
                        return;
                    }

                    actions.add(new EffectCustomAction(target, effectName.toUpperCase(), amplifier, duration, radius));
                } catch (Exception e) {
                }
            } else {

                String effectData = actionConfig.getString("effect");
                if (effectData != null) {
                    String[] parts = effectData.split(" ");
                    if (parts.length == 4) {
                        target = parts[0];
                        String effect = parts[1].toUpperCase();
                        int amplifier = Integer.parseInt(parts[2]);
                        int duration = Integer.parseInt(parts[3]);

                        if (isValidTarget(target)) {
                            actions.add(new EffectCustomAction(target, effect, amplifier, duration, radius));
                        }
                    } else {
                    }
                }
            }
        } else {
        }
    }
    public static List<Player> getTargets(String targetType, Player primaryPlayer, Player[] opponents, double radius) {
        List<Player> targets = new ArrayList<>();
        if (primaryPlayer == null) return targets;

        switch (targetType.toLowerCase()) {
            case "p":
            case "player":
                targets.add(primaryPlayer);
                break;
            case "o":
                if (opponents != null) targets.addAll(Arrays.asList(opponents));
                break;
            case "rp":
                Player random = getRandomPlayer(primaryPlayer, opponents);
                if (random != null) targets.add(random);
                break;
            case "not-in":
                List<Player> trapPlayers = new ArrayList<>();
                trapPlayers.add(primaryPlayer);
                if (opponents != null) trapPlayers.addAll(Arrays.asList(opponents));

                for (org.bukkit.entity.Entity entity : primaryPlayer.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof Player) {
                        Player nearbyPlayer = (Player) entity;
                        if (!trapPlayers.contains(nearbyPlayer)) targets.add(nearbyPlayer);
                    }
                }
                break;
        }
        return targets;
    }
    private static void loadCommandAction(ConfigurationSection actionConfig, String actionKey, String skinName,
                                          List<CustomAction> actions, VioTrap plugin) {
        String commandData = actionConfig.getString("command");
        double radius = actionConfig.getDouble("radius", 5.0);
        if (commandData != null) {
            String[] parts = commandData.split(" ");
            if (parts.length >= 2) {
                String target = parts[parts.length - 1];
                String command = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1));

                if (isValidTarget(target)) {
                    actions.add(new CommandCustomAction(target, command, radius));
                } else {
                }
            } else {
            }
        }
    }

    private static void loadTeleportOutAction(ConfigurationSection actionConfig, String actionKey, String skinName,
                                              List<CustomAction> actions, VioTrap plugin) {
        String teleportData = actionConfig.getString("teleport");
        int radius = actionConfig.getInt("radius", 5);
        if (teleportData != null) {
            String[] parts = teleportData.split(" ");
            if (parts.length == 3) {
                String target = parts[0];
                try {
                    int blocks = Integer.parseInt(parts[1]);
                    String location = parts[2];
                    int minHeight = actionConfig.getInt("min-height", 10);

                    if (isValidTarget(target) && location.equalsIgnoreCase("up") && blocks > 0 && minHeight >= 0) {
                        actions.add(new TeleportOutCustomAction(target, blocks, minHeight, radius));
                    } else {
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
    }
    private static void loadBlockSpreadAction(ConfigurationSection cfg, String key, String skin, List<CustomAction> actions, VioTrap plugin) {
        String target = cfg.getString("target", "not-in");
        double radius = cfg.getDouble("radius", 5.0);
        String materialName = cfg.getString("material");
        double chance = cfg.getDouble("chance", 30.0); // 30% территории по умолчанию
        int revertTime = cfg.getInt("revert-time", 10); // Через сколько вернуть блоки обратно

        if (materialName != null && isValidTarget(target)) {
            Material mat = Material.matchMaterial(materialName.toUpperCase());
            if (mat != null) {
                actions.add(new BlockSpreadCustomAction(target, radius, mat, chance, revertTime));
            }
        }
    }
    private static void loadParticleHitboxAction(ConfigurationSection actionConfig, String actionKey, String skinName,
                                                 List<CustomAction> actions, VioTrap plugin) {

        String target = actionConfig.getString("target", "p");
        double radius = actionConfig.getDouble("radius", 5.0);
        String particleType = actionConfig.getString("particle-type");
        int duration = actionConfig.getInt("duration");
        int updateInterval = actionConfig.getInt("update-interval", 4);

        if (particleType != null && duration > 0 && updateInterval > 0 && isValidTarget(target)) {
            try {
                Particle.valueOf(particleType.toUpperCase());
                actions.add(new ParticleHitboxCustomAction(target, particleType.toUpperCase(), duration, updateInterval, radius));
            } catch (IllegalArgumentException e) {
            }
        } else {

            String particleData = actionConfig.getString("particle");
            if (particleData != null) {
                String[] parts = particleData.split(" ");
                if (parts.length == 3) {
                    target = parts[0];
                    particleType = parts[1];
                    duration = Integer.parseInt(parts[2]);
                    updateInterval = actionConfig.getInt("update-interval", 4);

                    if (isValidTarget(target) && duration > 0 && updateInterval > 0) {
                        try {
                            Particle.valueOf(particleType.toUpperCase());
                            actions.add(new ParticleHitboxCustomAction(target, particleType.toUpperCase(), duration, updateInterval, radius));
                        } catch (IllegalArgumentException e) {
                        }
                    }
                }
            } else {
            }
        }
    }

    private static boolean isValidTarget(String target) {
        return target.equalsIgnoreCase("p") ||
                target.equalsIgnoreCase("player") ||
                target.equalsIgnoreCase("o") ||
                target.equalsIgnoreCase("rp") ||
                target.equalsIgnoreCase("not-in");
    }

    public static Player getRandomPlayer(Player player, Player[] opponents) {
        List<Player> candidates = new ArrayList<>();
        if (player != null && player.isOnline()) {
            candidates.add(player);
        }

        for (Player opponent : opponents) {
            if (opponent != null && opponent.isOnline()) {
                candidates.add(opponent);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }
}