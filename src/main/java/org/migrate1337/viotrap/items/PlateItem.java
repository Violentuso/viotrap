package org.migrate1337.viotrap.items;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.migrate1337.viotrap.VioTrap;

public class PlateItem {
    private static final NamespacedKey PLATE_ITEM_KEY = new NamespacedKey(VioTrap.getPlugin(VioTrap.class), "plate_item_id");
    private static final NamespacedKey SKIN_KEY = new NamespacedKey(VioTrap.getPlugin(VioTrap.class), "plate_skin");
    private static final String STATIC_ITEM_ID = "static_plate_item_id";

    public static ItemStack getPlateItem(int amount, String skin) {
        if (amount <= 0) return new ItemStack(Material.AIR);
        String appliedSkin = skin == null || skin.isEmpty() ? "default" : skin;
        String materialName = VioTrap.getPlugin().getPlateType();
        Material material = materialName == null ? null : Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            material = Material.DRIED_KELP;
        }
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(VioTrap.getPlugin().getPlateSkinDisplayName(appliedSkin));
            List<String> lore = VioTrap.getPlugin().getPlateSkinDescription(appliedSkin);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(PLATE_ITEM_KEY, PersistentDataType.STRING, STATIC_ITEM_ID);
            meta.getPersistentDataContainer().set(SKIN_KEY, PersistentDataType.STRING, appliedSkin);


            item.setItemMeta(meta);
        }

        return item;
    }

    public static String getUniqueId(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(PLATE_ITEM_KEY, PersistentDataType.STRING) ? (String)item.getItemMeta().getPersistentDataContainer().get(PLATE_ITEM_KEY, PersistentDataType.STRING) : null;
    }

    public static String getSkin(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(SKIN_KEY, PersistentDataType.STRING) ? (String)item.getItemMeta().getPersistentDataContainer().get(SKIN_KEY, PersistentDataType.STRING) : null;
    }

    public static NamespacedKey getSkinKey() {
        return SKIN_KEY;
    }
}
