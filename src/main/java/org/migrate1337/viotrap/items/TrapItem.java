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
        if (amount <= 0) return new ItemStack(Material.AIR);
        String appliedSkin = skin == null || skin.isEmpty() ? "default" : skin;
        String materialName = VioTrap.getPlugin().getTrapType();
        Material material = materialName == null ? null : Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            material = Material.NETHERITE_SCRAP;
        }
        ItemStack item = new ItemStack(material, amount);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(VioTrap.getPlugin().getSkinDisplayName(appliedSkin));
            List<String> lore = VioTrap.getPlugin().getSkinDescription(appliedSkin);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(TRAP_ITEM_KEY, PersistentDataType.STRING, DEFAULT_TRAP_ID);
            meta.getPersistentDataContainer().set(SKIN_KEY, PersistentDataType.STRING, appliedSkin);


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
