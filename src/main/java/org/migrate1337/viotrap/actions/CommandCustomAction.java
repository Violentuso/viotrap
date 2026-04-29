package org.migrate1337.viotrap.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;

public class CommandCustomAction implements CustomAction {
    private final String target;
    private final String command;
    private final double radius;
    public CommandCustomAction(String target, String command, double radius) {
        this.target = target.toLowerCase();
        this.command = command;
        this.radius = radius;

    }

    @Override
    public void execute(Player player, Player[] opponents, VioTrap plugin) {
        for (Player t : CustomActionFactory.getTargets(this.target, player, opponents, this.radius)) {
            this.executeCommand(t, this.command, plugin);
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
        }
    }
}
