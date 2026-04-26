package org.migrate1337.viotrap.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;
import org.migrate1337.viotrap.VioTrap;

import java.util.ArrayList;
import java.util.List;

public class PatternSelectMenu implements Listener {
    private final VioTrap plugin;
    private final String menuTitle = "§8Эффекты ловушек";

    public PatternSelectMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, menuTitle);

        // Кнопка отключения
        inv.setItem(4, createMenuItem(Material.BARRIER, "§cБез эффектов", null, false));

        String activeEffect = plugin.getConfig().getString("active_player_patterns." + player.getUniqueId().toString(), "");
        int slot = 9;

        // 1. Сначала выводим АНИМАЦИИ (как более приоритетные/крутые эффекты)
        ConfigurationSection animSection = plugin.getConfig().getConfigurationSection("custom_animations");
        if (animSection != null) {
            for (String animName : animSection.getKeys(false)) {
                if (slot >= 54) break;
                boolean isActive = animName.equals(activeEffect);
                // Используем NETHER_STAR для анимаций
                inv.setItem(slot++, createMenuItem(Material.NETHER_STAR, "§b§l[АНИМАЦИЯ] §f" + animName, "§7Тип: §fПодвижный эффект", isActive));
            }
        }
        ItemStack paletteItem = new ItemStack(Material.PAINTING);
        ItemMeta paletteMeta = paletteItem.getItemMeta();
        if (paletteMeta != null) {
            paletteMeta.setDisplayName("§dПалитра цветов");
            List<String> pLore = new ArrayList<>();
            pLore.add("§7Нажмите, чтобы изменить цвет");
            pLore.add("§7ваших анимаций и шаблонов.");
            paletteMeta.setLore(pLore);
            paletteItem.setItemMeta(paletteMeta);
        }
        inv.setItem(0, paletteItem);
        // 2. Затем выводим обычные СТАТИЧНЫЕ шаблоны
        ConfigurationSection patternSection = plugin.getConfig().getConfigurationSection("custom_patterns");
        if (patternSection != null) {
            for (String patternName : patternSection.getKeys(false)) {
                if (slot >= 54) break;
                boolean isActive = patternName.equals(activeEffect);
                // Используем BLAZE_POWDER для обычных паттернов
                inv.setItem(slot++, createMenuItem(Material.BLAZE_POWDER, "§e" + patternName, "§7Тип: §fСтатичный эффект", isActive));
            }
        }

        player.openInventory(inv);
    }

    // Вспомогательный метод для создания предметов в меню
    private ItemStack createMenuItem(Material mat, String name, String typeLore, boolean isActive) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            if (typeLore != null) lore.add(typeLore);

            if (isActive) {
                lore.add("§a▶ ВЫБРАНО");
                // Добавляем эффект зачарования для визуального выделения
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add("§7Нажмите, чтобы выбрать");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(menuTitle)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.BARRIER) {
            plugin.getConfig().set("active_player_patterns." + player.getUniqueId().toString(), null);
            plugin.saveConfig();
            player.sendMessage("§aЭффекты отключены.");
            player.closeInventory();
            return;
        }
        if (clicked.getType() == Material.PAINTING) {
            new ColorPaletteMenu(plugin).open(player);
            return;
        }
        if ((clicked.getType() == Material.BLAZE_POWDER || clicked.getType() == Material.NETHER_STAR) && clicked.hasItemMeta()) {
            String displayName = clicked.getItemMeta().getDisplayName();
            // Очищаем от всех префиксов, включая [АНИМАЦИЯ]
            String effectName = org.bukkit.ChatColor.stripColor(displayName)
                    .replace("[АНИМАЦИЯ] ", "");

            plugin.getConfig().set("active_player_patterns." + player.getUniqueId().toString(), effectName);
            plugin.saveConfig();

            player.sendMessage("§aВы выбрали эффект: §e" + effectName);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            open(player); // Обновляем меню
        }
    }
}