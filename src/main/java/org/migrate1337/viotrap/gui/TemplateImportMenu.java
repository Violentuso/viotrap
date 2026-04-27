package org.migrate1337.viotrap.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.editor.EditorSession;

import java.util.*;

public class TemplateImportMenu implements Listener, InventoryHolder {
    private final VioTrap plugin;
    private final String menuTitle = "§8Импорт шаблона";
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public TemplateImportMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(this, 54, menuTitle);

        // Информационная кнопка
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§eБиблиотека шаблонов");
            infoMeta.setLore(Arrays.asList(
                    "§7Выберите шаблон из списка ниже.",
                    "§7Он будет скопирован и наложен",
                    "§7в вашу текущую рабочую область."
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        // Декоративное стекло
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) { glassMeta.setDisplayName(" "); glass.setItemMeta(glassMeta); }
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, glass);
        }

        // Собираем список всех шаблонов
        List<String> allPatterns = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom_patterns");
        if (section != null) {
            allPatterns.addAll(section.getKeys(false));
        }

        // Пагинация
        int itemsPerPage = 27; // Слоты 18-44
        int totalPages = (int) Math.ceil((double) allPatterns.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        playerPages.put(player.getUniqueId(), page);

        int startIndex = page * itemsPerPage;
        int slot = 18;
        for (int i = startIndex; i < Math.min(startIndex + itemsPerPage, allPatterns.size()); i++) {
            String patternName = allPatterns.get(i);
            ItemStack patternItem = new ItemStack(Material.BLAZE_POWDER);
            ItemMeta meta = patternItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + patternName);
                meta.setLore(Arrays.asList("§7Нажмите ЛКМ, чтобы импортировать", "§7этот рисунок в редактор."));
                patternItem.setItemMeta(meta);
            }
            inv.setItem(slot++, patternItem);
        }

        // Кнопки навигации
        if (page > 0) inv.setItem(45, createNavButton("§a◀ Предыдущая страница"));
        if (page < totalPages - 1) inv.setItem(53, createNavButton("§aСледующая страница ▶"));

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

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TemplateImportMenu)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Навигация
        if (clicked.getType() == Material.ARROW) {
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            if (clicked.getItemMeta().getDisplayName().contains("◀")) {
                open(player, currentPage - 1);
            } else {
                open(player, currentPage + 1);
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        if (clicked.getType() == Material.BLAZE_POWDER && clicked.hasItemMeta()) {
            String patternToImport = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            EditorSession session = plugin.getParticleEditorManager().getSession(player);

            if (session != null) {
                plugin.getParticleEditorManager().loadPatternIntoSession(session, patternToImport);
                player.sendMessage("§aШаблон §e" + patternToImport + " §aуспешно наложен!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                player.closeInventory();
            }
        }
    }
}