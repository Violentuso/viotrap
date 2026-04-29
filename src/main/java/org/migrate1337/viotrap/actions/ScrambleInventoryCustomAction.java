package org.migrate1337.viotrap.actions;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.migrate1337.viotrap.VioTrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.migrate1337.viotrap.actions.CustomActionFactory.getRandomPlayer;

public class ScrambleInventoryCustomAction implements CustomAction {
    private final String targetType;
    private final double radius;

    public ScrambleInventoryCustomAction(String targetType, double radius) {
        this.targetType = targetType;
        this.radius = radius;
    }

    @Override
    public void execute(Player primaryPlayer, Player[] opponents, VioTrap plugin) {
        for (Player target : CustomActionFactory.getTargets(this.targetType, primaryPlayer, opponents, this.radius)) {
            if (target == null || !target.isOnline()) continue;

            PlayerInventory inv = target.getInventory();
            List<ItemStack> hotbarItems = new ArrayList<>();

            for (int i = 0; i < 9; i++) {
                hotbarItems.add(inv.getItem(i));
            }

            Collections.shuffle(hotbarItems);

            for (int i = 0; i < 9; i++) {
                inv.setItem(i, hotbarItems.get(i));
            }
        }
    }

    public static List<Player> getTargets(String targetType, Player primaryPlayer, Player[] opponents, double radius) {
        List<Player> targets = new ArrayList<>();
        if (primaryPlayer == null) return targets;

        switch (targetType.toLowerCase()) {
            case "p":
            case "player":
                targets.add(primaryPlayer);
                break;
            case "o":
                if (opponents != null) {
                    targets.addAll(Arrays.asList(opponents));
                }
                break;
            case "rp":
                Player random = getRandomPlayer(primaryPlayer, opponents);
                if (random != null) targets.add(random);
                break;
            case "not-in":
                List<Player> trapPlayers = new ArrayList<>();
                trapPlayers.add(primaryPlayer);
                if (opponents != null) trapPlayers.addAll(Arrays.asList(opponents));

                // Ищем всех в радиусе, кто не в ловушке
                for (org.bukkit.entity.Entity entity : primaryPlayer.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof Player) {
                        Player nearbyPlayer = (Player) entity;
                        if (!trapPlayers.contains(nearbyPlayer)) {
                            targets.add(nearbyPlayer);
                        }
                    }
                }
                break;
        }
        return targets;
    }
}