package org.migrate1337.viotrap.actions;

import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;

public class TeleportOutCustomAction implements CustomAction {
    private final String target;
    private final int blocks;
    private final int minHeight;

    public TeleportOutCustomAction(String target, int blocks, int minHeight) {
        this.target = target.toLowerCase();
        this.blocks = blocks;
        this.minHeight = minHeight;
    }

    public void execute(Player player, Player[] opponents, VioTrap plugin) {
        switch (this.target) {
            case "p":
            case "player":
                if (player != null && player.isOnline()) {
                    this.teleportPlayer(player, player.getLocation(), plugin);
                }
                break;
            case "o":
                for(Player opponent : opponents) {
                    if (opponent != null && opponent.isOnline()) {
                        this.teleportPlayer(opponent, opponent.getLocation(), plugin);
                    }
                }
                break;
            case "rp":
                Player randomPlayer = CustomActionFactory.getRandomPlayer(player, opponents);
                if (randomPlayer != null && randomPlayer.isOnline()) {
                    this.teleportPlayer(randomPlayer, randomPlayer.getLocation(), plugin);
                } else {
                }
                break;
            default:
        }

    }

    private void teleportPlayer(Player player, Location trapLocation, VioTrap plugin) {
        Location currentLocation = player.getLocation();
        World world = currentLocation.getWorld();
        if (world == null) {
        } else {
            double targetY = currentLocation.getY() + (double)this.blocks;
            Location targetLocation = new Location(world, currentLocation.getX(), targetY, currentLocation.getZ(), currentLocation.getYaw(), currentLocation.getPitch());
            if (this.isSafeLocation(targetLocation)) {
                String var13 = player.getName();

                player.teleport(targetLocation);
                player.playSound(targetLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
            } else {
                Location safeLocation = this.findSafeLocationAbove(currentLocation, plugin);
                if (safeLocation != null) {
                    String var10001 = player.getName();

                    player.teleport(safeLocation);
                    player.playSound(safeLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
                } else {
                    String var12 = player.getName();
                    player.sendMessage("§cНе удалось телепортироваться: нет безопасного места!");
                }

            }
        }
    }

    private boolean isSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        } else {
            Block feetBlock = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Block headBlock = world.getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());
            return feetBlock.getType() == Material.AIR && headBlock.getType() == Material.AIR && !this.isDangerousBlock(feetBlock) && !this.isDangerousBlock(headBlock);
        }
    }

    private boolean isDangerousBlock(Block block) {
        Material type = block.getType();
        return type == Material.LAVA || type == Material.FIRE || type == Material.MAGMA_BLOCK;
    }

    private Location findSafeLocationAbove(Location startLocation, VioTrap plugin) {
        World world = startLocation.getWorld();
        if (world == null) {
            return null;
        } else {
            int maxHeight = Math.min(world.getMaxHeight(), (int)startLocation.getY() + 100);
            int startY = (int)Math.max(startLocation.getY() + (double)this.minHeight, startLocation.getY() + (double)1.0F);

            for(int y = startY; y <= maxHeight - 1; ++y) {
                Location checkLocation = new Location(world, startLocation.getX(), (double)y, startLocation.getZ(), startLocation.getYaw(), startLocation.getPitch());
                if (this.isSafeLocation(checkLocation)) {
                    return checkLocation;
                }
            }

            return null;
        }
    }
}
