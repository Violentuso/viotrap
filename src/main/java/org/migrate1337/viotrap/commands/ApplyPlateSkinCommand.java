package org.migrate1337.viotrap.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.utils.ActiveSkinsManager;
import org.migrate1337.viotrap.utils.SkinPointsManager;

public class ApplyPlateSkinCommand implements CommandExecutor {
    private final VioTrap plugin;
    private final SkinPointsManager pointsManager;
    private final ActiveSkinsManager activeSkinsManager;
    private final String PREFIX = "§x§0§0§F§F§7§F§l✦ §x§5§5§F§F§5§5V§x§A§A§F§F§A§Ai§x§F§F§F§F§F§Fo§x§F§F§D§D§F§FT§x§F§F§B§B§F§Fr§x§F§F§9§9§F§Fa§x§F§F§7§7§F§Fp §8| §f";

    public ApplyPlateSkinCommand(VioTrap plugin, SkinPointsManager pointsManager, ActiveSkinsManager activeSkinsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
        this.activeSkinsManager = activeSkinsManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§cИспользование: §f/viotrap applyplateskin <игрок> <скин|all>");
            return true;
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(PREFIX + "§cИгрок §f" + args[0] + " §cне найден.");
                return true;
            } else {
                String skinOrAll = args[1];
                if (!skinOrAll.equalsIgnoreCase("all")) {
                    if (!this.plugin.getPlateSkinNames().contains(skinOrAll)) {
                        sender.sendMessage(PREFIX + "§cСкин пласта §f" + skinOrAll + " §cне существует.");
                        return true;
                    } else {
                        int requiredPoints = this.plugin.getConfig().getInt("plate_skins." + skinOrAll + ".points", 0);
                        if (requiredPoints > 0 && this.pointsManager.getPoints(target.getUniqueId(), skinOrAll) < requiredPoints) {
                            sender.sendMessage(PREFIX + "§cУ игрока недостаточно поинтов для скина §f" + skinOrAll);
                            return true;
                        } else {
                            this.activeSkinsManager.setActivePlateSkin(target.getUniqueId(), skinOrAll);
                            if (requiredPoints > 0) {
                                this.pointsManager.removePoints(target.getUniqueId(), skinOrAll, requiredPoints);
                            }

                            sender.sendMessage(PREFIX + "Скин пласта §x§5§5§F§F§5§5" + skinOrAll + " §fустановлен игроку §x§5§5§F§F§5§5" + target.getName());
                            target.sendMessage(PREFIX + "Вам установлен скин пласта: §x§5§5§F§F§5§5" + skinOrAll);
                            return true;
                        }
                    }
                } else {
                    int appliedCount = 0;
                    for(String skin : this.plugin.getPlateSkinNames()) {
                        int requiredPoints = this.plugin.getConfig().getInt("plate_skins." + skin + ".points", 0);
                        if (requiredPoints <= 0 || this.pointsManager.getPoints(target.getUniqueId(), skin) >= requiredPoints) {
                            this.activeSkinsManager.setActivePlateSkin(target.getUniqueId(), skin);
                            if (requiredPoints > 0) {
                                this.pointsManager.removePoints(target.getUniqueId(), skin, requiredPoints);
                            }
                            ++appliedCount;
                        }
                    }

                    if (appliedCount > 0) {
                        sender.sendMessage(PREFIX + "Обработано §x§5§5§F§F§5§5" + appliedCount + " §fскинов пластов для §x§5§5§F§F§5§5" + target.getName());
                        target.sendMessage(PREFIX + "К вам применена массовая операция скинов пластов.");
                    } else {
                        sender.sendMessage(PREFIX + "§cНе удалось применить скины: недостаточно очков.");
                    }

                    return true;
                }
            }
        }
    }
}