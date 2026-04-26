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
    private final Map<UUID, String> playerAnimNames = new HashMap<>();

    public AnimationCreatorMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return null; // Для идентификации через holder нам не нужно хранить инвентарь здесь
    }

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        playerAnimNames.putIfAbsent(uuid, player.getName() + "_anim_" + (System.currentTimeMillis() / 1000));
        playerFrames.putIfAbsent(uuid, new ArrayList<>());

        // Указываем 'this' (AnimationCreatorMenu) как владельца инвентаря
        Inventory inv = Bukkit.createInventory(this, 54, menuTitle);
        updateInventory(player, inv);
        player.openInventory(inv);
    }

    private void updateInventory(Player player, Inventory inv) {
        inv.clear();
        List<String> currentFrames = playerFrames.getOrDefault(player.getUniqueId(), new ArrayList<>());

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§eТаймлайн (" + currentFrames.size() + " кадров)");
            List<String> lore = new ArrayList<>();
            lore.add("§7Кадры по порядку:");
            if (currentFrames.isEmpty()) lore.add("§cПусто. Выберите шаблоны ниже.");
            else {
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
        inv.setItem(8, createItem(Material.LIME_DYE, "§aСохранить анимацию"));

        for (int i = 9; i < 18; i++) inv.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom_patterns");
        if (section != null) {
            int slot = 18;
            for (String patternName : section.getKeys(false)) {
                if (slot >= 54) break;
                inv.setItem(slot++, createItem(Material.BLAZE_POWDER, "§e" + patternName, "§7Нажмите, чтобы добавить кадр"));
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Проверка через Holder — самая надежная в Bukkit
        if (!(event.getInventory().getHolder() instanceof AnimationCreatorMenu)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // ФИКС: Если игрок кликает, но его сессии почему-то нет в памяти
        // (например, плагин перезагрузили), мы мгновенно создаем её заново
        playerFrames.putIfAbsent(uuid, new ArrayList<>());
        playerAnimNames.putIfAbsent(uuid, player.getName() + "_anim_" + (System.currentTimeMillis() / 1000));

        if (clicked.getType() == Material.RED_DYE) {
            playerFrames.get(uuid).clear();
            updateInventory(player, event.getInventory());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        } else if (clicked.getType() == Material.LIME_DYE) {
            List<String> frames = playerFrames.get(uuid);
            if (frames.isEmpty()) {
                player.sendMessage("§cВы не добавили ни одного кадра!");
                return;
            }

            String animName = playerAnimNames.get(uuid);
            plugin.getConfig().set("custom_animations." + animName, frames);
            plugin.saveConfig();

            player.sendMessage("§aАнимация '" + animName + "' сохранена!");
            player.closeInventory();

            playerFrames.remove(uuid);
            playerAnimNames.remove(uuid);

        } else if (clicked.getType() == Material.BLAZE_POWDER) {
            String patternName = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            playerFrames.get(uuid).add(patternName);
            updateInventory(player, event.getInventory());
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