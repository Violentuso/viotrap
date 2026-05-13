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

import java.util.*;

public class AnimationCreatorMenu implements Listener, InventoryHolder {
    private final VioTrap plugin;
    private final String menuTitle = "§8Создание анимации";
    private final Map<UUID, List<String>> playerFrames = new HashMap<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public AnimationCreatorMenu(VioTrap plugin) {
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
        UUID uuid = player.getUniqueId();

         
        playerFrames.putIfAbsent(uuid, new ArrayList<>());

        Inventory inv = Bukkit.createInventory(this, 54, menuTitle);
        updateInventory(player, inv, page);
        player.openInventory(inv);
    }

    private void updateInventory(Player player, Inventory inv, int page) {
        UUID uuid = player.getUniqueId();
        inv.clear();

         
        List<String> currentFrames = playerFrames.computeIfAbsent(uuid, k -> new ArrayList<>());

         
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§eТаймлайн (" + currentFrames.size() + " кадров)");
            List<String> lore = new ArrayList<>();
            lore.add("§7Имя: §fБудет указано при сохранении");  
            lore.add("§7Кадры по порядку:");
            if (currentFrames.isEmpty()) {
                lore.add("§cПусто. Выберите шаблоны ниже.");
            } else {
                for (int i = 0; i < Math.min(currentFrames.size(), 15); i++) {
                    lore.add("§7" + (i + 1) + ". §a" + currentFrames.get(i));
                }
                if (currentFrames.size() > 15) lore.add("§8...и еще " + (currentFrames.size() - 15));
            }
            infoMeta.setLore(lore);
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(0, infoItem);

         
        inv.setItem(4, createItem(Material.RED_DYE, "§cОчистить кадры"));
        inv.setItem(8, createItem(Material.LIME_DYE, "§aСохранить анимацию", "§7Нажмите, чтобы задать", "§7название и сохранить."));

         
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
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
        playerPages.put(uuid, page);

         
        int startIndex = page * itemsPerPage;
        int slot = 18;
        for (int i = startIndex; i < Math.min(startIndex + itemsPerPage, allPatterns.size()); i++) {
            inv.setItem(slot++, createItem(Material.BLAZE_POWDER, "§e" + allPatterns.get(i), "§7Нажмите, чтобы добавить кадр"));
        }

         
        if (page > 0) inv.setItem(45, createItem(Material.ARROW, "§a◀ Предыдущая страница"));
        if (page < totalPages - 1) inv.setItem(53, createItem(Material.ARROW, "§aСледующая страница ▶"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AnimationCreatorMenu)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

         
        playerFrames.putIfAbsent(uuid, new ArrayList<>());

        int currentPage = playerPages.getOrDefault(uuid, 0);

         
        if (clicked.getType() == Material.ARROW && clicked.hasItemMeta()) {
            if (clicked.getItemMeta().getDisplayName().contains("◀")) {
                updateInventory(player, event.getInventory(), currentPage - 1);
            } else {
                updateInventory(player, event.getInventory(), currentPage + 1);
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

         
        if (clicked.getType() == Material.RED_DYE) {
            playerFrames.get(uuid).clear();
            updateInventory(player, event.getInventory(), currentPage);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        } else if (clicked.getType() == Material.LIME_DYE) {
            List<String> frames = playerFrames.get(uuid);
            if (frames == null || frames.isEmpty()) {
                player.sendMessage("§cВы не добавили ни одного кадра!");
                return;
            }

            player.closeInventory();
            player.sendMessage("§e[VioTrap] §aВведите название для новой анимации (без пробелов):");

             
            plugin.getChatInputHandler().waitForInput(player, (input) -> {
                String safeName = input.replace(" ", "_").replaceAll("[^a-zA-Z0-9_\\-]", "");

                if (safeName.isEmpty()) {
                    player.sendMessage("§cНекорректное название! Сохранение отменено.");
                    return;
                }

                 
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getConfig().set("custom_animations." + safeName, frames);
                    plugin.saveConfig();

                     
                    if (plugin.getParticleCacheManager() != null) {
                        plugin.getParticleCacheManager().reloadCache();
                    }

                    player.sendMessage("§aАнимация '" + safeName + "' успешно сохранена!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

                    playerFrames.remove(uuid);
                });
            });

        } else if (clicked.getType() == Material.BLAZE_POWDER) {
            String patternName = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            playerFrames.get(uuid).add(patternName);
            updateInventory(player, event.getInventory(), currentPage);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}