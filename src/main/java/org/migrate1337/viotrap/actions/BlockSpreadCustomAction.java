package org.migrate1337.viotrap.actions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BlockSpreadCustomAction implements CustomAction {
    private final String targetType;
    private final double targetRadius;  
    private final double actionRadius;  
    private final Material materialToPlace;
    private final double chance;
    private final int revertDelaySeconds;
    private final Random random = new Random();

     
    public BlockSpreadCustomAction(String targetType, double targetRadius, double actionRadius, Material materialToPlace, double chance, int revertDelaySeconds) {
        this.targetType = targetType.toLowerCase();
        this.targetRadius = targetRadius;
        this.actionRadius = actionRadius;
        this.materialToPlace = materialToPlace;
        this.chance = chance;
        this.revertDelaySeconds = revertDelaySeconds;
    }

    @Override
    public void execute(Player primaryPlayer, Player[] opponents, VioTrap plugin) {
        if (primaryPlayer == null || !primaryPlayer.isOnline()) return;

        List<Player> validTargets = new ArrayList<>();

         
        switch (targetType) {
            case "p":
                validTargets.add(primaryPlayer);
                break;
            case "o":
                if (opponents != null) {
                    for (Player opp : opponents) validTargets.add(opp);
                }
                break;
            case "rp":
                if (opponents != null && opponents.length > 0) {
                    validTargets.add(opponents[random.nextInt(opponents.length)]);
                }
                break;
            case "not-in":
                if (opponents != null) {
                    for (Player opp : opponents) {
                         
                        if (opp.getLocation().distance(primaryPlayer.getLocation()) > targetRadius) {
                            validTargets.add(opp);
                        }
                    }
                }
                break;
        }

        if (validTargets.isEmpty()) return;

        int r = (int) Math.floor(actionRadius);
        Map<Location, BlockData> originalBlocks = new HashMap<>();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

         
        for (Player target : validTargets) {
            Location center = target.getLocation();
            int y = center.getBlockY();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(center.getWorld()));

            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z > r * r) continue;

                    Block block = center.getWorld().getBlockAt(center.getBlockX() + x, y, center.getBlockZ() + z);

                    if (!block.isReplaceable()) continue;

                    if (regionManager != null) {
                        BlockVector3 vector = BlockVector3.at(block.getX(), block.getY(), block.getZ());
                        ApplicableRegionSet regions = regionManager.getApplicableRegions(vector);
                        boolean isInsideTrap = false;

                        for (ProtectedRegion region : regions.getRegions()) {
                            if (region.getId().contains("_trap_")) {
                                isInsideTrap = true;
                                break;
                            }
                        }
                        if (isInsideTrap) continue;
                    }

                    if (random.nextDouble() * 100.0 <= chance) {
                         
                        if (!originalBlocks.containsKey(block.getLocation())) {
                            originalBlocks.put(block.getLocation(), block.getBlockData().clone());
                        }
                        block.setType(materialToPlace, false);
                    }
                }
            }
        }

         
        if (revertDelaySeconds > 0 && !originalBlocks.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
                    Block blockToRevert = entry.getKey().getBlock();

                    if (blockToRevert.getType() == materialToPlace) {
                        blockToRevert.setBlockData(entry.getValue(), false);
                    }
                }
            }, revertDelaySeconds * 20L);
        }
    }
}