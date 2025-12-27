package org.migrate1337.viotrap.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.utils.ActiveSkinsManager;
import org.migrate1337.viotrap.utils.SkinPointsManager;

public class ApplySkinCommand implements CommandExecutor {
    private final VioTrap plugin;
    private final SkinPointsManager pointsManager;
    private final ActiveSkinsManager activeSkinsManager;
    private final String PREFIX = "§x§0§0§F§F§7§F§l✦ §x§5§5§F§F§5§5V§x§A§A§F§F§A§Ai§x§F§F§F§F§F§Fo§x§F§F§D§D§F§FT§x§F§F§B§B§F§Fr§x§F§F§9§9§F§Fa§x§F§F§7§7§F§Fp §8| §f";

    public ApplySkinCommand(VioTrap plugin, SkinPointsManager pointsManager, ActiveSkinsManager activeSkinsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
        this.activeSkinsManager = activeSkinsManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§cИспользование: §f/viotrap applyskin <игрок> <скин|all>");
            return true;
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(PREFIX + "§cИгрок §f" + args[0] + " §cне найден.");
                return true;
            } else {
                String skinOrAll = args[1];
                if (!skinOrAll.equalsIgnoreCase("all")) {
                    if (!this.plugin.getSkinNames().contains(skinOrAll)) {
                        sender.sendMessage(PREFIX + "§cСкин §f" + skinOrAll + " §cне существует.");
                        return true;
                    } else {
                        int requiredPoints = this.plugin.getConfig().getInt("skins." + skinOrAll + ".points", 0);
                        if (requiredPoints > 0 && this.pointsManager.getPoints(target.getUniqueId(), skinOrAll) < requiredPoints) {
                            sender.sendMessage(PREFIX + "§cУ игрока недостаточно поинтов для скина §f" + skinOrAll);
                            return true;
                        } else {
                            this.activeSkinsManager.setActiveTrapSkin(target.getUniqueId(), skinOrAll);
                            if (requiredPoints > 0) {
                                this.pointsManager.removePoints(target.getUniqueId(), skinOrAll, requiredPoints);
                            }

                            sender.sendMessage(PREFIX + "Скин §x§5§5§F§F§5§5" + skinOrAll + " §fуспешно применен игроку §x§5§5§F§F§5§5" + target.getName());
                            return true;
                        }
                    }
                } else {
                    int appliedCount = 0;
                    for(String skin : this.plugin.getSkinNames()) {
                        int requiredPoints = this.plugin.getConfig().getInt("skins." + skin + ".points", 0);
                        if (requiredPoints <= 0 || this.pointsManager.getPoints(target.getUniqueId(), skin) >= requiredPoints) {

                            this.activeSkinsManager.setActiveTrapSkin(target.getUniqueId(), skin);
                            if (requiredPoints > 0) {
                                this.pointsManager.removePoints(target.getUniqueId(), skin, requiredPoints);
                            }
                            ++appliedCount;
                        }
                    }

                    if (appliedCount > 0) {
                        sender.sendMessage(PREFIX + "Обработано §x§5§5§F§F§5§5" + appliedCount + " §fскинов для §x§5§5§F§F§5§5" + target.getName());
                    } else {
                        sender.sendMessage(PREFIX + "§cНе удалось применить скины: недостаточно очков.");
                    }

                    return true;
                }
            }
        }
    }
}