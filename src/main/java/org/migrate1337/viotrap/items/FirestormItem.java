package org.migrate1337.viotrap.items;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;

public class FirestormItem {
    public static ItemStack getFirestormItem(int amount, VioTrap plugin) {
        ItemStack item = new ItemStack(Material.valueOf(VioTrap.getPlugin().getFirestormItemType()), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(VioTrap.getPlugin().getFirestormItemName());
            List<String> itemDescription = VioTrap.getPlugin().getFirestormItemDescription();
            meta.setLore(itemDescription);
            item.setItemMeta(meta);
        }

        return item;
    }
}
