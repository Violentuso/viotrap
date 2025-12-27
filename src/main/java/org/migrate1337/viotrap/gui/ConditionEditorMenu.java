package org.migrate1337.viotrap.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.conditions.ConditionType;
import org.migrate1337.viotrap.utils.ColorUtil;

import java.util.*;

public class ConditionEditorMenu implements Listener {

    private final VioTrap plugin;
    private final Map<Player, String> editingSection = new HashMap<>();
    private final Map<Player, ConditionType> editingConditionType = new HashMap<>();

    public ConditionEditorMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, "Редактор условий: Выбор");
        fillBorders(inv);

        inv.setItem(10, createItem(Material.TRIPWIRE_HOOK, "&#EAD7A2Трапка", "&7&lНажмите для настройки"));
        inv.setItem(11, createItem(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, "&#9ABDADПласт", "&7&lНажмите для настройки"));
        inv.setItem(12, createItem(Material.END_ROD, "&#AEC1F2Явная пыль", "&7&lНажмите для настройки"));
        inv.setItem(13, createItem(Material.BLAZE_ROD, "&#FF5555Огненный смерч", "&7&lНажмите для настройки"));
        inv.setItem(14, createItem(Material.ENDER_EYE, "&#CD8B62Дезориентация", "&7&lНажмите для настройки"));
        inv.setItem(15, createItem(Material.GHAST_TEAR, "&#FFFF55Божья аура", "&7&lНажмите для настройки"));

        player.openInventory(inv);
        editingSection.remove(player);
    }

    private void openConditionsList(Player player, String sectionKey) {
        editingSection.put(player, sectionKey);
        Inventory inv = Bukkit.createInventory(null, 54, "Условия: " + sectionKey);
        fillBorders(inv);

        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionKey + ".conditions");
        int slot = 10;

        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    ConditionType type = ConditionType.valueOf(key.toUpperCase());
                    String value = section.getString(key);

                    ItemStack item = createItem(type.getIcon(),
                            "&#EAD7A2" + type.getDisplayName(),
                            "&#AEC1F2 • Значение: &#FFFFFF" + value,
                            "",
                            "&#55FF55ЛКМ - Изменить",
                            "&#FF5555ПКМ - Удалить");

                    ItemMeta meta = item.getItemMeta();
                    meta.setLocalizedName(type.name());
                    item.setItemMeta(meta);

                    inv.setItem(slot++, item);

                    if ((slot + 1) % 9 == 0) slot += 2;
                } catch (IllegalArgumentException ignored) {}
            }
        }

        inv.setItem(40, createItem(Material.EMERALD, "&#90EE90Добавить условие", ""));
        inv.setItem(49, createItem(Material.BARRIER, "&#FF5555Назад", ""));

        player.openInventory(inv);
    }

    private void openAddConditionMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "Добавить условие"); // Увеличено до 54 для новых условий
        fillBorders(inv);

        int slot = 10;
        for (ConditionType type : ConditionType.values()) {
            ItemStack item = createItem(type.getIcon(), "&#EAD7A2" + type.getDisplayName(), "&#777777" + type.getDescription());
            ItemMeta meta = item.getItemMeta();
            meta.setLocalizedName(type.name());
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
            if ((slot + 1) % 9 == 0) slot += 2; // Перенос строки
        }

        inv.setItem(49, createItem(Material.BARRIER, "&#FF5555Назад", ""));
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack currentItem = event.getCurrentItem();

        if (title.startsWith("Редактор условий") || title.startsWith("Условия:") || title.equals("Добавить условие")) {
            event.setCancelled(true);
            if (currentItem == null || currentItem.getType() == Material.AIR || currentItem.getType() == Material.LIGHT_BLUE_STAINED_GLASS_PANE) return;

            if (title.equals("Редактор условий: Выбор")) {
                String section = null;
                switch (event.getSlot()) {
                    case 10: section = "trap"; break;
                    case 11: section = "plate"; break;
                    case 12: section = "reveal_item"; break;
                    case 13: section = "firestorm_item"; break;
                    case 14: section = "disorient_item"; break;
                    case 15: section = "divine_aura"; break;
                }
                if (section != null) {
                    openConditionsList(player, section);
                }
            } else if (title.startsWith("Условия:")) {
                if (currentItem.getType() == Material.BARRIER) {
                    openMainMenu(player);
                } else if (currentItem.getType() == Material.EMERALD) {
                    openAddConditionMenu(player);
                } else {
                    if (currentItem.getItemMeta().hasLocalizedName()) {
                        String typeName = currentItem.getItemMeta().getLocalizedName();
                        ConditionType type = ConditionType.valueOf(typeName);
                        String section = editingSection.get(player);

                        if (event.getClick().isLeftClick()) {
                            startEditingValue(player, type);
                        } else if (event.getClick().isRightClick()) {
                            plugin.getConfig().set(section + ".conditions." + typeName, null);
                            plugin.saveConfig();
                            openConditionsList(player, section);
                            player.sendMessage(ColorUtil.format("&#90EE90Условие удалено!"));
                        }
                    }
                }
            } else if (title.equals("Добавить условие")) {
                if (currentItem.getType() == Material.BARRIER) {
                    String section = editingSection.get(player);
                    if (section != null) openConditionsList(player, section);
                } else {
                    if (currentItem.getItemMeta().hasLocalizedName()) {
                        ConditionType type = ConditionType.valueOf(currentItem.getItemMeta().getLocalizedName());
                        startEditingValue(player, type);
                    }
                }
            }
        }
    }

    private void startEditingValue(Player player, ConditionType type) {
        editingConditionType.put(player, type);
        player.closeInventory();

        String hint = "";
        switch (type) {
            case PERMISSION: hint = "permission.node"; break;
            case BLOCK_BELOW: hint = "DIAMOND_BLOCK"; break;
            case IS_SNEAKING: hint = "true / false"; break;
            case MIN_HEALTH: hint = "10.0"; break;
            case GAMEMODE: hint = "SURVIVAL"; break;
            case ITEM_IN_OFFHAND: hint = "SHIELD"; break;
            case IN_REGION: hint = "spawn"; break;
            case NOT_IN_REGION: hint = "spawn"; break;
            case IS_FLYING: hint = "true"; break;
            case NOT_FLYING: hint = "true"; break;
            case HAS_EFFECT: hint = "STRENGTH:1"; break;
            case NO_EFFECT: hint = "STRENGTH:1"; break;
            case IN_BIOME: hint = "PLAINS"; break;
            case NOT_IN_BIOME: hint = "PLAINS"; break;
            case IS_SWIMMING: hint = "true"; break;
            case NOT_SWIMMING: hint = "true"; break;
        }

        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#FB654EВведите значение для &#EAD7A2" + type.getDisplayName()));
        player.sendMessage(ColorUtil.format("&#777777Пример: " + hint));

        plugin.getChatInputHandler().waitForInput(player, (input) -> {
            String section = editingSection.get(player);
            if (section != null) {
                // Валидация
                switch (type) {
                    case BLOCK_BELOW:
                    case ITEM_IN_OFFHAND:
                        if (Material.matchMaterial(input) == null) {
                            player.sendMessage(ColorUtil.format("&#EB2D3AНеверный материал! Попробуйте снова."));
                            return;
                        }
                        break;
                    case IN_BIOME:
                    case NOT_IN_BIOME:
                        try {
                            Biome.valueOf(input.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(ColorUtil.format("&#EB2D3AНеверный биом! Попробуйте снова."));
                            return;
                        }
                        break;
                    case HAS_EFFECT:
                    case NO_EFFECT:
                        String[] parts = input.split(":");
                        if (parts.length != 2 || PotionEffectType.getByName(parts[0].toUpperCase()) == null) {
                            player.sendMessage(ColorUtil.format("&#EB2D3AНеверный формат эффекта! Пример: STRENGTH:1"));
                            return;
                        }
                        try {
                            Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            player.sendMessage(ColorUtil.format("&#EB2D3AНеверный уровень эффекта!"));
                            return;
                        }
                        break;
                    case IS_SNEAKING:
                    case IS_FLYING:
                    case NOT_FLYING:
                    case IS_SWIMMING:
                    case NOT_SWIMMING:
                        if (!input.equalsIgnoreCase("true") && !input.equalsIgnoreCase("false")) {
                            player.sendMessage(ColorUtil.format("&#EB2D3AВведите true или false!"));
                            return;
                        }
                        break;
                }

                plugin.getConfig().set(section + ".conditions." + type.name(), input);
                plugin.saveConfig();

                player.sendMessage(ColorUtil.format("&#90EE90Значение установлено: &#FFFFFF" + input));

                Bukkit.getScheduler().runTask(plugin, () -> openConditionsList(player, section));
            }
            editingConditionType.remove(player);
        });
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.format(name));
            List<String> loreList = new ArrayList<>();
            for (String s : lore) loreList.add(ColorUtil.format(s));
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBorders(Inventory inv) {
        ItemStack glass = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, glass);
            }
        }
    }
}