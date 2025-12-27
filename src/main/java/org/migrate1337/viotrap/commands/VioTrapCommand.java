package org.migrate1337.viotrap.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.utils.ActiveSkinsManager;
import org.migrate1337.viotrap.utils.GiveItemTabCompleter;
import org.migrate1337.viotrap.utils.SkinPointsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VioTrapCommand implements CommandExecutor, TabCompleter {

    private final VioTrap plugin;

    // Ссылки на исполнителей подкоманд
    private final GiveItemCommand giveItemCommand;
    private final CreateSkinCommand createSkinCommand;
    private final CreatePlateSkinCommand createPlateSkinCommand;
    private final ApplySkinCommand applySkinCommand;
    private final ApplyPlateSkinCommand applyPlateSkinCommand;
    private final SkinPointsCommand skinPointsCommand;

    // Ссылки на таб-комплитеры (если есть отдельные)
    private final GiveItemTabCompleter giveItemTabCompleter;

    public VioTrapCommand(VioTrap plugin,
                          SkinPointsManager pointsManager,
                          ActiveSkinsManager activeSkinsManager) {
        this.plugin = plugin;

        // Инициализируем логику всех команд
        this.giveItemCommand = new GiveItemCommand(plugin, activeSkinsManager);
        this.createSkinCommand = new CreateSkinCommand(plugin);
        this.createPlateSkinCommand = new CreatePlateSkinCommand(plugin);
        this.applySkinCommand = new ApplySkinCommand(plugin, pointsManager, activeSkinsManager);
        this.applyPlateSkinCommand = new ApplyPlateSkinCommand(plugin, pointsManager, activeSkinsManager);
        this.skinPointsCommand = new SkinPointsCommand(plugin, pointsManager);

        this.giveItemTabCompleter = new GiveItemTabCompleter();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        if (!sender.hasPermission("viotrap.op")){
            return false;
        }
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("give")) {
            return giveItemCommand.onCommand(sender, command, label, args);
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "createskin":
                return createSkinCommand.onCommand(sender, command, label, subArgs);
            case "createplateskin":
                return createPlateSkinCommand.onCommand(sender, command, label, subArgs);
            case "applyskin":
                return applySkinCommand.onCommand(sender, command, label, subArgs);
            case "applyplateskin":
                return applyPlateSkinCommand.onCommand(sender, command, label, subArgs);
            case "skinpoints":
                return skinPointsCommand.onCommand(sender, command, label, subArgs);
            case "conditions":
                plugin.getConditionEditorMenu().openMainMenu((Player) sender);
                return true;
            default:
                sender.sendMessage("§cНеизвестная подкоманда. Используйте /viotrap info для помощи.");
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage("§x§0§0§F§F§7§F§l✦ §x§5§5§F§F§5§5V§x§A§A§F§F§A§Ai§x§F§F§F§F§F§Fo§x§F§F§D§D§F§FT§x§F§F§B§B§F§Fr§x§F§F§9§9§F§Fa§x§F§F§7§7§F§Fp §8| §fПомощь §x§0§0§F§F§7§F§l✦");
        sender.sendMessage("§7Список доступных команд:");
        sender.sendMessage(" ");

        sendHelpLine(sender, "info", "Показать этот список");
        sendHelpLine(sender, "give <player> <item> [amount]", "Выдать спец. предметы");
        sendHelpLine(sender, "createskin", "Открыть меню создания скина ловушки");
        sendHelpLine(sender, "createplateskin", "Открыть меню создания скина пласта");
        sendHelpLine(sender, "applyskin <player> <skin>", "Применить скин ловушки игроку");
        sendHelpLine(sender, "applyplateskin <player> <skin>", "Применить скин пласта игроку");
        sendHelpLine(sender, "conditions", "Настройка условий");
        sendHelpLine(sender, "skinpoints <add/remove> ...", "Управление очками скинов");
        sender.sendMessage(" ");
        sender.sendMessage("§7Плагин разработан: §x§5§5§F§F§5§5@etern4al1en");
    }

    private void sendHelpLine(CommandSender sender, String args, String desc) {
        sender.sendMessage(" §x§0§0§F§F§7§F➜ §f/viotrap " + args);
        sender.sendMessage("   §7" + desc);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList(
                    "info", "give", "createskin", "createplateskin",
                    "applyskin", "applyplateskin", "skinpoints", "conditions"
            );
            return StringUtil.copyPartialMatches(args[0], subcommands, new ArrayList<>());
        }

        String subCommand = args[0].toLowerCase();

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "give":
                return giveItemTabCompleter.onTabComplete(sender, command, alias, args);
            case "skinpoints":
                return skinPointsCommand.onTabComplete(sender, command, alias, subArgs);

            case "applyskin":
            case "applyplateskin":
                if (args.length == 2) return null;
                break;

        }

        return Collections.emptyList();
    }
}