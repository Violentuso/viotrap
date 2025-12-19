package org.migrate1337.viotrap.data;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

public class TrapBlockData {
    private final Material material;
    private final BlockData blockData; // Это Bukkit BlockData
    private ItemStack[] contents;
    private String spawnedType;
    private boolean isDoubleChest;
    private Location pairedChestLocation;

    public TrapBlockData(Material material, BlockData blockData, ItemStack[] contents, String spawnedType) {
        this.material = material;
        this.blockData = blockData;
        this.contents = contents;
        this.spawnedType = spawnedType;
        this.isDoubleChest = false;
        this.pairedChestLocation = null;
    }

    public Material getMaterial() {
        return this.material;
    }

    public BlockData getBlockData() {
        return this.blockData;
    }

    public ItemStack[] getContents() {
        return this.contents;
    }

    public void setContents(ItemStack[] contents) {
        this.contents = contents;
    }

    public String getSpawnedType() {
        return this.spawnedType;
    }

    public void setSpawnedType(String spawnedType) {
        this.spawnedType = spawnedType;
    }

    public boolean isDoubleChest() {
        return this.isDoubleChest;
    }

    public void setDoubleChest(boolean isDoubleChest) {
        this.isDoubleChest = isDoubleChest;
    }

    public Location getPairedChestLocation() {
        return this.pairedChestLocation;
    }

    public void setPairedChestLocation(Location pairedChestLocation) {
        this.pairedChestLocation = pairedChestLocation;
    }
}
