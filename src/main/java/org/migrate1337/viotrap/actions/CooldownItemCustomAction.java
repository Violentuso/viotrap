package org.migrate1337.viotrap.actions;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;

import java.util.HashSet;
import java.util.Set;

public class CooldownItemCustomAction implements CustomAction {
    private final String target;
    private final Set<Material> items;
    private final int cooldownTicks;

    public CooldownItemCustomAction(String target, Set<Material> items, int cooldownSeconds) {
        this.target = target.toLowerCase();
        this.items = new HashSet<>(items);
        this.cooldownTicks = cooldownSeconds * 20;
    }

    @Override
    public void execute(Player player, Player[] opponents, VioTrap plugin) {
        switch (target) {
            case "p":
            case "player":
                apply(player);
                break;
            case "o":
                for (Player opponent : opponents) {
                    apply(opponent);
                }
                break;
            case "rp":
                Player random = CustomActionFactory.getRandomPlayer(player, opponents);
                if (random != null) apply(random);
                break;
            default:
        }
    }

    private void apply(Player p) {
        if (p == null || !p.isOnline()) return;
        for (Material mat : items) {
            int current = p.getCooldown(mat);
            if (current < cooldownTicks) {
                p.setCooldown(mat, cooldownTicks);
            }
        }
    }
}