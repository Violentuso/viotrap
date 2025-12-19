package org.migrate1337.viotrap.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.*;
import org.migrate1337.viotrap.utils.ActiveSkinsManager;

public class GiveItemCommand implements CommandExecutor {

    private final VioTrap plugin;
    private final ActiveSkinsManager activeSkinsManager;

    public GiveItemCommand(VioTrap plugin, ActiveSkinsManager activeSkinsManager) {
        this.plugin = plugin;
        this.activeSkinsManager = activeSkinsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viotrap.*")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на эту команду!");
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.RED + "Использование: /viotrap give <игрок> <предмет> [количество]");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок '" + args[1] + "' не найден или оффлайн.");
            return true;
        }

        String itemName = args[2].toLowerCase();
        int amount = 1;

        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1) amount = 1;
                if (amount > 64) amount = 64;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Количество должно быть числом от 1 до 64!");
                return true;
            }
        }

        ItemStack item = createItemWithPlayerActiveSkin(itemName, target, amount);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Неизвестный предмет: " + args[2]);
            sender.sendMessage(ChatColor.GRAY + "Доступные: трапка, пласт, явная_пыль, дезориентация, божья_аура, огненный_смерч");
            return true;
        }

        target.getInventory().addItem(item);
        target.updateInventory();

        // Определяем активный скин
        String activeSkin = null;
        if ("трапка".equals(itemName)) {
            activeSkin = activeSkinsManager.getActivePlateSkin(target.getUniqueId());
        } else if ("пласт".equals(itemName)) {
            activeSkin = activeSkinsManager.getActivePlateSkin(target.getUniqueId());
        }

        String skinInfo = "";
        if (activeSkin != null) {
            skinInfo = ChatColor.GRAY + " (скин: " + ChatColor.YELLOW + activeSkin + ChatColor.GRAY + ")";
        } else if ("трапка".equals(itemName) || "пласт".equals(itemName)) {
            skinInfo = ChatColor.GRAY + " (дефолт)";
        }

        // Красивое название предмета
        String displayName;
        if ("трапка".equals(itemName)) displayName = "трапка";
        else if ("пласт".equals(itemName)) displayName = "пласт";
        else if ("явная_пыль".equals(itemName)) displayName = "явная пыль";
        else if ("дезориентация".equals(itemName)) displayName = "дезориентация";
        else if ("божья_аура".equals(itemName)) displayName = "божья аура";
        else if ("огненный_смерч".equals(itemName)) displayName = "огненный смерч";
        else displayName = itemName;

        String message = ChatColor.GREEN + "Выдано " + ChatColor.WHITE + amount + "× " + displayName + skinInfo
                + ChatColor.GREEN + " → " + ChatColor.AQUA + target.getName();





        return true;
    }

    private ItemStack createItemWithPlayerActiveSkin(String itemName, Player player, int amount) {
        if ("трапка".equals(itemName)) {
            String skin = activeSkinsManager.getActiveTrapSkin(player.getUniqueId());
            return TrapItem.getTrapItem(amount, skin);
        }
        if ("пласт".equals(itemName)) {
            String skin = activeSkinsManager.getActivePlateSkin(player.getUniqueId());
            return PlateItem.getPlateItem(amount, skin);
        }
        if ("явная_пыль".equals(itemName)) {
            return RevealItem.getRevealItem(amount);
        }
        if ("дезориентация".equals(itemName)) {
            return DisorientItem.getDisorientItem(amount);
        }
        if ("божья_аура".equals(itemName)) {
            return DivineAuraItem.getDivineAuraItem(amount, plugin);
        }
        if ("огненный_смерч".equals(itemName)) {
            return FirestormItem.getFirestormItem(amount, plugin);
        }
        return null;
    }
}