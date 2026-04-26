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

import java.util.Arrays;

public class TemplateImportMenu implements Listener, InventoryHolder {
    private final VioTrap plugin;
    private final String menuTitle = "§8Импорт шаблона";

    public TemplateImportMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void open(Player player) {
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

        // Выводим все доступные шаблоны из конфига
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom_patterns");
        if (section != null) {
            int slot = 18;
            for (String patternName : section.getKeys(false)) {
                if (slot >= 54) break;

                ItemStack patternItem = new ItemStack(Material.BLAZE_POWDER);
                ItemMeta meta = patternItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§e" + patternName);
                    meta.setLore(Arrays.asList("§7Нажмите ЛКМ, чтобы импортировать", "§7этот рисунок в редактор."));
                    patternItem.setItemMeta(meta);
                }
                inv.setItem(slot++, patternItem);
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TemplateImportMenu)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Если игрок кликнул по шаблону
        if (clicked.getType() == Material.BLAZE_POWDER && clicked.hasItemMeta()) {
            String patternToImport = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            // Получаем текущую сессию игрока в редакторе
            EditorSession session = plugin.getParticleEditorManager().getSession(player);

            if (session != null) {
                // Накладываем точки
                plugin.getParticleEditorManager().loadPatternIntoSession(session, patternToImport);

                player.sendMessage("§aШаблон §e" + patternToImport + " §aуспешно наложен на вашу область!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                player.closeInventory();
            } else {
                player.sendMessage("§cОшибка: Ваша сессия редактирования не найдена.");
                player.closeInventory();
            }
        }
    }
}