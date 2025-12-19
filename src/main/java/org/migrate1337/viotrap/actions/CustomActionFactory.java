package org.migrate1337.viotrap.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
            plugin.getLogger().info("[VioTrap] Actions не найдены для скина: " + skinName);
            return actions;
        }

        plugin.getLogger().info("[VioTrap] Загрузка actions для скина: " + skinName + ", найдено действий: " + actionsSection.getKeys(false).size());

        for (String actionKey : actionsSection.getKeys(false)) {
            ConfigurationSection actionConfig = actionsSection.getConfigurationSection(actionKey);
            if (actionConfig == null) {
                plugin.getLogger().warning("[VioTrap] Пустая секция действия: " + actionKey + " в скине " + skinName);
                continue;
            }

            String type = actionConfig.getString("type");
            if (type == null) {
                plugin.getLogger().warning("[VioTrap] Тип действия не указан для: " + actionKey + " в скине " + skinName);
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
                default:
                    plugin.getLogger().warning("[VioTrap] Неизвестный тип действия '" + type + "' для: " + actionKey + " в скине " + skinName);
            }
        }

        plugin.getLogger().info("[VioTrap] Загружено " + actions.size() + " actions для скина: " + skinName);
        return actions;
    }

    private static void loadEffectAction(ConfigurationSection actionConfig, String actionKey, String skinName,
                                         List<CustomAction> actions, VioTrap plugin) {
        // Новый формат: отдельные ключи (target, effect, amplifier, duration)
        String target = actionConfig.getString("target", "p");
        String effectName = actionConfig.getString("effect");

        if (effectName != null) {
            if (actionConfig.contains("amplifier") && actionConfig.contains("duration")) {
                try {
                    int amplifier = actionConfig.getInt("amplifier");
                    int duration = actionConfig.getInt("duration");

                    if (!isValidTarget(target)) {
                        plugin.getLogger().warning("[VioTrap] Некорректный target '" + target + "' для effect в " + actionKey + " (скин: " + skinName + ")");
                        return;
                    }

                    actions.add(new EffectCustomAction(target, effectName.toUpperCase(), amplifier, duration));
                    plugin.getLogger().info("[VioTrap] Загружен effect action: " + target + " " + effectName + " " + amplifier + " " + duration + " (скин: " + skinName + ")");
                } catch (Exception e) {
                    plugin.getLogger().warning("[VioTrap] Ошибка парсинга effect (отдельные ключи) в " + actionKey + " (скин: " + skinName + "): " + e.getMessage());
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
                            plugin.getLogger().info("[VioTrap] Загружен effect action (старая строка): " + effectData + " (скин: " + skinName + ")");
                        }
                    } else {
                        plugin.getLogger().warning("[VioTrap] Некорректный формат effect (строка) в " + actionKey + ": " + effectData + " (скин: " + skinName + ")");
                    }
                }
            }
        } else {
            plugin.getLogger().warning("[VioTrap] Effect не указан для действия " + actionKey + " в скине " + skinName);
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
                    plugin.getLogger().info("[VioTrap] Загружен command action: " + command + " (target: " + target + ") (скин: " + skinName + ")");
                } else {
                    plugin.getLogger().warning("[VioTrap] Некорректный target для команды в " + actionKey + ": " + target + " (скин: " + skinName + ")");
                }
            } else {
                plugin.getLogger().warning("[VioTrap] Некорректный формат команды в " + actionKey + ": " + commandData + " (скин: " + skinName + ")");
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
                        plugin.getLogger().info("[VioTrap] Загружен teleportout action: " + target + " " + blocks + " up (min-height: " + minHeight + ") (скин: " + skinName + ")");
                    } else {
                        plugin.getLogger().warning("[VioTrap] Некорректные параметры teleportout в " + actionKey + " (скин: " + skinName + ")");
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[VioTrap] Некорректный формат числа в teleportout: " + teleportData + " (скин: " + skinName + ")");
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
                plugin.getLogger().info("[VioTrap] Загружен particlehitbox action: " + target + " " + particleType + " " + duration + "s (скин: " + skinName + ")");
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[VioTrap] Некорректный тип частиц в " + actionKey + ": " + particleType + " (скин: " + skinName + ")");
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
                            plugin.getLogger().info("[VioTrap] Загружен particlehitbox (старая строка): " + particleData + " (скин: " + skinName + ")");
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("[VioTrap] Некорректный тип частиц (строка): " + particleType + " (скин: " + skinName + ")");
                        }
                    }
                }
            } else {
                plugin.getLogger().warning("[VioTrap] Параметры particlehitbox не найдены в " + actionKey + " (скин: " + skinName + ")");
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