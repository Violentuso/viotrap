package org.migrate1337.viotrap.gui;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

public class SkinCreationMenu implements Listener {
    private final VioTrap plugin;
    private final Map<Player, String> currentSubMenu = new HashMap();
    private final Map<Player, String> editingActionKey = new HashMap();
    private final Map<Player, String> editingFlagKey = new HashMap();

    public SkinCreationMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)null, 54, "Создание скина для трапки");
        this.currentSubMenu.remove(player);
        this.updateMainMenuItems(inventory);
        player.openInventory(inventory);
    }

    private void updateMainMenu(Player player) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (inventory != null && player.getOpenInventory().getTitle().equals("Создание скина для трапки")) {
            this.updateMainMenuItems(inventory);
        } else {
            this.openMenu(player);
        }
    }

    private void updateMainMenuItems(Inventory inv) {
        inv.clear();

        int[] glassSlots = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int slot : glassSlots) {
            inv.setItem(slot, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        }

        inv.setItem(19, createMenuItem(Material.PAPER,
                ColorUtil.format("&#CCCAF0Название скина"),
                ColorUtil.format("&#AEC1F2 • Текущее: ") + plugin.getTempSkinData().getOrDefault("name", "Не задано")));

        inv.setItem(20, createMenuItem(Material.CHEST,
                ColorUtil.format("&#EAD7A2Схематика"),
                ColorUtil.format("&#AEC1F2 • Текущая: ") + plugin.getTempSkinData().getOrDefault("schem", "Не задано")));

        inv.setItem(21, createMenuItem(Material.NOTE_BLOCK,
                ColorUtil.format("&#9ABDADТип звука"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getTempSkinData().getOrDefault("sound.type", "Не задано")));

        inv.setItem(22, createMenuItem(Material.NOTE_BLOCK,
                ColorUtil.format("&#E6E6C2Звук завершения"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getTempSkinData().getOrDefault("sound.type-ended", "Не задано")));

        inv.setItem(23, createMenuItem(Material.BEACON,
                ColorUtil.format("&#FFD700Флаги"),
                ColorUtil.format("&#AEC1F2 • Настроить флаги (" + getFlagsCount() + ")")));

        inv.setItem(24, createMenuItem(Material.CLOCK,
                ColorUtil.format("&#FFEB3BКулдаун"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getTempSkinData().getOrDefault("cooldown", "Не задано")));

        inv.setItem(25, createMenuItem(Material.GLASS,
                ColorUtil.format("&#FF9800Время действия"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getTempSkinData().getOrDefault("duration", "Не задано")));

        inv.setItem(32, createMenuItem(Material.FIREWORK_STAR,
                ColorUtil.format("&#FF69B4Анимация / Партиклы"),
                ColorUtil.format("&#AEC1F2 • Текущая: ") + plugin.getTempSkinData().getOrDefault("animation", "Не задано")));

        inv.setItem(31, createMenuItem(Material.COMMAND_BLOCK,
                ColorUtil.format("&#ADD8E6Действия"),
                ColorUtil.format("&#AEC1F2 • Настроить действия (" + getActionsCount() + ")")));

        inv.setItem(40, createMenuItem(Material.GREEN_WOOL,
                ColorUtil.format("&#90EE90Сохранить"),
                ColorUtil.format("&#AEC1F2 • Сохранить новый скин")));
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

    private List<String> getDescriptionLines() {
        String descData = (String)this.plugin.getTempSkinData().getOrDefault("desc_for_trap", "");
        return descData.isEmpty() ? new ArrayList() : new ArrayList(Arrays.asList(descData.split("\\|")));
    }

    private int getActionsCount() {
        String actionsStr = (String) plugin.getTempSkinData().getOrDefault("actions", "");
        return actionsStr.isEmpty() ? 0 : actionsStr.split("\\|").length;
    }

    private int getFlagsCount() {
        String flagsStr = (String) plugin.getTempSkinData().getOrDefault("flags", "");
        return flagsStr.isEmpty() ? 0 : flagsStr.split("\\|").length;
    }

    private void openActionsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Действия скина");
        currentSubMenu.put(player, "actions");
        updateActionsMenuItems(inv, player);
        player.openInventory(inv);
    }

    private void openDescriptionMenu(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)null, 27, "Редактирование описания");
        this.currentSubMenu.put(player, "description");
        this.updateDescriptionMenuItems(inventory);
        player.openInventory(inventory);
    }

    private void updateDescriptionMenuItems(Inventory inventory) {
        inventory.clear();
        List<String> descLines = this.getDescriptionLines();

        for(int i = 0; i < descLines.size() && i < 20; ++i) {
            String line = (String)descLines.get(i);
            Material var10003 = Material.PAPER;
            String var10004 = ColorUtil.format("&#FFFFFFСтрока " + (i + 1));
            String[] var10005 = new String[4];
            String var10008 = ColorUtil.format("&#AEC1F2 • ");
            var10005[0] = var10008 + line;
            var10005[1] = "";
            var10005[2] = ColorUtil.format("&#55FF55ЛКМ для изменения");
            var10005[3] = ColorUtil.format("&#FF5555ПКМ для удаления");
            inventory.setItem(i, this.createMenuItem(var10003, var10004, var10005));
        }

        inventory.setItem(25, this.createMenuItem(Material.EMERALD, ColorUtil.format("&#90EE90Добавить строку"), ColorUtil.format("&#AEC1F2 • Добавить новую строку описания")));
        inventory.setItem(26, this.createMenuItem(Material.BARRIER, ColorUtil.format("&#FF5555Назад"), ColorUtil.format("&#AEC1F2 • Вернуться в главное меню")));
    }

    private void updateActionsMenuItems(Inventory inv, Player player) {
        inv.clear();
        Map<String, String> actions = parseActions((String) plugin.getTempSkinData().getOrDefault("actions", ""));
        int slot = 0;

        for (Map.Entry<String, String> entry : actions.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String[] parts = value.split(";", 2);
            String type = parts[0].toLowerCase();

            String displayArgs = "?";
            if (parts.length > 1) {
                String[] args = parts[1].split(";");
                String targetDisplay = args[0];
                String targetRadius = args.length > 1 ? args[1] : "5.0";

                StringBuilder sb = new StringBuilder(targetDisplay);

                 
                if (targetDisplay.equalsIgnoreCase("not-in")) {
                    sb.append(" (r=").append(targetRadius).append(")");
                }

                 
                if (type.equals("blockspread")) {
                    sb.append(" [Радиус блоков: ").append(args[2]).append("]");
                    for (int i = 3; i < args.length; i++) {
                        sb.append(" ").append(args[i]);
                    }
                } else {
                    for (int i = 2; i < args.length; i++) {
                        sb.append(" ").append(args[i]);
                    }
                }
                displayArgs = sb.toString();
            }

            String display;
            switch (type) {
                case "effect": display = "Эффект: " + displayArgs; break;
                case "command": display = "Команда: " + displayArgs; break;
                case "teleportout": display = "Телепортация: " + displayArgs; break;
                case "particlehitbox": display = "Частицы: " + displayArgs; break;
                case "cooldownitem": display = "Кулдаун предм: " + displayArgs; break;
                case "denyitemuse": display = "Запрет предм: " + displayArgs; break;
                case "launch": display = "Подкидывание: " + displayArgs; break;
                case "scrambleinventory": display = "Перемешка: " + displayArgs; break;
                case "blockspread": display = "Блоки: " + displayArgs; break;
                default: display = "Неизвестное действие"; break;
            }

            inv.setItem(slot++, createMenuItem(Material.PAPER,
                    ColorUtil.format("&#FFFFFF" + key),
                    ColorUtil.format("&#AEC1F2 • ") + display,
                    "",
                    ColorUtil.format("&#55FF55ЛКМ → изменить"),
                    ColorUtil.format("&#FF5555ПКМ → удалить")));
        }

        inv.setItem(25, createMenuItem(Material.EMERALD,
                ColorUtil.format("&#90EE90Добавить действие"),
                ColorUtil.format("&#AEC1F2 • Добавить новое действие")));

        inv.setItem(26, createMenuItem(Material.BARRIER,
                ColorUtil.format("&#FF5555Назад"),
                ColorUtil.format("&#AEC1F2 • Вернуться в главное меню")));
    }

    private void openActionTypeMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 18, "Выбор типа действия");
        currentSubMenu.put(player, "action_type");

        inv.setItem(0, createMenuItem(Material.POTION,
                ColorUtil.format("&#EED7A1Эффект"),
                ColorUtil.format("&#AEC1F2 • Добавить эффект (SPEED, POISON и т.д.)")));

        inv.setItem(1, createMenuItem(Material.COMMAND_BLOCK,
                ColorUtil.format("&#CD8B62Команда"),
                ColorUtil.format("&#AEC1F2 • Добавить команду (gamemode, effect и т.д.)")));

        inv.setItem(2, createMenuItem(Material.ENDER_PEARL,
                ColorUtil.format("&#475C6CТелепортация вверх"),
                ColorUtil.format("&#AEC1F2 • Выкинуть игрока вверх")));

        inv.setItem(3, createMenuItem(Material.FIREWORK_ROCKET,
                ColorUtil.format("&#50B8E7Частицы хитбокса"),
                ColorUtil.format("&#AEC1F2 • Показать частицы вокруг игрока")));

        inv.setItem(4, createMenuItem(Material.CLOCK,
                ColorUtil.format("&#FFEB3BКулдаун предметов"),
                ColorUtil.format("&#AEC1F2 • Наложить кулдаун на предметы")));

        inv.setItem(5, createMenuItem(Material.BARRIER,
                ColorUtil.format("&#FF5555Запрет использования"),
                ColorUtil.format("&#AEC1F2 • Запретить использовать предметы")));

        inv.setItem(6, createMenuItem(Material.SLIME_BLOCK,
                ColorUtil.format("&#55FF55Подкидывание"),
                ColorUtil.format("&#AEC1F2 • Подкинуть игроков (игнорируя ловушку)")));

        inv.setItem(7, createMenuItem(Material.HOPPER,
                ColorUtil.format("&#FF55FFПеремешка инвентаря"),
                ColorUtil.format("&#AEC1F2 • Перемешать хотбар игроку")));

        inv.setItem(8, createMenuItem(Material.COBWEB,
                ColorUtil.format("&#EAD7A2Спавн блоков"),
                ColorUtil.format("&#AEC1F2 • Спавнит блоки в радиусе")));

        inv.setItem(17, createMenuItem(Material.BARRIER,
                ColorUtil.format("&#EB2D3AНазад"),
                ColorUtil.format("&#AEC1F2 • Вернуться к действиям")));

        player.openInventory(inv);
    }

    private void openFlagsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)null, 27, "Флаги скина");
        this.currentSubMenu.put(player, "flags");
        this.updateFlagsMenuItems(inventory, player);
        player.openInventory(inventory);
    }

    private void updateFlagsMenuItems(Inventory inventory, Player player) {
        inventory.clear();
        Map<String, String> flags = this.parseFlags((String)this.plugin.getTempSkinData().getOrDefault("flags", ""));
        int slot = 0;

        for(Map.Entry<String, String> flagEntry : flags.entrySet()) {
            String flagKey = (String)flagEntry.getKey();
            String flagValue = (String)flagEntry.getValue();
            int var10001 = slot++;
            Material var10003 = Material.BEACON;
            String var10004 = ColorUtil.format("&#FFFFFFФлаг: " + flagKey);
            String[] var10005 = new String[3];
            String var10008 = ColorUtil.format("&#AEC1F2 • ");
            var10005[0] = var10008 + flagValue;
            var10005[1] = ColorUtil.format("&#55FF55ЛКМ для изменения");
            var10005[2] = ColorUtil.format("&#FF5555ПКМ для удаления");
            inventory.setItem(var10001, this.createMenuItem(var10003, var10004, var10005));
        }

        inventory.setItem(25, this.createMenuItem(Material.EMERALD, ColorUtil.format("&#90EE90Добавить флаг"), ColorUtil.format("&#AEC1F2 • Добавить новый флаг")));
        inventory.setItem(26, this.createMenuItem(Material.BARRIER, ColorUtil.format("&#EB2D3AНазад"), ColorUtil.format("&#AEC1F2 • Вернуться в главное меню")));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity var3 = event.getWhoClicked();
        if (var3 instanceof Player) {
            Player player = (Player) var3;
            if (event.getView().getTitle().equals("Создание скина для трапки") || event.getView().getTitle().equals("Редактирование описания") || event.getView().getTitle().equals("Действия скина") || event.getView().getTitle().equals("Выбор типа действия") || event.getView().getTitle().equals("Флаги скина")) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    if (event.getView().getTitle().equals("Создание скина для трапки")) {
                        switch (event.getSlot()) {
                            case 19: this.handleInput(player, "name", "Введите название скина:"); break;
                            case 20: this.handleInput(player, "schem", "Введите название схематики:"); break;
                            case 21: this.handleInput(player, "sound.type", "Введите тип звука (например, BLOCK_ANVIL_PLACE):"); break;
                            case 22: this.handleInput(player, "sound.type-ended", "Введите тип звука завершения (например, BLOCK_PISTON_EXTEND):"); break;
                            case 31: this.openActionsMenu(player); break;
                            case 23: this.openFlagsMenu(player); break;
                            case 24: this.handleInput(player, "cooldown", "Введите кулдаун в секундах (целое число):"); break;
                            case 25: this.handleInput(player, "duration", "Введите время действия в секундах (целое число):"); break;
                            case 32: this.handleInput(player, "animation", "Введите название анимации или паттерна:"); break;
                            case 40: this.saveSkin(player); break;
                        }
                    } else if (event.getView().getTitle().equals("Действия скина")) {
                        if (event.getSlot() == 26) {
                            this.currentSubMenu.remove(player);
                            this.updateMainMenu(player);
                        } else if (event.getSlot() == 25) {
                            this.openActionTypeMenu(player);
                        } else if (event.getSlot() < 25) {
                            Map<String, String> actions = this.parseActions((String)this.plugin.getTempSkinData().getOrDefault("actions", ""));
                            String actionKey = "action" + (event.getSlot() + 1);
                            if (actions.containsKey(actionKey)) {
                                if (event.getClick() == ClickType.LEFT) {
                                    this.editingActionKey.put(player, actionKey);
                                    this.handleActionEdit(player, actionKey);
                                } else if (event.getClick() == ClickType.RIGHT) {
                                    actions.remove(actionKey);
                                    this.plugin.getTempSkinData().put("actions", this.serializeActions(actions));
                                    this.updateActionsMenuItems(event.getInventory(), player);
                                }
                            }
                        }
                    } else if (event.getView().getTitle().equals("Выбор типа действия")) {
                        if (event.getSlot() == 17) {
                            this.openActionsMenu(player);
                        } else {
                            String type = null;
                            switch (event.getSlot()) {
                                case 0: type = "effect"; break;
                                case 1: type = "command"; break;
                                case 2: type = "teleportout"; break;
                                case 3: type = "particlehitbox"; break;
                                case 4: type = "cooldownitem"; break;
                                case 5: type = "denyitemuse"; break;
                                case 6: type = "launch"; break;
                                case 7: type = "scrambleinventory"; break;
                                case 8: type = "blockspread"; break;
                            }

                            if (type != null) {
                                int actionNum = this.getActionsCount() + 1;
                                this.editingActionKey.put(player, "action" + actionNum);

                                switch (type) {
                                    case "effect":
                                        handleInput(player, "action_effect", "Формат: <p/o/rp/not-in> [радиус] <эффект> <сила> <время>");
                                        break;
                                    case "command":
                                        handleInput(player, "action_command", "Формат: <p/o/rp/not-in> [радиус] <команда>");
                                        break;
                                    case "teleportout":
                                        handleInput(player, "action_teleportout", "Формат: <p/o/rp/not-in> [радиус] <блоки> up");
                                        break;
                                    case "particlehitbox":
                                        handleInput(player, "action_particlehitbox", "Формат: <p/o/rp/not-in> [радиус] <частица> <время>");
                                        break;
                                    case "cooldownitem":
                                        handleInput(player, "action_cooldownitem", "Формат: <p/o/rp/not-in> [радиус] <предметы> <сек>");
                                        break;
                                    case "denyitemuse":
                                        handleInput(player, "action_denyitemuse", "Формат: <p/o/rp/not-in> [радиус] <предметы>");
                                        break;
                                    case "launch":
                                        handleInput(player, "action_launch", "Формат: <p/o/rp/not-in> [радиус] <сила_вверх> <сила_в_стороны>");
                                        break;
                                    case "scrambleinventory":
                                        handleInput(player, "action_scrambleinventory", "Формат: <p/o/rp/not-in> [радиус]");
                                        break;
                                    case "blockspread":
                                        handleInput(player, "action_blockspread", "Формат: <p/o/rp/not-in> [радиус] <материал> <шанс(%)> <возврат_сек>");
                                        break;
                                }
                            }
                        }
                    } else if (event.getView().getTitle().equals("Флаги скина")) {
                        if (event.getSlot() == 26) {
                            this.currentSubMenu.remove(player);
                            this.updateMainMenu(player);
                        } else if (event.getSlot() == 25) {
                            this.handleInput(player, "flag_add", "Введите новый флаг (формат: <flag_name> <value>):");
                        } else if (event.getSlot() < 25) {
                            Map<String, String> flags = this.parseFlags((String)this.plugin.getTempSkinData().getOrDefault("flags", ""));
                            String flagKey = "flag" + (event.getSlot() + 1);
                            if (flags.containsKey(flagKey)) {
                                if (event.getClick() == ClickType.LEFT) {
                                    this.editingFlagKey.put(player, flagKey);
                                    this.handleFlagEdit(player, flagKey);
                                } else if (event.getClick() == ClickType.RIGHT) {
                                    this.removeFlag(player, flagKey);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleInput(Player player, String key, String message) {
        player.closeInventory();
        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#FB654E" + message));
        this.plugin.getChatInputHandler().waitForInput(player, (input) -> {
            if (key.equals("cooldown")) {
                try {
                    int cooldown = Integer.parseInt(input.trim());
                    if (cooldown < 0) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AКулдаун не может быть отрицательным!"));
                    } else {
                        this.plugin.getTempSkinData().put("cooldown", String.valueOf(cooldown));
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Кулдаун установлен: " + cooldown + " сек."));
                    }
                } catch (NumberFormatException var13) {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AВведите целое число для кулдауна!"));
                }
                Bukkit.getScheduler().runTask(this.plugin, () -> this.updateMainMenu(player));
            } else if (key.equals("duration")) {
                try {
                    int duration = Integer.parseInt(input.trim());
                    if (duration <= 0) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AВремя действия должно быть больше 0!"));
                    } else {
                        this.plugin.getTempSkinData().put("duration", String.valueOf(duration));
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Время действия установлено: " + duration + " сек."));
                    }
                } catch (NumberFormatException var12) {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AВведите целое число для времени действия!"));
                }
                Bukkit.getScheduler().runTask(this.plugin, () -> this.updateMainMenu(player));
            } else if (key.startsWith("action_")) {
                String actionType = key.replace("action_", "");
                Map<String, String> actions = this.parseActions((String)this.plugin.getTempSkinData().getOrDefault("actions", ""));
                String actionKey = this.editingActionKey.getOrDefault(player, "action" + (actions.size() + 1));

                String[] tokens = input.split(" ");
                if (tokens.length < 1 || !isValidTarget(tokens[0])) {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный таргет. Допустимые: p, o, rp, not-in"));
                    Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player));
                    return;
                }

                String target = tokens[0];
                double targetRadius = 5.0;  
                int offset = 1;

                 
                if (target.equalsIgnoreCase("not-in")) {
                    if (tokens.length < 2) {
                        player.sendMessage("§cДля таргета not-in необходимо указать радиус!");
                        Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player));
                        return;
                    }
                    try {
                        targetRadius = Double.parseDouble(tokens[1]);
                        offset = 2;  
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cРадиус для not-in должен быть числом!");
                        Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player));
                        return;
                    }
                }

                try {
                    switch (actionType) {
                        case "effect":
                            if (tokens.length - offset != 3) {
                                player.sendMessage("§cНеверно! Формат: <таргет> [радиус_not_in] <эффект> <сила> <время>");
                                Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player)); return;
                            }
                            Integer.parseInt(tokens[offset + 1]);
                            Integer.parseInt(tokens[offset + 2]);
                            actions.put(actionKey, "effect;" + target + ";" + targetRadius + ";" + tokens[offset] + ";" + tokens[offset+1] + ";" + tokens[offset+2]);
                            break;

                        case "command":
                            if (tokens.length - offset < 1) {
                                player.sendMessage("§cНеверно! Формат: <таргет> [радиус_not_in] <команда>");
                                Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player)); return;
                            }
                            String cmd = String.join(" ", Arrays.copyOfRange(tokens, offset, tokens.length));
                            actions.put(actionKey, "command;" + target + ";" + targetRadius + ";" + cmd);
                            break;

                        case "teleportout":
                            if (tokens.length - offset != 2 || !tokens[offset+1].equalsIgnoreCase("up")) {
                                player.sendMessage("§cНеверно! Формат: <таргет> [радиус_not_in] <блоки> up");
                                Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player)); return;
                            }
                            Integer.parseInt(tokens[offset]);
                            actions.put(actionKey, "teleportout;" + target + ";" + targetRadius + ";" + tokens[offset]);
                            break;

                        case "particlehitbox":
                            if (tokens.length - offset != 2) {
                                player.sendMessage("§cНеверно! Формат: <таргет> [радиус_not_in] <частица> <время>");
                                Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player)); return;
                            }
                            Integer.parseInt(tokens[offset+1]);
                            actions.put(actionKey, "particlehitbox;" + target + ";" + targetRadius + ";" + tokens[offset] + ";" + tokens[offset+1]);
                            break;

                        case "cooldownitem":
                            if (tokens.length - offset != 2) {
                                player.sendMessage("§cНеверно! Формат: <таргет> [радиус_not_in] <предметы> <сек>");
                                Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player)); return;
                            }
                            Integer.parseInt(tokens[offset+1]);
                            actions.put(actionKey, "cooldownitem;" + target + ";" + targetRadius + ";" + tokens[offset] + ";" + tokens[offset+1]);
                            break;

                        case "denyitemuse":
                            if (tokens.length - offset != 1) {
                                player.sendMessage("§cНеверно! Формат: <таргет> [радиус_not_in] <предметы>");
                                Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player)); return;
                            }
                            actions.put(actionKey, "denyitemuse;" + target + ";" + targetRadius + ";" + tokens[offset]);
                            break;

                        case "launch":
                            if (tokens.length - offset != 2) {
                                player.sendMessage("§cНеверно! Формат: <таргет> [радиус_not_in] <сила_вверх> <сила_в_стороны>");
                                Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player)); return;
                            }
                            Double.parseDouble(tokens[offset]);
                            Double.parseDouble(tokens[offset+1]);
                            actions.put(actionKey, "launch;" + target + ";" + targetRadius + ";" + tokens[offset] + ";" + tokens[offset+1]);
                            break;

                        case "scrambleinventory":
                            if (tokens.length - offset != 0) {
                                player.sendMessage("§cНеверно! Формат: <таргет> [радиус_not_in]");
                                Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player)); return;
                            }
                            actions.put(actionKey, "scrambleinventory;" + target + ";" + targetRadius);
                            break;

                        case "blockspread":
                             
                            if (tokens.length - offset != 4) {
                                player.sendMessage("§cНеверно! Формат: <таргет> [радиус_not_in] <радиус_блоков> <материал> <шанс(%)> <возврат_сек>");
                                Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player)); return;
                            }
                            double actionRadius = Double.parseDouble(tokens[offset]);
                            Material mat = Material.matchMaterial(tokens[offset+1].toUpperCase());
                            if (mat == null) {
                                player.sendMessage("§cУказанный материал " + tokens[offset+1] + " не найден!");
                                Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player)); return;
                            }
                            double chance = Double.parseDouble(tokens[offset+2]);
                            int revert = Integer.parseInt(tokens[offset+3]);

                            actions.put(actionKey, "blockspread;" + target + ";" + targetRadius + ";" + actionRadius + ";" + tokens[offset+1].toUpperCase() + ";" + chance + ";" + revert);
                            break;
                    }
                } catch (Exception e) {
                    player.sendMessage("§cОшибка: Проверьте правильность введенных чисел!");
                    Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player));
                    return;
                }

                this.plugin.getTempSkinData().put("actions", this.serializeActions(actions));
                this.editingActionKey.remove(player);
                Bukkit.getScheduler().runTask(this.plugin, () -> this.openActionsMenu(player));
            } else if (key.equals("flag_add")) {
                String[] parts = input.split(" ", 2);
                if (parts.length != 2) {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный формат флага. Ожидается: <flag_name> <value>"));
                    Bukkit.getScheduler().runTask(this.plugin, () -> this.openFlagsMenu(player));
                    return;
                }

                String flagKey = parts[0];
                String flagValue = parts[1].toUpperCase();

                try {
                    StateFlag flag = (StateFlag)WorldGuard.getInstance().getFlagRegistry().get(flagKey);
                    if (flag == null) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AФлаг '" + flagKey + "' не найден."));
                        Bukkit.getScheduler().runTask(this.plugin, () -> this.openFlagsMenu(player));
                        return;
                    }
                    State.valueOf(flagValue);
                } catch (IllegalArgumentException var14) {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректное значение флага. Ожидается: ALLOW или DENY"));
                    Bukkit.getScheduler().runTask(this.plugin, () -> this.openFlagsMenu(player));
                    return;
                }

                Map<String, String> flags = this.parseFlags((String)this.plugin.getTempSkinData().getOrDefault("flags", ""));
                String flagData = flagKey + ":" + flagValue;
                flags.put("flag" + (flags.size() + 1), flagData);
                this.plugin.getTempSkinData().put("flags", this.serializeFlags(flags));
                player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Флаг '" + flagKey + "' добавлен с значением: " + flagValue));
                Bukkit.getScheduler().runTask(this.plugin, () -> this.openFlagsMenu(player));
            } else {
                this.plugin.getTempSkinData().put(key, input);
                player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Значение '" + key + "' установлено: " + input));
                Bukkit.getScheduler().runTask(this.plugin, () -> this.updateMainMenu(player));
            }
        });
    }

    private void handleFlagEdit(Player player, String flagKey) {
        Map<String, String> flags = this.parseFlags((String)this.plugin.getTempSkinData().getOrDefault("flags", ""));
        if (flags.containsKey(flagKey)) {
            String flagData = (String)flags.get(flagKey);
            String[] parts = flagData.split(":", 2);
            String currentFlag = parts[0];
            String currentValue = parts[1];
            player.closeInventory();
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#FB654EВведите новое значение для флага '" + currentFlag + "' (текущий: " + currentValue + ", формат: <flag_name> <value>):"));
            this.plugin.getChatInputHandler().waitForInput(player, (input) -> {
                String[] newParts = input.split(" ", 2);
                if (newParts.length == 2 && newParts[0].equalsIgnoreCase(currentFlag)) {
                    String newValue = newParts[1].toUpperCase();
                    try {
                        StateFlag flag = (StateFlag)WorldGuard.getInstance().getFlagRegistry().get(currentFlag);
                        if (flag == null) {
                            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AФлаг '" + currentFlag + "' не найден."));
                            Bukkit.getScheduler().runTask(this.plugin, () -> this.openFlagsMenu(player));
                            return;
                        }
                        State.valueOf(newValue);
                    } catch (IllegalArgumentException var9) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректное значение флага. Ожидается: ALLOW или DENY"));
                        Bukkit.getScheduler().runTask(this.plugin, () -> this.openFlagsMenu(player));
                        return;
                    }
                    flags.put(flagKey, currentFlag + ":" + newValue);
                    this.plugin.getTempSkinData().put("flags", this.serializeFlags(flags));
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Флаг '" + currentFlag + "' обновлен: " + newValue));
                    this.editingFlagKey.remove(player);
                    Bukkit.getScheduler().runTask(this.plugin, () -> this.openFlagsMenu(player));
                } else {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный формат или имя флага. Ожидается: " + currentFlag + " <value>"));
                    Bukkit.getScheduler().runTask(this.plugin, () -> this.openFlagsMenu(player));
                }
            });
        } else {
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный ключ флага."));
            Bukkit.getScheduler().runTask(this.plugin, () -> this.openFlagsMenu(player));
        }
    }

    private void removeFlag(Player player, String flagKey) {
        Map<String, String> flags = this.parseFlags((String)this.plugin.getTempSkinData().getOrDefault("flags", ""));
        if (flags.containsKey(flagKey)) {
            String removedFlag = (String)flags.remove(flagKey);
            String serialized = flags.isEmpty() ? "" : this.serializeFlags(flags);
            this.plugin.getTempSkinData().put("flags", serialized);
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Флаг '" + flagKey + "' удален."));
        } else {
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный ключ флага."));
        }
        Bukkit.getScheduler().runTask(this.plugin, () -> this.openFlagsMenu(player));
    }

    private Map<String, String> parseActions(String actionsStr) {
        Map<String, String> actions = new HashMap();
        if (actionsStr.isEmpty()) {
            return actions;
        } else {
            String[] actionEntries = actionsStr.split("\\|");
            for(String entry : actionEntries) {
                String[] parts = entry.split(":", 2);
                if (parts.length == 2) {
                    actions.put(parts[0], parts[1]);
                }
            }
            return actions;
        }
    }

    private String serializeActions(Map<String, String> actions) {
        List<String> entries = new ArrayList();
        for(Map.Entry<String, String> entry : actions.entrySet()) {
            String var10001 = (String)entry.getKey();
            entries.add(var10001 + ":" + (String)entry.getValue());
        }
        return String.join("|", entries);
    }

    private Map<String, String> parseFlags(String flagsStr) {
        Map<String, String> flags = new HashMap();
        if (flagsStr.isEmpty()) {
            return flags;
        } else {
            String[] flagEntries = flagsStr.split("\\|");
            for(String entry : flagEntries) {
                String[] parts = entry.split(":", 2);
                if (parts.length == 2) {
                    flags.put(parts[0], parts[1]);
                }
            }
            return flags;
        }
    }

    private String serializeFlags(Map<String, String> flags) {
        List<String> entries = new ArrayList();
        for(Map.Entry<String, String> entry : flags.entrySet()) {
            String var10001 = (String)entry.getKey();
            entries.add(var10001 + ":" + (String)entry.getValue());
        }
        return String.join("|", entries);
    }

    private boolean isValidTarget(String target) {
        return target.equalsIgnoreCase("p") || target.equalsIgnoreCase("o") || target.equalsIgnoreCase("rp") || target.equalsIgnoreCase("not-in");
    }

    private void handleActionEdit(Player player, String actionKey) {
        Map<String, String> actions = this.parseActions((String)this.plugin.getTempSkinData().getOrDefault("actions", ""));
        if (actions.containsKey(actionKey)) {
            String actionData = (String)actions.get(actionKey);
            String[] parts = actionData.split(";", 2);
            String type = parts[0];
            String data = parts.length > 1 ? parts[1].replace(";", " ") : "";
            switch (type.toLowerCase()) {
                case "effect":
                    this.handleInput(player, "action_effect", "Формат: <target> [radius] <эффект> <сила> <время>\nТекущий: " + data);
                    break;
                case "command":
                    this.handleInput(player, "action_command", "Формат: <target> [radius] <команда>\nТекущая: " + data);
                    break;
                case "teleportout":
                    this.handleInput(player, "action_teleportout", "Формат: <target> [radius] <блоки> up\nТекущая: " + data);
                    break;
                case "particlehitbox":
                    this.handleInput(player, "action_particlehitbox", "Формат: <target> [radius] <частица> <время>\nТекущие: " + data);
                    break;
                case "cooldownitem":
                    this.handleInput(player, "action_cooldownitem", "Формат: <target> [radius] <предметы> <сек>\nТекущие: " + data);
                    break;
                case "denyitemuse":
                    this.handleInput(player, "action_denyitemuse", "Формат: <target> [radius] <предметы>\nТекущие: " + data);
                    break;
                case "launch":
                    this.handleInput(player, "action_launch", "Формат: <target> [radius] <сила_вверх> <сила_в_стороны>\nТекущее: " + data);
                    break;
                case "scrambleinventory":
                    this.handleInput(player, "action_scrambleinventory", "Формат: <target> [radius]\nТекущий: " + data);
                    break;
                case "blockspread":
                    this.handleInput(player, "action_blockspread", "Формат: <target> [radius] <материал> <шанс(%)> <возврат_сек>\nТекущее: " + data);
                    break;
                default:
                    player.sendMessage("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНеизвестный тип действия: " + type);
                    this.openActionsMenu(player);
            }
        }
    }

    private void saveSkin(Player player) {
        String skinName = (String) this.plugin.getTempSkinData().get("name");
        String schematic = (String) this.plugin.getTempSkinData().get("schem");
        String sound = (String) this.plugin.getTempSkinData().get("sound.type");
        String soundEnded = (String) this.plugin.getTempSkinData().getOrDefault("sound.type-ended", "ENTITY_WITHER_AMBIENT");

        Object cooldownObj = this.plugin.getTempSkinData().get("cooldown");
        String cooldown = cooldownObj != null ? cooldownObj.toString() : null;

        Object animObj = this.plugin.getTempSkinData().get("animation");
        String animation = animObj != null ? animObj.toString() : null;

        Object durationObj = this.plugin.getTempSkinData().get("duration");
        String duration = durationObj != null ? durationObj.toString() : null;

        List<String> descriptionLines = this.getDescriptionLines();

        if (skinName != null && schematic != null && sound != null && cooldown != null && duration != null) {

            List<String> coloredDesc = new ArrayList<>();
            for (String line : descriptionLines) {
                coloredDesc.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            if (animation != null && !animation.equals("Не задано") && !animation.isEmpty()) {
                this.plugin.getConfig().set("skins." + skinName + ".animation", animation);
            } else {
                this.plugin.getConfig().set("skins." + skinName + ".animation", null);
            }
            this.plugin.getConfig().set("skins." + skinName + ".schem", schematic);
            this.plugin.getConfig().set("skins." + skinName + ".sound.type", sound);
            this.plugin.getConfig().set("skins." + skinName + ".sound.volume", 1.0F);
            this.plugin.getConfig().set("skins." + skinName + ".sound.pitch", 1.0F);
            this.plugin.getConfig().set("skins." + skinName + ".sound.type-ended", soundEnded);
            this.plugin.getConfig().set("skins." + skinName + ".sound.volume-ended", 1.0F);
            this.plugin.getConfig().set("skins." + skinName + ".sound.pitch-ended", 1.0F);

            try {
                this.plugin.getConfig().set("skins." + skinName + ".cooldown", Integer.parseInt(cooldown));
                this.plugin.getConfig().set("skins." + skinName + ".duration", Integer.parseInt(duration));
            } catch (NumberFormatException e) {
                player.sendMessage("§cОшибка: Кулдаун или длительность должны быть числами!");
                return;
            }

            Map<String, String> actions = this.parseActions((String)this.plugin.getTempSkinData().getOrDefault("actions", ""));
            this.plugin.getConfig().set("skins." + skinName + ".actions", null);

            for(Map.Entry<String, String> actionEntry : actions.entrySet()) {
                String actionKey = (String)actionEntry.getKey();
                String actionData = (String)actionEntry.getValue();
                if (actionData == null) continue;

                String[] parts = actionData.split(";");
                if (parts.length >= 3) {
                    String type = parts[0];
                    String target = parts[1];
                    double radius = Double.parseDouble(parts[2]);
                    String basePath = "skins." + skinName + ".actions." + actionKey;

                    this.plugin.getConfig().set(basePath + ".type", type);

                    switch (type.toLowerCase()) {
                        case "effect":
                            this.plugin.getConfig().set(basePath + ".target", target);
                            this.plugin.getConfig().set(basePath + ".radius", radius);
                            this.plugin.getConfig().set(basePath + ".effect", parts[3]);
                            this.plugin.getConfig().set(basePath + ".amplifier", Integer.parseInt(parts[4]));
                            this.plugin.getConfig().set(basePath + ".duration", Integer.parseInt(parts[5]));
                            break;
                        case "command":
                            this.plugin.getConfig().set(basePath + ".radius", radius);
                            this.plugin.getConfig().set(basePath + ".command", parts[3] + " " + target);
                            break;
                        case "teleportout":
                            this.plugin.getConfig().set(basePath + ".radius", radius);
                            this.plugin.getConfig().set(basePath + ".teleport", target + " " + parts[3] + " up");
                            this.plugin.getConfig().set(basePath + ".min-height", 10);
                            break;
                        case "particlehitbox":
                            this.plugin.getConfig().set(basePath + ".target", target);
                            this.plugin.getConfig().set(basePath + ".radius", radius);
                            this.plugin.getConfig().set(basePath + ".particle-type", parts[3]);
                            this.plugin.getConfig().set(basePath + ".duration", Integer.parseInt(parts[4]));
                            this.plugin.getConfig().set(basePath + ".update-interval", 4);
                            break;
                        case "cooldownitem":
                            this.plugin.getConfig().set(basePath + ".target", target);
                            this.plugin.getConfig().set(basePath + ".radius", radius);
                            this.plugin.getConfig().set(basePath + ".items", Arrays.asList(parts[3].split(",")));
                            this.plugin.getConfig().set(basePath + ".seconds", Integer.parseInt(parts[4]));
                            break;
                        case "denyitemuse":
                            this.plugin.getConfig().set(basePath + ".target", target);
                            this.plugin.getConfig().set(basePath + ".radius", radius);
                            this.plugin.getConfig().set(basePath + ".items", Arrays.asList(parts[3].split(",")));
                            break;
                        case "launch":
                            this.plugin.getConfig().set(basePath + ".target", target);
                            this.plugin.getConfig().set(basePath + ".radius", radius);
                            this.plugin.getConfig().set(basePath + ".upward-force", Double.parseDouble(parts[3]));
                            this.plugin.getConfig().set(basePath + ".horizontal-force", Double.parseDouble(parts[4]));
                            break;
                        case "scrambleinventory":
                            this.plugin.getConfig().set(basePath + ".target", target);
                            this.plugin.getConfig().set(basePath + ".radius", radius);
                            break;
                        case "blockspread":
                            this.plugin.getConfig().set(basePath + ".target", target);
                            this.plugin.getConfig().set(basePath + ".radius", radius);  
                            this.plugin.getConfig().set(basePath + ".action-radius", Double.parseDouble(parts[3]));  
                            this.plugin.getConfig().set(basePath + ".material", parts[4]);
                            this.plugin.getConfig().set(basePath + ".chance", Double.parseDouble(parts[5]));
                            this.plugin.getConfig().set(basePath + ".revert-time", Integer.parseInt(parts[6]));
                            break;
                    }
                }
            }

            Map<String, String> flags = this.parseFlags((String)this.plugin.getTempSkinData().getOrDefault("flags", ""));
            this.plugin.getConfig().set("skins." + skinName + ".flags", null);

            for(Map.Entry<String, String> flagEntry : flags.entrySet()) {
                String flagValueRaw = (String)flagEntry.getValue();
                if (flagValueRaw != null) {
                    String[] parts = flagValueRaw.split(":", 2);
                    if (parts.length == 2) {
                        String flagName = parts[0];
                        String flagValue = parts[1];
                        this.plugin.getConfig().set("skins." + skinName + ".flags." + flagName, flagValue);
                    }
                }
            }

            this.plugin.saveConfig();
            this.plugin.getTempSkinData().clear();
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Скин '" + skinName + "' успешно сохранён!"));
            player.closeInventory();
        } else {
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AПожалуйста, заполните все обязательные поля!"));
        }
    }
}