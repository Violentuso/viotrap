package org.migrate1337.viotrap.commands;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.PlateItem;
import org.migrate1337.viotrap.items.TrapItem;
import org.migrate1337.viotrap.utils.ActiveSkinsManager;
import org.migrate1337.viotrap.utils.SkinPointsManager;

public class ApplySkinCommand implements CommandExecutor {
    private final VioTrap plugin;
    private final SkinPointsManager pointsManager;
    private final ActiveSkinsManager activeSkinsManager;

    public ApplySkinCommand(VioTrap plugin, SkinPointsManager pointsManager, ActiveSkinsManager activeSkinsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
        this.activeSkinsManager = activeSkinsManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /applyskin <player> <skin|all>");
            return true;
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cИгрок " + args[0] + " не найден.");
                return true;
            } else if (!sender.hasPermission("viotrap.applyskin")) {
                sender.sendMessage("§cУ вас нет прав на использование данной команды!");
                return true;
            } else {
                String skinOrAll = args[1];
                if (!skinOrAll.equalsIgnoreCase("all")) {
                    boolean isTrapSkin = this.plugin.getSkinNames().contains(skinOrAll);
                    boolean isPlateSkin = this.plugin.getPlateSkinNames().contains(skinOrAll);
                    if (!isTrapSkin && !isPlateSkin) {
                        sender.sendMessage("§cСкин " + skinOrAll + " не найден.");
                        return true;
                    } else {
                        int requiredPoints = 0;
                        if (isTrapSkin) {
                            requiredPoints = this.plugin.getConfig().getInt("skins." + skinOrAll + ".points", 0);
                        } else if (isPlateSkin) {
                            requiredPoints = this.plugin.getConfig().getInt("plate_skins." + skinOrAll + ".points", 0);
                        }

                        if (requiredPoints > 0 && this.pointsManager.getPoints(target.getUniqueId(), skinOrAll) < requiredPoints) {
                            sender.sendMessage("§cУ игрока недостаточно очков для активации скина " + skinOrAll + "!");
                            return true;
                        } else {
                            if (isTrapSkin) {
                                this.activeSkinsManager.setActiveTrapSkin(target.getUniqueId(), skinOrAll);

                            } else if (isPlateSkin) {
                                this.activeSkinsManager.setActiveTrapSkin(target.getUniqueId(), skinOrAll);

                            }

                            if (requiredPoints > 0) {
                                this.pointsManager.removePoints(target.getUniqueId(), skinOrAll, requiredPoints);
                            }

                            sender.sendMessage("§aСкин " + skinOrAll + " успешно применён для игрока " + target.getName() + "!");
                            target.sendMessage("§aВам применён скин '" + skinOrAll + "'!");
                            return true;
                        }
                    }
                } else {
                    int appliedCount = 0;

                    for(String skin : this.plugin.getSkinNames()) {
                        int requiredPoints = this.plugin.getConfig().getInt("skins." + skin + ".points", 0);
                        if (requiredPoints > 0 && this.pointsManager.getPoints(target.getUniqueId(), skin) < requiredPoints) {
                        } else {
                            this.activeSkinsManager.setActiveTrapSkin(target.getUniqueId(), skinOrAll);
                            if (requiredPoints > 0) {
                                this.pointsManager.removePoints(target.getUniqueId(), skin, requiredPoints);
                            }

                            ++appliedCount;
                        }
                    }

                    if (appliedCount > 0) {
                        sender.sendMessage("§aПрименено " + appliedCount + " скинов для игрока " + target.getName() + "!");
                        target.sendMessage("§aВам применено " + appliedCount + " скинов!");
                    } else {
                        sender.sendMessage("§cНе удалось применить ни один скин для игрока " + target.getName() + ": недостаточно очков.");
                    }

                    return true;
                }
            }
        }
    }

}
