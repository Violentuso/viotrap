package org.migrate1337.viotrap.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.migrate1337.viotrap.data.TrapBlockData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class BlockDataSerializer {

    public static void saveBlocksToConfig(ConfigurationSection parentSection, String key, Map<Location, TrapBlockData> blocks) {
        if (blocks == null || blocks.isEmpty()) return;

        ConfigurationSection blocksSection = parentSection.createSection(key);
        int index = 0;

        for (Map.Entry<Location, TrapBlockData> entry : blocks.entrySet()) {
            Location loc = entry.getKey();
            TrapBlockData data = entry.getValue();
            if (loc.getWorld() == null) continue;

            ConfigurationSection blockSection = blocksSection.createSection("b" + index);
            blockSection.set("w", loc.getWorld().getName());
            blockSection.set("x", loc.getBlockX());
            blockSection.set("y", loc.getBlockY());
            blockSection.set("z", loc.getBlockZ());
            blockSection.set("m", data.getMaterial().name());
            blockSection.set("bd", data.getBlockData().getAsString());

            if (data.getContents() != null) {
                String encoded = itemsToBase64(data.getContents());
                if (encoded != null) {
                    blockSection.set("inv", encoded);
                }
            }

            if (data.getSpawnedType() != null) {
                blockSection.set("st", data.getSpawnedType());
            }

            if (data.isDoubleChest()) {
                blockSection.set("dc", true);
            }

            if (data.getPairedChestLocation() != null) {
                Location paired = data.getPairedChestLocation();
                if (paired.getWorld() != null) {
                    blockSection.set("pw", paired.getWorld().getName());
                    blockSection.set("px", paired.getBlockX());
                    blockSection.set("py", paired.getBlockY());
                    blockSection.set("pz", paired.getBlockZ());
                }
            }

            index++;
        }
    }

    public static Map<Location, TrapBlockData> loadBlocksFromConfig(ConfigurationSection parentSection, String key) {
        Map<Location, TrapBlockData> blocks = new HashMap<>();
        ConfigurationSection blocksSection = parentSection.getConfigurationSection(key);
        if (blocksSection == null) return blocks;

        for (String blockKey : blocksSection.getKeys(false)) {
            ConfigurationSection blockSection = blocksSection.getConfigurationSection(blockKey);
            if (blockSection == null) continue;

            String worldName = blockSection.getString("w");
            if (worldName == null) continue;
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            int x = blockSection.getInt("x");
            int y = blockSection.getInt("y");
            int z = blockSection.getInt("z");
            Location loc = new Location(world, x, y, z);

            String materialName = blockSection.getString("m", "AIR");
            Material material = Material.getMaterial(materialName);
            if (material == null) material = Material.AIR;

            BlockData blockData;
            try {
                blockData = Bukkit.createBlockData(blockSection.getString("bd", "minecraft:air"));
            } catch (Exception e) {
                blockData = material.createBlockData();
            }

            ItemStack[] contents = null;
            if (blockSection.contains("inv")) {
                contents = itemsFromBase64(blockSection.getString("inv"));
            }

            String spawnedType = blockSection.getString("st", null);

            TrapBlockData trapBlockData = new TrapBlockData(material, blockData, contents, spawnedType);
            trapBlockData.setDoubleChest(blockSection.getBoolean("dc", false));

            if (blockSection.contains("pw")) {
                World pairedWorld = Bukkit.getWorld(blockSection.getString("pw"));
                if (pairedWorld != null) {
                    Location pairedLoc = new Location(pairedWorld,
                            blockSection.getInt("px"),
                            blockSection.getInt("py"),
                            blockSection.getInt("pz"));
                    trapBlockData.setPairedChestLocation(pairedLoc);
                }
            }

            blocks.put(loc, trapBlockData);
        }

        return blocks;
    }

    public static String itemsToBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            Bukkit.getLogger().warning("[VioTrap] Failed to serialize inventory contents: " + e.getMessage());
            return null;
        }
    }

    public static ItemStack[] itemsFromBase64(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[VioTrap] Failed to deserialize inventory contents: " + e.getMessage());
            return null;
        }
    }
}
