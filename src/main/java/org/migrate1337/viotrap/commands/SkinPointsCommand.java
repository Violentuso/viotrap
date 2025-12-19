package org.migrate1337.viotrap.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.utils.ColorUtil;
import org.migrate1337.viotrap.utils.SkinPointsManager;

public class SkinPointsCommand implements CommandExecutor, TabCompleter {
    private final VioTrap plugin;
    private final SkinPointsManager pointsManager;

    public SkinPointsCommand(VioTrap plugin, SkinPointsManager pointsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viotrap.admin")) {
            sender.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНет прав!"));
            return true;
        } else if (args.length < 3) {
            sender.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AИспользование:"));
            sender.sendMessage("/skinpoints add <player> <skin> <amount>");
            sender.sendMessage("/skinpoints remove <player> <skin> <amount>");
            return true;
        } else {
            String action = args[0].toLowerCase();
            String playerName = args[1];
            String skin = args[2];
            Player target = this.plugin.getServer().getPlayer(playerName);
            if (target == null) {
                sender.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AИгрок не найден!"));
                return true;
            } else {
                if (action.equals("add") || action.equals("remove")) {
                    if (args.length != 4) {
                        sender.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AУкажите количество!"));
                        return true;
                    }

                    int amount;
                    try {
                        amount = Integer.parseInt(args[3]);
                        if (amount <= 0) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException var11) {
                        sender.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНедопустимое количество!"));
                        return true;
                    }

                    if (action.equals("add")) {
                        this.pointsManager.addPoints(target.getUniqueId(), skin, amount);
                        sender.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Добавлено " + amount + " поинтов для скина '" + skin + "' игроку " + playerName));
                    } else {
                        int currentPoints = this.pointsManager.getPoints(target.getUniqueId(), skin);
                        if (currentPoints < amount) {
                            sender.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AУ игрока недостаточно поинтов! Текущее количество: " + currentPoints));
                            return true;
                        }

                        this.pointsManager.removePoints(target.getUniqueId(), skin, amount);
                        sender.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Удалено " + amount + " поинтов для скина '" + skin + "' у игрока " + playerName));
                    }
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
            return (List)this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter((name) -> name.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        } else if (args.length == 3) {
            return (List)this.plugin.getSkinNames().stream().filter((skin) -> skin.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        } else {
            return args.length == 4 ? Arrays.asList("1", "10", "100") : Collections.emptyList();
        }
    }
}
