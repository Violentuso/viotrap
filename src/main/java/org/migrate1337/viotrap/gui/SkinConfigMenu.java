package org.migrate1337.viotrap.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.utils.ColorUtil;

public class SkinConfigMenu implements Listener {
    private final VioTrap plugin;
    private final Map<Player, String> currentSkinType = new HashMap<>();
    private final Map<Player, String> editingSkin = new HashMap<>();
    private final Map<Player, String> currentField = new HashMap<>();

    private static final int[] SKIN_LIST_SLOTS = {
        10, 11, 12, 13,
        19, 20, 21, 22,
        28, 29, 30, 31
    };

    public SkinConfigMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, "Настройка скинов");
        updateMainMenuItems(inv);
        player.openInventory(inv);
    }

    private void updateMainMenuItems(Inventory inv) {
        inv.clear();

        int[] glass = {0,1,2,3,4,5,6,7,8,9,17,18,26};
        for (int i : glass) {
            inv.setItem(i, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        }

        inv.setItem(11, createMenuItem(Material.TRIPWIRE_HOOK,
                ColorUtil.format("&#FF6B6BСкины трапок"),
                ColorUtil.format("&#AEC1F2 • Настроить скины трапок")));

        inv.setItem(15, createMenuItem(Material.STONE_BUTTON,
                ColorUtil.format("&#4ECDC4Скины пластов"),
                ColorUtil.format("&#AEC1F2 • Настроить скины пластов")));
    }

    private void openSkinsList(Player player, String type) {
        this.currentSkinType.put(player, type);
        Inventory inv = Bukkit.createInventory(null, 54, type.equals("trap") ? "Скины трапок" : "Скины пластов");
        updateSkinsListItems(inv, type);
        player.openInventory(inv);
    }

    private void updateSkinsListItems(Inventory inv, String type) {
        inv.clear();

        int[] glass = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int i : glass) {
            inv.setItem(i, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        }

        ConfigurationSection skinsSection = type.equals("trap") ?
            plugin.getConfig().getConfigurationSection("skins") :
            plugin.getConfig().getConfigurationSection("plate_skins");

        if (skinsSection == null) {
            inv.setItem(22, createMenuItem(Material.BARRIER,
                    ColorUtil.format("&#FF5555Нет скинов"),
                    ColorUtil.format("&#AEC1F2 • Создайте скин через меню создания")));
            return;
        }

        List<String> skinNames = new ArrayList<>(skinsSection.getKeys(false));
        if (skinNames.isEmpty()) {
            inv.setItem(22, createMenuItem(Material.BARRIER,
                    ColorUtil.format("&#FF5555Нет скинов"),
                    ColorUtil.format("&#AEC1F2 • Создайте скин через меню создания")));
            return;
        }

        for (int i = 0; i < skinNames.size() && i < SKIN_LIST_SLOTS.length; i++) {
            String skinName = skinNames.get(i);
            int cooldown = plugin.getConfig().getInt("skins." + skinName + ".cooldown", 0);
            int duration = plugin.getConfig().getInt("skins." + skinName + ".duration", 0);
            String schematic = plugin.getConfig().getString("skins." + skinName + ".schem", "");

            if (type.equals("plate")) {
                cooldown = plugin.getConfig().getInt("plate_skins." + skinName + ".cooldown", 0);
                duration = plugin.getConfig().getInt("plate_skins." + skinName + ".duration", 5);
                schematic = plugin.getConfig().getString("plate_skins." + skinName + ".forward_schematic", "");
            }

            inv.setItem(SKIN_LIST_SLOTS[i], createMenuItem(Material.NAME_TAG,
                    ColorUtil.format("&#FFFFFF" + skinName),
                    ColorUtil.format("&#AEC1F2 • Кулдаун: ") + cooldown + " сек",
                    ColorUtil.format("&#AEC1F2 • Длительность: ") + duration + " сек",
                    ColorUtil.format("&#AEC1F2 • Схематика: ") + (schematic.isEmpty() ? "Нет" : schematic),
                    "",
                    ColorUtil.format("&#55FF55ЛКМ → редактировать"),
                    ColorUtil.format("&#FF5555ПКМ → удалить")));
        }

        inv.setItem(49, createMenuItem(Material.BARRIER,
                ColorUtil.format("&#FF5555Назад"),
                ColorUtil.format("&#AEC1F2 • Вернуться в главное меню")));
    }

    private void openSkinEditor(Player player, String skinName, String type) {
        this.editingSkin.put(player, skinName);
        this.currentSkinType.put(player, type);

        Inventory inv = Bukkit.createInventory(null, 36, "Редактирование: " + skinName);
        updateSkinEditorItems(inv, skinName, type);
        player.openInventory(inv);
    }

    private void updateSkinEditorItems(Inventory inv, String skinName, String type) {
        inv.clear();

        int[] glass = {0,1,2,3,4,5,6,7,8,9,11,12,13,14,15,16,17,18,20,21,22,23,24,25,26,27,29,30,31,32,33,34,35};
        for (int i : glass) {
            inv.setItem(i, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        }

        String prefix = type.equals("trap") ? "skins." + skinName : "plate_skins." + skinName;

        inv.setItem(10, createMenuItem(Material.CHEST,
                ColorUtil.format("&#EAD7A2Схематика"),
                ColorUtil.format("&#AEC1F2 • Текущая: ") + (type.equals("trap") ?
                    plugin.getConfig().getString(prefix + ".schem", "Не задано") :
                    plugin.getConfig().getString(prefix + ".forward_schematic", "Не задано"))));

        inv.setItem(11, createMenuItem(Material.NOTE_BLOCK,
                ColorUtil.format("&#9ABDADЗвук активации"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getConfig().getString(prefix + ".sound.type", "Не задано")));

        inv.setItem(12, createMenuItem(Material.NOTE_BLOCK,
                ColorUtil.format("&#E6E6C2Звук завершения"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getConfig().getString(prefix + ".sound.type-ended", "Не задано")));

        inv.setItem(13, createMenuItem(Material.CLOCK,
                ColorUtil.format("&#FFEB3BКулдаун"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getConfig().getInt(prefix + ".cooldown", 0) + " сек"));

        inv.setItem(19, createMenuItem(Material.GLASS,
                ColorUtil.format("&#FF9800Длительность"),
                ColorUtil.format("&#AEC1F2 • Текущая: ") + plugin.getConfig().getInt(prefix + ".duration", 0) + " сек"));

        if (type.equals("trap")) {
            inv.setItem(20, createMenuItem(Material.FIREWORK_STAR,
                    ColorUtil.format("&#FF69B4Анимация"),
                    ColorUtil.format("&#AEC1F2 • Текущая: ") + plugin.getConfig().getString(prefix + ".animation", "Нет")));
        }

        inv.setItem(21, createMenuItem(Material.BEACON,
                ColorUtil.format("&#FFD700Поинты"),
                ColorUtil.format("&#AEC1F2 • Требуется: ") + plugin.getConfig().getInt(prefix + ".points", 0)));

        if (type.equals("trap")) {
            inv.setItem(22, createMenuItem(Material.COMMAND_BLOCK,
                    ColorUtil.format("&#ADD8E6Действия"),
                    ColorUtil.format("&#AEC1F2 • Настроить действия")));

            inv.setItem(23, createMenuItem(Material.BEACON,
                    ColorUtil.format("&#FFD700Флаги"),
                    ColorUtil.format("&#AEC1F2 • Настроить флаги")));
        } else {
            inv.setItem(22, createMenuItem(Material.COMMAND_BLOCK,
                    ColorUtil.format("&#ADD8E6Действия"),
                    ColorUtil.format("&#AEC1F2 • Настроить действия")));
        }

        inv.setItem(28, createMenuItem(Material.REDSTONE,
                ColorUtil.format("&#FF5555Удалить скин"),
                ColorUtil.format("&#AEC1F2 • Удалить этот скин полностью")));

        inv.setItem(31, createMenuItem(Material.EMERALD,
                ColorUtil.format("&#90EE90Сохранить"),
                ColorUtil.format("&#AEC1F2 • Сохранить изменения")));

        inv.setItem(35, createMenuItem(Material.BARRIER,
                ColorUtil.format("&#FF5555Назад"),
                ColorUtil.format("&#AEC1F2 • Вернуться к списку скинов")));
    }

    private void openActionsMenu(Player player, String skinName, String type) {
        Inventory inv = Bukkit.createInventory(null, 36, "Действия: " + skinName);
        updateActionsMenuItems(inv, skinName, type);
        player.openInventory(inv);
    }

    private void updateActionsMenuItems(Inventory inv, String skinName, String type) {
        inv.clear();

        int[] glass = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35};
        for (int i : glass) {
            inv.setItem(i, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        }

        String prefix = type.equals("trap") ? "skins." + skinName + ".actions" : "plate_skins." + skinName + ".actions";
        ConfigurationSection actionsSection = plugin.getConfig().getConfigurationSection(prefix);

        inv.setItem(10, createMenuItem(Material.PAPER,
                ColorUtil.format("&#FFFFFFДобавить действие"),
                ColorUtil.format("&#AEC1F2 • Формат: <тип> <таргет> [радиус] [параметры]")));

        if (actionsSection != null) {
            List<String> actionKeys = new ArrayList<>(actionsSection.getKeys(false));
            int slot = 11;
            for (String actionKey : actionKeys) {
                if (slot >= 34) break;

                String actionType = plugin.getConfig().getString(prefix + "." + actionKey + ".type", "unknown");
                String target = plugin.getConfig().getString(prefix + "." + actionKey + ".target", "?");
                double radius = plugin.getConfig().getDouble(prefix + "." + actionKey + ".radius", 5.0);

                String description = actionType;
                switch (actionType.toLowerCase()) {
                    case "effect":
                        String effect = plugin.getConfig().getString(prefix + "." + actionKey + ".effect", "?");
                        description = "Эффект: " + effect;
                        break;
                    case "command":
                        String cmd = plugin.getConfig().getString(prefix + "." + actionKey + ".command", "?");
                        description = "Команда: " + cmd;
                        break;
                    case "launch":
                        double up = plugin.getConfig().getDouble(prefix + "." + actionKey + ".upward-force", 0);
                        double horiz = plugin.getConfig().getDouble(prefix + "." + actionKey + ".horizontal-force", 0);
                        description = "Подкидывание (вверх: " + up + ", в сторону: " + horiz + ")";
                        break;
                    case "teleportout":
                        description = "Телепортация вверх";
                        break;
                    case "particlehitbox":
                        description = "Частицы хитбокса";
                        break;
                    case "cooldownitem":
                        description = "Кулдаун предметов";
                        break;
                    case "denyitemuse":
                        description = "Запрет предметов";
                        break;
                    case "scrambleinventory":
                        description = "Перемешка инвентаря";
                        break;
                    case "blockspread":
                        description = "Распространение блоков";
                        break;
                }

                inv.setItem(slot++, createMenuItem(Material.PAPER,
                        ColorUtil.format("&#FFFFFF" + actionKey),
                        ColorUtil.format("&#AEC1F2 • ") + description,
                        ColorUtil.format("&#AEC1F2 • Таргет: ") + target + ", радиус: " + radius,
                        "",
                        ColorUtil.format("&#FF5555ПКМ → удалить")));
            }
        }

        inv.setItem(35, createMenuItem(Material.BARRIER,
                ColorUtil.format("&#FF5555Назад"),
                ColorUtil.format("&#AEC1F2 • Вернуться к редактированию")));
    }

    private void openFlagsMenu(Player player, String skinName, String type) {
        Inventory inv = Bukkit.createInventory(null, 27, "Флаги: " + skinName);
        updateFlagsMenuItems(inv, skinName, type);
        player.openInventory(inv);
    }

    private void updateFlagsMenuItems(Inventory inv, String skinName, String type) {
        inv.clear();

        int[] glass = {0,1,2,3,4,5,6,7,8,9,17,18,26};
        for (int i : glass) {
            inv.setItem(i, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        }

        String prefix = type.equals("trap") ? "skins." + skinName + ".flags" : "plate_skins." + skinName + ".flags";
        ConfigurationSection flagsSection = plugin.getConfig().getConfigurationSection(prefix);

        inv.setItem(10, createMenuItem(Material.BEACON,
                ColorUtil.format("&#FFD700Добавить флаг"),
                ColorUtil.format("&#AEC1F2 • Формат: <flag_name> <ALLOW/DENY>")));

        if (flagsSection != null) {
            List<String> flagNames = new ArrayList<>(flagsSection.getKeys(false));
            int slot = 11;
            for (String flagName : flagNames) {
                if (slot >= 17) break;

                String flagValue = plugin.getConfig().getString(prefix + "." + flagName, "?");

                inv.setItem(slot++, createMenuItem(Material.BEACON,
                        ColorUtil.format("&#FFD700" + flagName),
                        ColorUtil.format("&#AEC1F2 • Значение: ") + flagValue,
                        "",
                        ColorUtil.format("&#FF5555ПКМ → удалить")));
            }
        }

        inv.setItem(26, createMenuItem(Material.BARRIER,
                ColorUtil.format("&#FF5555Назад"),
                ColorUtil.format("&#AEC1F2 • Вернуться к редактированию")));
    }

    private void handleInput(Player player, String field, String message) {
        player.closeInventory();
        this.currentField.put(player, field);
        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#FB654E" + message));
        this.plugin.getChatInputHandler().waitForInput(player, (input) -> {
            String skinName = this.editingSkin.get(player);
            String type = this.currentSkinType.get(player);
            if (skinName == null || type == null) {
                player.sendMessage("§cОшибка: скин не выбран");
                return;
            }
            String prefix = type.equals("trap") ? "skins." + skinName : "plate_skins." + skinName;

            boolean saved = false;

            switch (field) {
                case "schematic":
                    if (type.equals("trap")) {
                        plugin.getConfig().set(prefix + ".schem", input);
                    } else {
                        plugin.getConfig().set(prefix + ".forward_schematic", input);
                        plugin.getConfig().set(prefix + ".forward_left_schematic", input);
                        plugin.getConfig().set(prefix + ".forward_right_schematic", input);
                        plugin.getConfig().set(prefix + ".backward_schematic", input);
                        plugin.getConfig().set(prefix + ".backward_left_schematic", input);
                        plugin.getConfig().set(prefix + ".backward_right_schematic", input);
                        plugin.getConfig().set(prefix + ".left_schematic", input);
                        plugin.getConfig().set(prefix + ".right_schematic", input);
                        plugin.getConfig().set(prefix + ".up_schematic", input);
                        plugin.getConfig().set(prefix + ".down_schematic", input);
                    }
                    saved = true;
                    break;

                case "sound.type":
                case "sound.type-ended":
                    plugin.getConfig().set(prefix + "." + field, input);
                    saved = true;
                    break;

                case "cooldown":
                case "duration":
                    try {
                        int value = Integer.parseInt(input);
                        plugin.getConfig().set(prefix + "." + field, value);
                        saved = true;
                    } catch (NumberFormatException e) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AВведите целое число!"));
                    }
                    break;

                case "points":
                    try {
                        int value = Integer.parseInt(input);
                        plugin.getConfig().set(prefix + ".points", value);
                        saved = true;
                    } catch (NumberFormatException e) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AВведите целое число!"));
                    }
                    break;

                case "animation":
                    if (type.equals("trap")) {
                        if (input.isEmpty() || input.equalsIgnoreCase("нет") || input.equalsIgnoreCase("none")) {
                            plugin.getConfig().set(prefix + ".animation", null);
                        } else {
                            plugin.getConfig().set(prefix + ".animation", input);
                        }
                        saved = true;
                    }
                    break;

                case "add_action":
                    String[] parts = input.split(" ", 2);
                    if (parts.length < 2) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AФормат: <тип> <таргет> [радиус] [параметры]"));
                        Bukkit.getScheduler().runTask(plugin, () -> openActionsMenu(player, skinName, type));
                        return;
                    }
                    String actionType = parts[0];
                    String actionData = parts[1];
                    String actionsPath = prefix + ".actions";
                    ConfigurationSection actionsSec = plugin.getConfig().getConfigurationSection(actionsPath);
                    int actionNum = actionsSec != null ? actionsSec.getKeys(false).size() + 1 : 1;
                    plugin.getConfig().set(actionsPath + ".action" + actionNum + ".type", actionType);
                    plugin.getConfig().set(actionsPath + ".action" + actionNum + ".target", actionData.split(" ")[0]);
                    plugin.getConfig().set(actionsPath + ".action" + actionNum + ".radius", 5.0);
                    saved = true;
                    break;

                case "add_flag":
                    String[] flagParts = input.split(" ", 2);
                    if (flagParts.length != 2) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AФормат: <flag_name> <ALLOW/DENY>"));
                        Bukkit.getScheduler().runTask(plugin, () -> openFlagsMenu(player, skinName, type));
                        return;
                    }
                    plugin.getConfig().set(prefix + ".flags." + flagParts[0], flagParts[1].toUpperCase());
                    saved = true;
                    break;
            }

            if (saved) {
                plugin.saveConfig();
                plugin.reloadConfig();
                plugin.getSkinNames().clear();
                ConfigurationSection skinsSection = plugin.getConfig().getConfigurationSection("skins");
                if (skinsSection != null) {
                    plugin.getSkinNames().addAll(skinsSection.getKeys(false));
                }
                player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Значение сохранено!"));
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (skinName != null) {
                    if ("add_action".equals(field)) {
                        openActionsMenu(player, skinName, type);
                    } else if ("add_flag".equals(field)) {
                        openFlagsMenu(player, skinName, type);
                    } else {
                        openSkinEditor(player, skinName, type);
                    }
                }
                currentField.remove(player);
            });
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity entity = event.getWhoClicked();
        if (!(entity instanceof Player)) return;

        Player player = (Player) entity;
        String title = event.getView().getTitle();

        if (title.equals("Настройка скинов") ||
            title.equals("Скины трапок") ||
            title.equals("Скины пластов") ||
            title.startsWith("Редактирование:") ||
            title.startsWith("Действия:") ||
            title.startsWith("Флаги:")) {

            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            String type = this.currentSkinType.getOrDefault(player, "trap");

            if (title.equals("Настройка скинов")) {
                if (event.getSlot() == 11) {
                    openSkinsList(player, "trap");
                } else if (event.getSlot() == 15) {
                    openSkinsList(player, "plate");
                }
            } else if (title.equals("Скины трапок") || title.equals("Скины пластов")) {
                type = title.equals("Скины трапок") ? "trap" : "plate";
                if (event.getSlot() == 49) {
                    openMainMenu(player);
                } else if (event.getClick() == ClickType.LEFT) {
                    int index = -1;
                    for (int i = 0; i < SKIN_LIST_SLOTS.length; i++) {
                        if (SKIN_LIST_SLOTS[i] == event.getSlot()) {
                            index = i;
                            break;
                        }
                    }
                    if (index >= 0) {
                        ConfigurationSection skinsSection = type.equals("trap") ?
                            plugin.getConfig().getConfigurationSection("skins") :
                            plugin.getConfig().getConfigurationSection("plate_skins");
                        if (skinsSection != null) {
                            List<String> skinNames = new ArrayList<>(skinsSection.getKeys(false));
                            if (index < skinNames.size()) {
                                openSkinEditor(player, skinNames.get(index), type);
                            }
                        }
                    }
                } else if (event.getClick() == ClickType.RIGHT) {
                    int index = -1;
                    for (int i = 0; i < SKIN_LIST_SLOTS.length; i++) {
                        if (SKIN_LIST_SLOTS[i] == event.getSlot()) {
                            index = i;
                            break;
                        }
                    }
                    if (index >= 0) {
                        ConfigurationSection skinsSection = type.equals("trap") ?
                            plugin.getConfig().getConfigurationSection("skins") :
                            plugin.getConfig().getConfigurationSection("plate_skins");
                        if (skinsSection != null) {
                            List<String> skinNames = new ArrayList<>(skinsSection.getKeys(false));
                            if (index < skinNames.size()) {
                                String skinName = skinNames.get(index);
                                player.closeInventory();
                                player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AВы уверены, что хотите удалить скин '" + skinName + "'?"));
                                player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#AEC1F2Напишите 'да' для подтверждения или 'нет' для отмены."));

                                final String finalSkinName = skinName;
                                final String finalType = type;
                                plugin.getChatInputHandler().waitForInput(player, (input) -> {
                                    if (input.equalsIgnoreCase("да") || input.equalsIgnoreCase("yes")) {
                                        String path = finalType.equals("trap") ? "skins." + finalSkinName : "plate_skins." + finalSkinName;
                                        plugin.getConfig().set(path, null);
                                        plugin.saveConfig();
                                        plugin.reloadConfig();

                                        plugin.getSkinNames().clear();
                                        ConfigurationSection skinsSec = plugin.getConfig().getConfigurationSection("skins");
                                        if (skinsSec != null) {
                                            plugin.getSkinNames().addAll(skinsSec.getKeys(false));
                                        }

                                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Скин '" + finalSkinName + "' удалён!"));
                                    } else {
                                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#AEC1F2Удаление отменено."));
                                    }
                                    Bukkit.getScheduler().runTask(plugin, () -> openSkinsList(player, finalType));
                                });
                            }
                        }
                    }
                }
            } else if (title.startsWith("Действия:")) {
                String skinName = title.replace("Действия: ", "");
                if (event.getSlot() == 35) {
                    openSkinEditor(player, skinName, type);
                } else if (event.getSlot() == 10) {
                    handleInput(player, "add_action", "Введите действие (формат: <тип> <таргет> [радиус] [параметры])\nТипы: effect, command, teleportout, particlehitbox, cooldownitem, denyitemuse, launch, scrambleinventory, blockspread");
                } else if (event.getClick() == ClickType.RIGHT && event.getSlot() >= 11 && event.getSlot() <= 33) {
                    String prefix = type.equals("trap") ? "skins." + skinName + ".actions" : "plate_skins." + skinName + ".actions";
                    ConfigurationSection actionsSection = plugin.getConfig().getConfigurationSection(prefix);
                    if (actionsSection != null) {
                        List<String> actionKeys = new ArrayList<>(actionsSection.getKeys(false));
                        int index = event.getSlot() - 11;
                        if (index >= 0 && index < actionKeys.size()) {
                            plugin.getConfig().set(prefix + "." + actionKeys.get(index), null);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Действие удалено!"));
                            openActionsMenu(player, skinName, type);
                        }
                    }
                }
            } else if (title.startsWith("Флаги:")) {
                String skinName = title.replace("Флаги: ", "");
                if (event.getSlot() == 26) {
                    openSkinEditor(player, skinName, type);
                } else if (event.getSlot() == 10) {
                    handleInput(player, "add_flag", "Введите флаг (формат: <flag_name> <ALLOW/DENY>)\nНапример: build ALLOW");
                } else if (event.getClick() == ClickType.RIGHT && event.getSlot() >= 11 && event.getSlot() <= 16) {
                    String prefix = type.equals("trap") ? "skins." + skinName + ".flags" : "plate_skins." + skinName + ".flags";
                    ConfigurationSection flagsSection = plugin.getConfig().getConfigurationSection(prefix);
                    if (flagsSection != null) {
                        List<String> flagKeys = new ArrayList<>(flagsSection.getKeys(false));
                        int index = event.getSlot() - 11;
                        if (index >= 0 && index < flagKeys.size()) {
                            plugin.getConfig().set(prefix + "." + flagKeys.get(index), null);
                            plugin.saveConfig();
                            plugin.reloadConfig();
                            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Флаг удалён!"));
                            openFlagsMenu(player, skinName, type);
                        }
                    }
                }
            } else if (title.startsWith("Редактирование:")) {
                String skinName = title.replace("Редактирование: ", "");

                if (event.getSlot() == 35) {
                    openSkinsList(player, type);
                } else if (event.getSlot() == 10) {
                    handleInput(player, "schematic", "Введите название схематики:");
                } else if (event.getSlot() == 11) {
                    handleInput(player, "sound.type", "Введите тип звука активации (например, BLOCK_ANVIL_PLACE):");
                } else if (event.getSlot() == 12) {
                    handleInput(player, "sound.type-ended", "Введите тип звука завершения (например, BLOCK_PISTON_EXTEND):");
                } else if (event.getSlot() == 13) {
                    handleInput(player, "cooldown", "Введите кулдаун в секундах (целое число):");
                } else if (event.getSlot() == 19) {
                    handleInput(player, "duration", "Введите длительность в секундах (целое число):");
                } else if (event.getSlot() == 20 && type.equals("trap")) {
                    handleInput(player, "animation", "Введите название анимации (или 'нет' для отключения):");
                } else if (event.getSlot() == 21) {
                    handleInput(player, "points", "Введите количество требуемых поинтов:");
                } else if (event.getSlot() == 22) {
                    openActionsMenu(player, skinName, type);
                } else if (event.getSlot() == 23 && type.equals("trap")) {
                    openFlagsMenu(player, skinName, type);
                } else if (event.getSlot() == 28) {
                    player.closeInventory();
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AВы уверены, что хотите удалить скин '" + skinName + "'?"));
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#AEC1F2Напишите 'да' для подтверждения или 'нет' для отмены."));

                    final String finalSkinName = skinName;
                    final String finalType = type;
                    plugin.getChatInputHandler().waitForInput(player, (input) -> {
                        if (input.equalsIgnoreCase("да") || input.equalsIgnoreCase("yes")) {
                            String path = finalType.equals("trap") ? "skins." + finalSkinName : "plate_skins." + finalSkinName;
                            plugin.getConfig().set(path, null);
                            plugin.saveConfig();
                            plugin.reloadConfig();

                            plugin.getSkinNames().clear();
                            ConfigurationSection skinsSec = plugin.getConfig().getConfigurationSection("skins");
                            if (skinsSec != null) {
                                plugin.getSkinNames().addAll(skinsSec.getKeys(false));
                            }

                            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Скин '" + finalSkinName + "' удалён!"));
                            openMainMenu(player);
                        } else {
                            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#AEC1F2Удаление отменено."));
                            openSkinEditor(player, finalSkinName, finalType);
                        }
                    });
                } else if (event.getSlot() == 31) {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Изменения сохранены!"));
                    openSkinEditor(player, skinName, type);
                }
            }
        }
    }

    private ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}