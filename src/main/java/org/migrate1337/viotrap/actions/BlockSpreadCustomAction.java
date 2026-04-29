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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BlockSpreadCustomAction implements CustomAction {
    private final String targetType;
    private final double radius;
    private final Material materialToPlace;
    private final double chance;
    private final int revertDelaySeconds;
    private final Random random = new Random();

    public BlockSpreadCustomAction(String targetType, double radius, Material materialToPlace, double chance, int revertDelaySeconds) {
        this.targetType = targetType.toLowerCase();
        this.radius = radius;
        this.materialToPlace = materialToPlace;
        this.chance = chance;
        this.revertDelaySeconds = revertDelaySeconds;
    }

    @Override
    public void execute(Player primaryPlayer, Player[] opponents, VioTrap plugin) {
        if (primaryPlayer == null || !primaryPlayer.isOnline()) return;

        Location center = primaryPlayer.getLocation();
        int r = (int) Math.floor(radius);

        int y = center.getBlockY();

        // Храним локацию и точные данные блока (BlockData)
        Map<Location, BlockData> originalBlocks = new HashMap<>();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
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
                    // Сохраняем ТОЧНУЮ копию данных блока (воздух, трава, снег и т.д.)
                    originalBlocks.put(block.getLocation(), block.getBlockData().clone());

                    // Ставим наш блок ловушки
                    block.setType(materialToPlace, false); // false - не обновлять физику соседей при установке
                }
            }
        }

        if (revertDelaySeconds > 0 && !originalBlocks.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
                    Block blockToRevert = entry.getKey().getBlock();

                    // Важная проверка: возвращаем блок ТОЛЬКО если там до сих пор наша паутина/лава.
                    // Если игрок уже сам сломал блок или поставил туда обсидиан, мы его не трогаем!
                    if (blockToRevert.getType() == materialToPlace) {
                        blockToRevert.setBlockData(entry.getValue(), false);
                    }
                }
            }, revertDelaySeconds * 20L);
        }
    }
}