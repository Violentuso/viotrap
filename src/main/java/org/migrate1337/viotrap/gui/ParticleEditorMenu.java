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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ParticleEditorMenu implements Listener {
    private final VioTrap plugin;
    private final String menuTitle = "§8Редактор партиклов";

     
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public ParticleEditorMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, menuTitle);

         
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
        inv.setItem(4, cubePreset);

         
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, glass);
        }

         
        List<String> allPatterns = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom_patterns");
        if (section != null) {
            allPatterns.addAll(section.getKeys(false));
        }

         
        int itemsPerPage = 27;  
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
                meta.setDisplayName("§a" + patternName);
                meta.setLore(Arrays.asList(
                        "§7Нажмите ЛКМ, чтобы открыть",
                        "§7и отредактировать этот шаблон."
                ));
                patternItem.setItemMeta(meta);
            }
            inv.setItem(slot++, patternItem);
        }

         
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

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(menuTitle)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

         
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

        if (clicked.getType() == Material.STONE) {
            player.closeInventory();
            player.sendMessage("§e[VioTrap] §aВведите название для нового шаблона (без пробелов):");

            plugin.getChatInputHandler().waitForInput(player, (input) -> {
                 
                String safeName = input.replace(" ", "_").replaceAll("[^a-zA-Z0-9_\\-]", "");

                if (safeName.isEmpty()) {
                    player.sendMessage("§cНекорректное название!");
                    return;
                }


                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getParticleEditorManager().startEditorSession(player, safeName, null);
                });
            });
            return;
        }

        if (clicked.getType() == Material.BLAZE_POWDER && clicked.hasItemMeta()) {
            player.closeInventory();
            String existingTemplate = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
             
            plugin.getParticleEditorManager().startEditorSession(player, existingTemplate, existingTemplate);
        }
    }
}