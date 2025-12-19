package org.migrate1337.viotrap.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.gui.PlateSkinCreationMenu;

public class CreatePlateSkinCommand implements CommandExecutor {
    private final VioTrap plugin;

    public CreatePlateSkinCommand(VioTrap plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭту команду можно использовать только игроку.");
            return true;
        } else if (!sender.hasPermission("viotrap.createplateskin")) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "У вас нет прав на использование данной команды!");
            return false;
        } else {
            Player player = (Player)sender;
            PlateSkinCreationMenu menu = new PlateSkinCreationMenu(this.plugin);
            menu.openMenu(player);
            return true;
        }
    }
}
