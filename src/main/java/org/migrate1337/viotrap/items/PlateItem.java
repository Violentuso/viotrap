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
        ItemStack item = new ItemStack(Material.valueOf(VioTrap.getPlugin().getPlateType()), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(VioTrap.getPlugin().getPlateDisplayName());
            List<String> lore = VioTrap.getPlugin().getPlateDescription();
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(PLATE_ITEM_KEY, PersistentDataType.STRING, "static_plate_item_id");


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
