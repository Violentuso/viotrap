package org.migrate1337.viotrap.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;

import java.util.Arrays;

public class ColorPaletteMenu implements Listener, InventoryHolder {
    private final VioTrap plugin;
    private final String menuTitle = "§8Выбор цвета эффекта";
    public static final java.util.Set<java.util.UUID> waitingForColor = new java.util.HashSet<>();
    public ColorPaletteMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(this, 27, menuTitle);
        ItemStack customColor = new ItemStack(Material.NAME_TAG);
        ItemMeta customMeta = customColor.getItemMeta();
        if (customMeta != null) {
            customMeta.setDisplayName("§dСвой цвет");
            customMeta.setLore(Arrays.asList("§7Нажмите, чтобы ввести", "§7свой RGB или HEX код в чат."));
            customColor.setItemMeta(customMeta);
        }
        inv.setItem(25, customColor);
        // Расставляем базовые цвета
        addColor(inv, 10, Material.RED_DYE, "§cКрасный", "255,0,0");
        addColor(inv, 11, Material.ORANGE_DYE, "§6Оранжевый", "255,165,0");
        addColor(inv, 12, Material.YELLOW_DYE, "§eЖелтый", "255,255,0");
        addColor(inv, 13, Material.LIME_DYE, "§aЛаймовый", "0,255,0");
        addColor(inv, 14, Material.CYAN_DYE, "§bГолубой", "0,255,255");
        addColor(inv, 15, Material.BLUE_DYE, "§9Синий", "0,0,255");
        addColor(inv, 16, Material.PURPLE_DYE, "§5Фиолетовый", "128,0,128");

        addColor(inv, 21, Material.PINK_DYE, "§dРозовый", "255,192,203");
        addColor(inv, 22, Material.WHITE_DYE, "§fБелый", "255,255,255");
        addColor(inv, 23, Material.INK_SAC, "§8Черный", "0,0,0");

        // Кнопка возврата (можно сделать переход обратно в меню эффектов)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§7Назад к эффектам");
            back.setItemMeta(backMeta);
        }
        inv.setItem(26, back);

        player.openInventory(inv);
    }

    private void addColor(Inventory inv, int slot, Material mat, String name, String rgb) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            // Прячем RGB код в лор (невидимым или серым текстом), чтобы потом легко его достать при клике
            meta.setLore(Arrays.asList("§7Нажмите, чтобы выбрать", "§8RGB: " + rgb));
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ColorPaletteMenu)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.NAME_TAG && clicked.hasItemMeta() && clicked.getItemMeta().getDisplayName().equals("§dСвой цвет")) {
            player.closeInventory();
            waitingForColor.add(player.getUniqueId()); // Помещаем игрока в режим ожидания
            player.sendMessage("§eВведите цвет в чат (например: §f255,100,50 §eили §f#FF5533§e):");
            player.sendMessage("§7Напишите 'отмена' для выхода.");
            return;
        }
        if (clicked.getType() == Material.ARROW) {
            // Возвращаем в меню выбора эффектов
            new PatternSelectMenu(plugin).open(player);
            return;
        }

        if (clicked.hasItemMeta() && clicked.getItemMeta().getLore() != null) {
            // Достаем наш RGB код из второй строчки лора "§8RGB: 255,0,0"
            String loreLine = clicked.getItemMeta().getLore().get(1);
            String rgbStr = loreLine.replace("§8RGB: ", "");

            if (plugin.getParticleEditorManager().isEditing(player)) {
                plugin.getParticleEditorManager().getSession(player).setCurrentBrushColor(rgbStr);
                player.sendMessage("§aВы обмакнули кисть в новый цвет! §7(RGB: " + rgbStr + ")");
            } else {
                // На всякий случай оставляем логику для конфига, если вдруг он открыл не из редактора
                plugin.getConfig().set("active_player_colors." + player.getUniqueId().toString(), rgbStr);
                plugin.saveConfig();
                player.sendMessage("§aВы успешно изменили цвет эффекта!");
            }
        }
    }
}