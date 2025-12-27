package org.migrate1337.viotrap.commands;

import org.bukkit.Bukkit;
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

    // Красивый префикс
    private final String PREFIX = "§x§0§0§F§F§7§F§l✦ §x§5§5§F§F§5§5V§x§A§A§F§F§A§Ai§x§F§F§F§F§F§Fo§x§F§F§D§D§F§FT§x§F§F§B§B§F§Fr§x§F§F§9§9§F§Fa§x§F§F§7§7§F§Fp §8| §f";

    public GiveItemCommand(VioTrap plugin, ActiveSkinsManager activeSkinsManager) {
        this.plugin = plugin;
        this.activeSkinsManager = activeSkinsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viotrap.give")) { // Лучше использовать конкретное право, а не *
            sender.sendMessage(PREFIX + "§cУ вас недостаточно прав.");
            return true;
        }

        // Так как это вызывается через диспетчер, args[0] это "give"
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§cИспользование: §f/viotrap give <игрок> <предмет> [кол-во]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + "§cИгрок §f" + args[1] + " §cне найден.");
            return true;
        }

        String itemName = args[2].toLowerCase();
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(PREFIX + "§cНеверное количество.");
                return true;
            }
        }

        ItemStack itemToGive = createItemWithPlayerActiveSkin(itemName, target, amount);

        if (itemToGive == null) {
            sender.sendMessage(PREFIX + "§cПредмет §f" + itemName + " §cне найден.");
            return true;
        }

        target.getInventory().addItem(itemToGive);


        String displayName;
        if ("трапка".equals(itemName)) displayName = "Трапка";
        else if ("пласт".equals(itemName)) displayName = "Пласт";
        else if ("явная_пыль".equals(itemName)) displayName = "Явная пыль";
        else if ("дезориентация".equals(itemName)) displayName = "Дезориентация";
        else if ("божья_аура".equals(itemName)) displayName = "Божья аура";
        else if ("огненный_смерч".equals(itemName)) displayName = "Огненный смерч";
        else displayName = itemName;

        String skinInfo = "";
        if ("трапка".equals(itemName)) {
            String skin = activeSkinsManager.getActiveTrapSkin(target.getUniqueId());
            skinInfo = " §7(Скин: " + (skin.equals("default") ? "Обычный" : skin) + ")";
        } else if ("пласт".equals(itemName)) {
            String skin = activeSkinsManager.getActivePlateSkin(target.getUniqueId());
            skinInfo = " §7(Скин: " + (skin.equals("default") ? "Обычный" : skin) + ")";
        }


        sender.sendMessage(PREFIX + "Выдано §x§5§5§F§F§5§5" + amount + "шт. §f" + displayName + skinInfo);
        sender.sendMessage("      §x§0§0§F§F§7§F➜ §fИгроку: §x§5§5§F§F§5§5" + target.getName());

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