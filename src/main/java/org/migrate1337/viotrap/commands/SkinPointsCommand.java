package org.migrate1337.viotrap.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.utils.SkinPointsManager;

public class SkinPointsCommand implements CommandExecutor, TabCompleter {
    private final VioTrap plugin;
    private final SkinPointsManager pointsManager;
    private final String PREFIX = "§x§0§0§F§F§7§F§l✦ §x§5§5§F§F§5§5V§x§A§A§F§F§A§Ai§x§F§F§F§F§F§Fo§x§F§F§D§D§F§FT§x§F§F§B§B§F§Fr§x§F§F§9§9§F§Fa§x§F§F§7§7§F§Fp §8| §f";

    public SkinPointsCommand(VioTrap plugin, SkinPointsManager pointsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viotrap.admin")) {
            sender.sendMessage(PREFIX + "§cУ вас нет прав.");
            return true;
        } else if (args.length < 3) {
            sender.sendMessage(PREFIX + "§cИспользование: §f/viotrap skinpoints <add/remove> <игрок> <скин> [кол-во]");
            return true;
        } else {
            String subCommand = args[0];
            String playerName = args[1];
            String skin = args[2];
            Player target = Bukkit.getPlayer(playerName);
            if (target == null) {
                sender.sendMessage(PREFIX + "§cИгрок §f" + playerName + " §cне найден.");
                return true;
            } else if (!this.plugin.getSkinNames().contains(skin) && !this.plugin.getPlateSkinNames().contains(skin)) {
                sender.sendMessage(PREFIX + "§cСкин §f" + skin + " §cне найден.");
                return true;
            } else {
                int amount = 1;
                if (args.length == 4) {
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException var10) {
                        sender.sendMessage(PREFIX + "§cНеверное количество.");
                        return true;
                    }
                }

                if (subCommand.equalsIgnoreCase("add")) {
                    this.pointsManager.addPoints(target.getUniqueId(), skin, amount);
                    sender.sendMessage(PREFIX + "Добавлено §x§5§5§F§F§5§5" + amount + " §fпоинтов для скина §7" + skin + " §fигроку §x§5§5§F§F§5§5" + playerName);
                } else {
                    if (!subCommand.equalsIgnoreCase("remove")) {
                        sender.sendMessage(PREFIX + "§cНеизвестная операция. Используйте add или remove.");
                        return true;
                    }

                    int currentPoints = this.pointsManager.getPoints(target.getUniqueId(), skin);
                    if (currentPoints < amount) {
                        sender.sendMessage(PREFIX + "§cУ игрока недостаточно поинтов! Текущее количество: §f" + currentPoints);
                        return true;
                    }

                    this.pointsManager.removePoints(target.getUniqueId(), skin, amount);
                    sender.sendMessage(PREFIX + "Удалено §x§5§5§F§F§5§5" + amount + " §fпоинтов для скина §7" + skin + " §fу игрока §x§5§5§F§F§5§5" + playerName);
                }

                return true;
            }
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("viotrap.admin")) {
            return Collections.emptyList();
        } else if (args.length == 1) {
            return Arrays.asList("add", "remove");
        } else if (args.length == 2) {
            return (List)this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter((name) -> {
                return name.toLowerCase().startsWith(args[1].toLowerCase());
            }).collect(Collectors.toList());
        } else if (args.length == 3) {
            return (List)this.plugin.getSkinNames().stream().filter((skin) -> {
                return skin.toLowerCase().startsWith(args[2].toLowerCase());
            }).collect(Collectors.toList());
        } else {
            return args.length == 4 ? Arrays.asList("1", "5", "10") : Collections.emptyList();
        }
    }
}