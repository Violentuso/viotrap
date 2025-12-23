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
                    default:
            }
        }

        return actions;
    }
    private static void loadCooldownItemAction(ConfigurationSection cfg, String key, String skin, List<CustomAction> actions, VioTrap plugin) {
        String target = cfg.getString("target", "p");
        List<String> itemNames = cfg.getStringList("items");
        int seconds = cfg.getInt("seconds", 30);

        if (itemNames.isEmpty() || seconds <= 0 || !isValidTarget(target)) {
            return;
        }

        Set<Material> materials = new HashSet<>();
        for (String name : itemNames) {
            Material mat = Material.matchMaterial(name.toUpperCase());
            if (mat != null) materials.add(mat);
        }

        if (!materials.isEmpty()) {
            actions.add(new CooldownItemCustomAction(target, materials, seconds));
        }
    }

    private static void loadDenyItemUseAction(ConfigurationSection cfg, String key, String skin, List<CustomAction> actions, VioTrap plugin) {
        String target = cfg.getString("target", "p");
        List<String> itemNames = cfg.getStringList("items");

        if (itemNames.isEmpty() || !isValidTarget(target)) {
            return;
        }

        Set<Material> materials = new HashSet<>();
        for (String name : itemNames) {
            Material mat = Material.matchMaterial(name.toUpperCase());
            if (mat != null) materials.add(mat);
        }

        if (!materials.isEmpty()) {
            DenyItemUseCustomAction action = new DenyItemUseCustomAction(target, materials);
            actions.add(action);

            plugin.getServer().getPluginManager().registerEvents(action, plugin);

        }
    }
    private static void loadEffectAction(ConfigurationSection actionConfig, String actionKey, String skinName,
                                         List<CustomAction> actions, VioTrap plugin) {
        String target = actionConfig.getString("target", "p");
        String effectName = actionConfig.getString("effect");

        if (effectName != null) {
            if (actionConfig.contains("amplifier") && actionConfig.contains("duration")) {
                try {
                    int amplifier = actionConfig.getInt("amplifier");
                    int duration = actionConfig.getInt("duration");

                    if (!isValidTarget(target)) {
                        return;
                    }

                    actions.add(new EffectCustomAction(target, effectName.toUpperCase(), amplifier, duration));
                } catch (Exception e) {
                }
            } else {
                // Fallback: старый формат - строка в "effect"
                String effectData = actionConfig.getString("effect");
                if (effectData != null) {
                    String[] parts = effectData.split(" ");
                    if (parts.length == 4) {
                        target = parts[0];
                        String effect = parts[1].toUpperCase();
                        int amplifier = Integer.parseInt(parts[2]);
                        int duration = Integer.parseInt(parts[3]);

                        if (isValidTarget(target)) {
                            actions.add(new EffectCustomAction(target, effect, amplifier, duration));
                        }
                    } else {
                    }
                }
            }
        } else {
        }
    }

    private static void loadCommandAction(ConfigurationSection actionConfig, String actionKey, String skinName,
                                          List<CustomAction> actions, VioTrap plugin) {
        String commandData = actionConfig.getString("command");
        if (commandData != null) {
            String[] parts = commandData.split(" ");
            if (parts.length >= 2) {
                String target = parts[parts.length - 1];
                String command = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1));

                if (isValidTarget(target)) {
                    actions.add(new CommandCustomAction(target, command));
                } else {
                }
            } else {
            }
        }
    }

    private static void loadTeleportOutAction(ConfigurationSection actionConfig, String actionKey, String skinName,
                                              List<CustomAction> actions, VioTrap plugin) {
        String teleportData = actionConfig.getString("teleport");
        if (teleportData != null) {
            String[] parts = teleportData.split(" ");
            if (parts.length == 3) {
                String target = parts[0];
                try {
                    int blocks = Integer.parseInt(parts[1]);
                    String location = parts[2];
                    int minHeight = actionConfig.getInt("min-height", 10);

                    if (isValidTarget(target) && location.equalsIgnoreCase("up") && blocks > 0 && minHeight >= 0) {
                        actions.add(new TeleportOutCustomAction(target, blocks, minHeight));
                    } else {
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    private static void loadParticleHitboxAction(ConfigurationSection actionConfig, String actionKey, String skinName,
                                                 List<CustomAction> actions, VioTrap plugin) {
        // Новый формат: отдельные ключи
        String target = actionConfig.getString("target", "p");
        String particleType = actionConfig.getString("particle-type");
        int duration = actionConfig.getInt("duration");
        int updateInterval = actionConfig.getInt("update-interval", 4);

        if (particleType != null && duration > 0 && updateInterval > 0 && isValidTarget(target)) {
            try {
                Particle.valueOf(particleType.toUpperCase());
                actions.add(new ParticleHitboxCustomAction(target, particleType.toUpperCase(), duration, updateInterval));
            } catch (IllegalArgumentException e) {
            }
        } else {
            // Fallback: старый формат
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
                            actions.add(new ParticleHitboxCustomAction(target, particleType.toUpperCase(), duration, updateInterval));
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
                target.equalsIgnoreCase("rp");
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