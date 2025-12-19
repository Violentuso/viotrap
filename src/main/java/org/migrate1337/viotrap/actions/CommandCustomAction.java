package org.migrate1337.viotrap.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;

public class CommandCustomAction implements CustomAction {
    private final String target;
    private final String command;

    public CommandCustomAction(String target, String command) {
        this.target = target.toLowerCase();
        this.command = command;
    }

    public void execute(Player player, Player[] opponents, VioTrap plugin) {
        switch (this.target) {
            case "p":
            case "player":
                this.executeCommand(player, this.command, plugin);
                break;
            case "o":
                for(Player opponent : opponents) {
                    this.executeCommand(opponent, this.command, plugin);
                }
                break;
            case "rp":
                Player randomPlayer = CustomActionFactory.getRandomPlayer(player, opponents);
                this.executeCommand(randomPlayer, this.command, plugin);
                break;
            default:
                plugin.getLogger().warning("Некорректный таргет в CommandCustomAction: " + this.target);
        }

    }

    private void executeCommand(Player player, String command, VioTrap plugin) {
        if (player != null && player.isOnline()) {
            String finalCommand = command + " " + player.getName();

            try {
                boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                if (success) {
                }
            } catch (Exception var6) {
            }

        } else {
            plugin.getLogger().warning("Игрок недоступен для выполнения команды: " + command);
        }
    }
}
