package org.migrate1337.viotrap.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;

import java.util.Arrays;

public class ParticleEditorMenu implements Listener {
    private final VioTrap plugin;
    private final String menuTitle = "§8Редактор партиклов";

    public ParticleEditorMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        // Увеличим меню до 54 слотов, чтобы влезли старые шаблоны
        Inventory inv = Bukkit.createInventory(null, 54, menuTitle);

        // Кнопка: Создать с нуля (Чистый куб)
        ItemStack cubePreset = new ItemStack(Material.STONE);
        ItemMeta cubeMeta = cubePreset.getItemMeta();
        if (cubeMeta != null) {
            cubeMeta.setDisplayName("§eСоздать с нуля (Куб 5x5x5)");
            cubeMeta.setLore(Arrays.asList(
                    "§7Нажмите, чтобы создать новый",
                    "§7чистый шаблон партиклов."
            ));
            cubePreset.setItemMeta(cubeMeta);
        }
        inv.setItem(4, cubePreset); // По центру сверху

        // Декоративное стекло
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, glass);
        }

        // Выводим все СУЩЕСТВУЮЩИЕ шаблоны для редактирования
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom_patterns");
        if (section != null) {
            int slot = 18;
            for (String patternName : section.getKeys(false)) {
                if (slot >= 54) break;

                ItemStack patternItem = new ItemStack(Material.BLAZE_POWDER);
                ItemMeta meta = patternItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§a" + patternName);
                    meta.setLore(Arrays.asList(
                            "§7Нажмите ЛКМ, чтобы открыть",
                            "§7и отредактировать этот шаблон."
                    ));
                    patternItem.setItemMeta(meta);
                }
                inv.setItem(slot++, patternItem);
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(menuTitle)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Генерируем новое имя для сохранения (как и раньше)
        String tempPatternName = player.getName() + "_pattern_" + (System.currentTimeMillis() / 1000);

        // 1. Если кликнул на Камень (Создать с нуля)
        if (clicked.getType() == Material.STONE) {
            player.closeInventory();
            // Передаем null, так как старого шаблона нет
            plugin.getParticleEditorManager().startEditorSession(player, tempPatternName, null);
            return;
        }

        // 2. Если кликнул на сохраненный шаблон (Редактировать старый)
        if (clicked.getType() == Material.BLAZE_POWDER && clicked.hasItemMeta()) {
            player.closeInventory();
            // Достаем имя шаблона без цветовых кодов (§a)
            String existingTemplate = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            // Запускаем сессию, передавая имя старого шаблона ТРЕТЬИМ аргументом!
            plugin.getParticleEditorManager().startEditorSession(player, tempPatternName, existingTemplate);
        }
    }
}