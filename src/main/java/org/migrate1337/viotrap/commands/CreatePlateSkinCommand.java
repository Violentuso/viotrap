package org.migrate1337.viotrap.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.gui.PlateSkinCreationMenu;

public class CreatePlateSkinCommand implements CommandExecutor {
    private final VioTrap plugin;
    private final String PREFIX = "§x§0§0§F§F§7§F§l✦ §x§5§5§F§F§5§5V§x§A§A§F§F§A§Ai§x§F§F§F§F§F§Fo§x§F§F§D§D§F§FT§x§F§F§B§B§F§Fr§x§F§F§9§9§F§Fa§x§F§F§7§7§F§Fp §8| §f";

    public CreatePlateSkinCommand(VioTrap plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + "§cЭту команду может использовать только игрок.");
            return true;
        } else if (!sender.hasPermission("viotrap.createplateskin")) {
            sender.sendMessage(PREFIX + "§cУ вас нет прав на создание скина пластин.");
            return false;
        } else {
            Player player = (Player)sender;
            PlateSkinCreationMenu menu = new PlateSkinCreationMenu(this.plugin);
            menu.openMenu(player);
            return true;
        }
    }
}