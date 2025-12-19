package org.migrate1337.viotrap.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.utils.ColorUtil;

public class PlateSkinCreationMenu implements Listener {
    private final VioTrap plugin;
    private final Map<Player, String> currentSubMenu = new HashMap<>();
    private final Map<Player, String> editingField = new HashMap<>();

    public PlateSkinCreationMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "Создание скина для пласта");
        currentSubMenu.remove(player);
        updateMainMenuItems(inventory, player);
        player.openInventory(inventory);
    }

    private void updateMainMenu(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top != null && "Создание скина для пласта".equals(player.getOpenInventory().getTitle())) {
            updateMainMenuItems(top, player);
        } else {
            openMenu(player);
        }
    }

    private void updateMainMenuItems(Inventory inventory, Player player) {
        inventory.clear();

        int[] glass = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int i : glass) {
            inventory.setItem(i, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        }

        inventory.setItem(19, createMenuItem(Material.PAPER,
                ColorUtil.format("&#CCCAF0Название скина"),
                ColorUtil.format("&#AEC1F2 • Текущее: ") + plugin.getTempPlateSkinData().getOrDefault("name", "Не задано")));

        inventory.setItem(20, createMenuItem(Material.CHEST,
                ColorUtil.format("&#EAD7A2Схематики"),
                ColorUtil.format("&#AEC1F2 • Кликните, чтобы настроить")));

        // Основной звук
        inventory.setItem(23, createMenuItem(Material.NOTE_BLOCK,
                ColorUtil.format("&#AEC1F2Звук активации"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getTempPlateSkinData().getOrDefault("sound.type", "Не задано")));

        // Новый: звук завершения
        inventory.setItem(24, createMenuItem(Material.NOTE_BLOCK,
                ColorUtil.format("&#E6E6C2Звук завершения"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getTempPlateSkinData().getOrDefault("sound.type-ended", "Не задано")));

        inventory.setItem(25, createMenuItem(Material.CLOCK,
                ColorUtil.format("&#AEC1F2Кулдаун"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getTempPlateSkinData().getOrDefault("cooldown", "Не задано")));

        inventory.setItem(21, createMenuItem(Material.GLASS_PANE,
                ColorUtil.format("&#AEC1F2Длительность"),
                ColorUtil.format("&#AEC1F2 • Текущая: ") + plugin.getTempPlateSkinData().getOrDefault("duration", "Не задано")));

        inventory.setItem(31, createMenuItem(Material.EMERALD,
                ColorUtil.format("&#90EE90Сохранить скин"),
                ColorUtil.format("&#AEC1F2 • Кликните, чтобы сохранить")));
    }

    // ==================== СХЕМАТИКИ ====================
    private void openSchematicsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "Схематики пласта");
        currentSubMenu.put(player, "schematics");
        updateSchematicsMenuItems(inv, player);
        player.openInventory(inv);
    }

    private void updateSchematicsMenuItems(Inventory inv, Player player) {
        inv.clear();
        int[] glass = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int i : glass) inv.setItem(i, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));

        Map<String, String> data = plugin.getTempPlateSkinData();

        inv.setItem(19, createMenuItem(Material.CHEST, ColorUtil.format("&#EAD7A2Forward Schematic"), ColorUtil.format("&#AEC1F2 • Текущая: ") + data.getOrDefault("forward_schematic", "Не задано")));
        inv.setItem(20, createMenuItem(Material.CHEST, ColorUtil.format("&#EAD7A2Forward Left Schematic"), ColorUtil.format("&#AEC1F2 • Текущая: ") + data.getOrDefault("forward_left_schematic", "Не задано")));
        inv.setItem(21, createMenuItem(Material.CHEST, ColorUtil.format("&#EAD7A2Forward Right Schematic"), ColorUtil.format("&#AEC1F2 • Текущая: ") + data.getOrDefault("forward_right_schematic", "Не задано")));
        inv.setItem(22, createMenuItem(Material.CHEST, ColorUtil.format("&#EAD7A2Backward Schematic"), ColorUtil.format("&#AEC1F2 • Текущая: ") + data.getOrDefault("backward_schematic", "Не задано")));
        inv.setItem(23, createMenuItem(Material.CHEST, ColorUtil.format("&#EAD7A2Backward Left Schematic"), ColorUtil.format("&#AEC1F2 • Текущая: ") + data.getOrDefault("backward_left_schematic", "Не задано")));
        inv.setItem(24, createMenuItem(Material.CHEST, ColorUtil.format("&#EAD7A2Backward Right Schematic"), ColorUtil.format("&#AEC1F2 • Текущая: ") + data.getOrDefault("backward_right_schematic", "Не задано")));
        inv.setItem(25, createMenuItem(Material.CHEST, ColorUtil.format("&#EAD7A2Left Schematic"), ColorUtil.format("&#AEC1F2 • Текущая: ") + data.getOrDefault("left_schematic", "Не задано")));
        inv.setItem(28, createMenuItem(Material.CHEST, ColorUtil.format("&#EAD7A2Right Schematic"), ColorUtil.format("&#AEC1F2 • Текущая: ") + data.getOrDefault("right_schematic", "Не задано")));
        inv.setItem(29, createMenuItem(Material.CHEST, ColorUtil.format("&#EAD7A2Up Schematic"), ColorUtil.format("&#AEC1F2 • Текущая: ") + data.getOrDefault("up_schematic", "Не задано")));
        inv.setItem(30, createMenuItem(Material.CHEST, ColorUtil.format("&#EAD7A2Down Schematic"), ColorUtil.format("&#AEC1F2 • Текущая: ") + data.getOrDefault("down_schematic", "Не задано")));

        inv.setItem(49, createMenuItem(Material.BARRIER, ColorUtil.format("&#FF5555Вернуться назад"), ColorUtil.format("&#AEC1F2 • Кликните, чтобы вернуться")));
    }

    private ItemStack createMenuItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.equals("Создание скина для пласта") &&
                !title.equals("Схематики пласта")) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        String sub = currentSubMenu.getOrDefault(player, "");

        // Главное меню
        if (title.equals("Создание скина для пласта") && sub.isEmpty()) {
            int slot = event.getSlot();
            if (slot == 19) handleInput(player, "name", "Введите название скина:");
            else if (slot == 21) Bukkit.getScheduler().runTask(plugin, () -> openSchematicsMenu(player));
            else if (slot == 23) handleInput(player, "sound.type", "Введите тип звука активации:");
            else if (slot == 24) handleInput(player, "sound.type-ended", "Введите тип звука завершения:"); // Новый слот
            else if (slot == 25) handleInput(player, "cooldown", "Введите кулдаун в секундах:");
            else if (slot == 29) handleInput(player, "duration", "Введите длительность в секундах:");
            else if (slot == 31) saveSkin(player);
        }

        // Схематики
        else if (sub.equals("schematics")) {
            int slot = event.getSlot();
            if (slot == 19) handleInput(player, "forward_schematic", "Введите forward schematic:");
            else if (slot == 20) handleInput(player, "forward_left_schematic", "Введите forward left schematic:");
            else if (slot == 21) handleInput(player, "forward_right_schematic", "Введите forward right schematic:");
            else if (slot == 22) handleInput(player, "backward_schematic", "Введите backward schematic:");
            else if (slot == 23) handleInput(player, "backward_left_schematic", "Введите backward left schematic:");
            else if (slot == 24) handleInput(player, "backward_right_schematic", "Введите backward right schematic:");
            else if (slot == 25) handleInput(player, "left_schematic", "Введите left schematic:");
            else if (slot == 28) handleInput(player, "right_schematic", "Введите right schematic:");
            else if (slot == 29) handleInput(player, "up_schematic", "Введите up schematic:");
            else if (slot == 30) handleInput(player, "down_schematic", "Введите down schematic:");
            else if (slot == 49) {
                currentSubMenu.remove(player);
                Bukkit.getScheduler().runTask(plugin, () -> updateMainMenu(player));
            }
        }
    }

    private void handleInput(Player player, String field, String message) {
        player.closeInventory();
        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] " + message));
        editingField.put(player, field);

        plugin.getChatInputHandler().waitForInput(player, input -> {
            plugin.getTempPlateSkinData().put(field, input);
            editingField.remove(player);

            Bukkit.getScheduler().runTask(plugin, () -> {
                String sub = currentSubMenu.getOrDefault(player, "");
                if ("schematics".equals(sub)) openSchematicsMenu(player);
                else updateMainMenu(player);
            });
        });
    }

    private void saveSkin(Player player) {
        Map<String, String> data = plugin.getTempPlateSkinData();

        // Проверка обязательных полей
        if (data.get("name") == null ||
                data.get("sound.type") == null ||
                data.get("cooldown") == null ||
                data.get("duration") == null) {
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AЗаполните все обязательные поля!"));
            return;
        }

        String name = data.get("name");
        String[] schemKeys = {"forward", "forward_left", "forward_right", "backward", "backward_left",
                "backward_right", "left", "right", "up", "down"};
        for (String k : schemKeys) {
            if (!data.containsKey(k + "_schematic") || "Не задано".equals(data.get(k + "_schematic"))) {
                player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AУкажите все схематики!"));
                return;
            }
            plugin.getConfig().set("plate_skins." + name + "." + k + "_schematic", data.get(k + "_schematic"));
        }

        // Основной звук
        plugin.getConfig().set("plate_skins." + name + ".sound.type", data.get("sound.type"));
        plugin.getConfig().set("plate_skins." + name + ".sound.volume", 1.0F);
        plugin.getConfig().set("plate_skins." + name + ".sound.pitch", 1.0F);

        // Звук завершения (новое)
        String endedSound = data.getOrDefault("sound.type-ended", "ENTITY_GENERIC_EXPLODE"); // значение по умолчанию, если не задано
        plugin.getConfig().set("plate_skins." + name + ".sound.type-ended", endedSound);
        plugin.getConfig().set("plate_skins." + name + ".sound.volume-ended", 1.0F);
        plugin.getConfig().set("plate_skins." + name + ".sound.pitch-ended", 1.0F);

        plugin.getConfig().set("plate_skins." + name + ".cooldown", Integer.parseInt(data.get("cooldown")));
        plugin.getConfig().set("plate_skins." + name + ".duration", Integer.parseInt(data.get("duration")));

        plugin.saveConfig();
        plugin.getTempPlateSkinData().clear();

        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Скин '" + name + "' сохранён!"));
        player.closeInventory();
    }
}