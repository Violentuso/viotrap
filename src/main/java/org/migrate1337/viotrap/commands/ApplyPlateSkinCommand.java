package org.migrate1337.viotrap.commands;

import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.PlateItem;
import org.migrate1337.viotrap.utils.ActiveSkinsManager;
import org.migrate1337.viotrap.utils.SkinPointsManager;

public class ApplyPlateSkinCommand implements CommandExecutor {
    private final VioTrap plugin;
    private final SkinPointsManager pointsManager;
    private final ActiveSkinsManager activeSkinsManager;

    public ApplyPlateSkinCommand(VioTrap plugin, SkinPointsManager pointsManager, ActiveSkinsManager activeSkinsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
        this.activeSkinsManager = activeSkinsManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Использование: /applyplateskin <игрок> <скин|all>");
            return true;
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Игрок не найден или не в сети.");
                return true;
            } else if (!sender.hasPermission("viotrap.applyplateskin")) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "У вас нет прав на использование данной команды!");
                return true;
            } else {
                String skinOrAll = args[1];
                if (!skinOrAll.equalsIgnoreCase("all")) {
                    if (!this.plugin.getPlateSkinNames().contains(skinOrAll)) {
                        String var16 = String.valueOf(ChatColor.RED);
                        sender.sendMessage(var16 + "Скин '" + skinOrAll + "' не найден.");
                        return true;
                    } else {
                        int requiredPoints = this.plugin.getConfig().getInt("plate_skins." + skinOrAll + ".points", 0);
                        this.plugin.getLogger().info("[VioTrap] Applying plate skin " + skinOrAll + " for " + target.getName() + ", required points: " + requiredPoints);
                        if (requiredPoints > 0 && this.pointsManager.getPoints(target.getUniqueId(), skinOrAll) < requiredPoints) {
                            String var15 = String.valueOf(ChatColor.RED);
                            sender.sendMessage(var15 + "У игрока недостаточно очков для скина '" + skinOrAll + "'.");
                            return true;
                        } else {
                            this.activeSkinsManager.setActivePlateSkin(target.getUniqueId(), skinOrAll);
                            if (requiredPoints > 0) {
                                this.pointsManager.removePoints(target.getUniqueId(), skinOrAll, requiredPoints);
                                this.plugin.getLogger().info("[VioTrap] Removed " + requiredPoints + " points for plate skin " + skinOrAll + " from " + target.getName());
                            } else {
                                this.plugin.getLogger().info("[VioTrap] No points required for plate skin " + skinOrAll);
                            }

                            String var13 = String.valueOf(ChatColor.GREEN);
                            sender.sendMessage(var13 + "Скин '" + skinOrAll + "' успешно применён для пласта игрока " + target.getName() + ".");
                            var13 = String.valueOf(ChatColor.GREEN);
                            target.sendMessage(var13 + "Вам применён скин '" + skinOrAll + "' для пласта.");
                            return true;
                        }
                    }
                } else {
                    int appliedCount = 0;

                    for(String skin : this.plugin.getPlateSkinNames()) {
                        int requiredPoints = this.plugin.getConfig().getInt("plate_skins." + skin + ".points", 0);
                        if (requiredPoints > 0 && this.pointsManager.getPoints(target.getUniqueId(), skin) < requiredPoints) {
                            this.plugin.getLogger().info("[VioTrap] Skipped plate skin " + skin + " for " + target.getName() + ": insufficient points (" + this.pointsManager.getPoints(target.getUniqueId(), skin) + "/" + requiredPoints + ")");
                        } else {
                            this.activeSkinsManager.setActivePlateSkin(target.getUniqueId(), skin);
                            if (requiredPoints > 0) {
                                this.pointsManager.removePoints(target.getUniqueId(), skin, requiredPoints);
                                this.plugin.getLogger().info("[VioTrap] Removed " + requiredPoints + " points for plate skin " + skin + " from " + target.getName());
                            }

                            ++appliedCount;
                        }
                    }

                    if (appliedCount > 0) {
                        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Применено " + appliedCount + " скинов пластов для игрока " + target.getName() + ".");
                        String var10001 = String.valueOf(ChatColor.GREEN);
                        target.sendMessage(var10001 + "Вам применено " + appliedCount + " скинов пластов.");
                    } else {
                        String var12 = String.valueOf(ChatColor.RED);
                        sender.sendMessage(var12 + "Не удалось применить ни один скин пластов для игрока " + target.getName() + ": недостаточно очков.");
                    }

                    return true;
                }
            }
        }
    }

}
