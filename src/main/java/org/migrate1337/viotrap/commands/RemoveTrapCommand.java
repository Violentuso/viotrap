package org.migrate1337.viotrap.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.TrapItem;
import org.migrate1337.viotrap.utils.ColorUtil;

public class RemoveTrapCommand implements CommandExecutor, TabCompleter {
    private final VioTrap plugin;

    public RemoveTrapCommand(VioTrap plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viotrap.admin")) {
            sender.sendMessage(ColorUtil.format("&#EB2D3AНет прав!"));
            return true;
        } else if (args.length != 3) {
            sender.sendMessage(ColorUtil.format("&#EB2D3AИспользование: /removetrap <player> <skin> <amount>"));
            return true;
        } else {
            String playerName = args[0];
            String skin = args[1];
            Player target = this.plugin.getServer().getPlayer(playerName);
            if (target == null) {
                sender.sendMessage(ColorUtil.format("&#EB2D3AИгрок не найден!"));
                return true;
            } else if (!skin.equalsIgnoreCase("none") && !this.plugin.getSkinNames().contains(skin)) {
                sender.sendMessage(ColorUtil.format("&#EB2D3AСкин не найден!"));
                return true;
            } else {
                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                    if (amount <= 0) {
                        sender.sendMessage(ColorUtil.format("&#EB2D3AКоличество должно быть больше 0!"));
                        return true;
                    }
                } catch (NumberFormatException var10) {
                    sender.sendMessage(ColorUtil.format("&#EB2D3AНедопустимое количество!"));
                    return true;
                }

                int removed = this.removeTraps(target, skin, amount);
                sender.sendMessage(ColorUtil.format("&#ADD8E6Удалено " + removed + " трапок со скином '" + skin + "' у игрока " + playerName));
                return true;
            }
        }
    }

    private int removeTraps(Player player, String skin, int amount) {
        int removed = 0;

        for(ItemStack item : player.getInventory().getContents()) {
            if (removed < amount && item != null && TrapItem.isTrapItem(item) && (skin.equalsIgnoreCase("none") && TrapItem.getSkin(item) == null || skin.equals(TrapItem.getSkin(item)))) {
                int itemAmount = item.getAmount();
                if (itemAmount + removed <= amount) {
                    player.getInventory().remove(item);
                    removed += itemAmount;
                } else {
                    item.setAmount(itemAmount - (amount - removed));
                    removed = amount;
                }
            }
        }

        player.updateInventory();
        return removed;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("viotrap.admin")) {
            return Collections.emptyList();
        } else if (args.length == 1) {
            return (List)this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter((name) -> name.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        } else if (args.length == 2) {
            List<String> skins = new ArrayList(this.plugin.getSkinNames());
            skins.add("none");
            return (List)skins.stream().filter((skin) -> skin.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        } else {
            return args.length == 3 ? Arrays.asList("1", "16", "32", "64") : Collections.emptyList();
        }
    }
}
