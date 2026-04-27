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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PatternSelectMenu implements Listener {
    private final VioTrap plugin;
    private final String menuTitle = "§8Эффекты ловушек";

    // Память: кто на какой странице сейчас находится
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public PatternSelectMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    // Для вызова меню из других мест (открывает 1-ю страницу)
    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, menuTitle);

        // Кнопка Палитры цветов
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

        // Кнопка отключения
        inv.setItem(4, createMenuItem(Material.BARRIER, "§cБез эффектов", null, false));

        String activeEffect = plugin.getConfig().getString("active_player_patterns." + player.getUniqueId().toString(), "");

        // Собираем ВСЕ предметы в один список
        List<ItemStack> allItems = new ArrayList<>();

        // 1. АНИМАЦИИ
        ConfigurationSection animSection = plugin.getConfig().getConfigurationSection("custom_animations");
        if (animSection != null) {
            for (String animName : animSection.getKeys(false)) {
                boolean isActive = animName.equals(activeEffect);
                allItems.add(createMenuItem(Material.NETHER_STAR, "§b§l[АНИМАЦИЯ] §f" + animName, "§7Тип: §fПодвижный эффект", isActive));
            }
        }

        // 2. СТАТИЧНЫЕ ШАБЛОНЫ
        ConfigurationSection patternSection = plugin.getConfig().getConfigurationSection("custom_patterns");
        if (patternSection != null) {
            for (String patternName : patternSection.getKeys(false)) {
                boolean isActive = patternName.equals(activeEffect);
                allItems.add(createMenuItem(Material.BLAZE_POWDER, "§e" + patternName, "§7Тип: §fСтатичный эффект", isActive));
            }
        }

        // --- МАТЕМАТИКА СТРАНИЦ ---
        int itemsPerPage = 36; // Слоты с 9 по 44
        int totalPages = (int) Math.ceil((double) allItems.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        // Защита от выхода за пределы
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        playerPages.put(player.getUniqueId(), page);

        // Выводим только те предметы, которые попадают на текущую страницу
        int startIndex = page * itemsPerPage;
        int slot = 9;
        for (int i = startIndex; i < Math.min(startIndex + itemsPerPage, allItems.size()); i++) {
            inv.setItem(slot++, allItems.get(i));
        }

        // Кнопки навигации (45 и 53 слоты)
        if (page > 0) {
            inv.setItem(45, createNavButton("§a◀ Предыдущая страница"));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, createNavButton("§aСледующая страница ▶"));
        }

        player.openInventory(inv);
    }

    private ItemStack createNavButton(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMenuItem(Material mat, String name, String typeLore, boolean isActive) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            if (typeLore != null) lore.add(typeLore);

            if (isActive) {
                lore.add("§a▶ ВЫБРАНО");
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

        // Обработка стрелочек
        if (clicked.getType() == Material.ARROW && clicked.hasItemMeta()) {
            String name = clicked.getItemMeta().getDisplayName();
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);

            if (name.contains("Предыдущая")) {
                open(player, currentPage - 1);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            } else if (name.contains("Следующая")) {
                open(player, currentPage + 1);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            plugin.getConfig().set("active_player_patterns." + player.getUniqueId().toString(), null);
            plugin.saveConfig();
            plugin.getParticleCacheManager().reloadCache();
            player.sendMessage("§aЭффекты отключены.");
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.PAINTING) {
            new org.migrate1337.viotrap.gui.ColorPaletteMenu(plugin).open(player);
            return;
        }

        if ((clicked.getType() == Material.BLAZE_POWDER || clicked.getType() == Material.NETHER_STAR) && clicked.hasItemMeta()) {
            String displayName = clicked.getItemMeta().getDisplayName();
            String effectName = org.bukkit.ChatColor.stripColor(displayName).replace("[АНИМАЦИЯ] ", "");

            plugin.getConfig().set("active_player_patterns." + player.getUniqueId().toString(), effectName);
            plugin.saveConfig();
            plugin.getParticleCacheManager().reloadCache();

            player.sendMessage("§aВы выбрали эффект: §e" + effectName);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

            // Обновляем текущую страницу, чтобы загорелось "ВЫБРАНО"
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            open(player, currentPage);
        }
    }
}