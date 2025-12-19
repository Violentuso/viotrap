package org.migrate1337.viotrap.items;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.migrate1337.viotrap.VioTrap;

public class TrapItem {
    private static final NamespacedKey TRAP_ITEM_KEY = new NamespacedKey(VioTrap.getPlugin(VioTrap.class), "trap_item_id");
    private static final NamespacedKey SKIN_KEY = new NamespacedKey(VioTrap.getPlugin(VioTrap.class), "trap_skin");
    private static final String DEFAULT_TRAP_ID = "default_trap";

    public static ItemStack getTrapItem(int amount, String skin) {
        ItemStack item = new ItemStack(Material.getMaterial(VioTrap.getPlugin().getTrapType()), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(VioTrap.getPlugin().getTrapDisplayName());
            List<String> lore = VioTrap.getPlugin().getTrapDescription();
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(TRAP_ITEM_KEY, PersistentDataType.STRING, "default_trap");


            item.setItemMeta(meta);
        }

        return item;
    }

    public static String getSkin(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(SKIN_KEY, PersistentDataType.STRING) ? (String)item.getItemMeta().getPersistentDataContainer().get(SKIN_KEY, PersistentDataType.STRING) : null;
    }

    public static boolean isTrapItem(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(TRAP_ITEM_KEY, PersistentDataType.STRING)) {
            String trapId = (String)item.getItemMeta().getPersistentDataContainer().get(TRAP_ITEM_KEY, PersistentDataType.STRING);
            return "default_trap".equals(trapId);
        } else {
            return false;
        }
    }

    public static NamespacedKey getSkinKey() {
        return SKIN_KEY;
    }
}
