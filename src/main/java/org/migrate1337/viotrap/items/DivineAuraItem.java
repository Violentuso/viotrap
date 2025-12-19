package org.migrate1337.viotrap.items;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;

public class DivineAuraItem {
    public static ItemStack getDivineAuraItem(int amount, VioTrap plugin) {
        Material material = plugin.getDivineAuraItemMaterial();
        String itemName = plugin.getDivineAuraItemName();
        List<String> itemDescription = plugin.getDivineAuraItemDescription();
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String var10001 = String.valueOf(ChatColor.GREEN);
            meta.setDisplayName(var10001 + itemName);
            meta.setLore(itemDescription);
            item.setItemMeta(meta);
        }

        return item;
    }
}
