package org.migrate1337.viotrap.actions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.migrate1337.viotrap.VioTrap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DenyItemUseCustomAction implements CustomAction, Listener {
    private static final Map<UUID, Set<Material>> trapDeniedItems = new HashMap<>();

    private final String target;
    private final Set<Material> items;

    public DenyItemUseCustomAction(String target, Set<Material> items) {
        this.target = target.toLowerCase();
        this.items = new HashSet<>(items);
    }

    // Вставьте этот метод в DenyItemUseCustomAction.java

    @Override
    public void execute(Player player, Player[] opponents, VioTrap plugin) {
        switch (target) {
            case "p":
            case "player":
                apply(player, plugin);
                break;
            case "o":
                for (Player opponent : opponents) {
                    apply(opponent, plugin);
                }
                break;
            case "rp":
                Player random = CustomActionFactory.getRandomPlayer(player, opponents);
                if (random != null) apply(random, plugin);
                break;
            default:
                plugin.getLogger().warning("[VioTrap] Некорректный target в DenyItemUseCustomAction: " + target);
        }
    }

    // И добавьте приватный метод apply, который выполняет основное действие
    private void apply(Player p, VioTrap plugin) {
        if (p == null || !p.isOnline()) return;

        Set<Material> playerDenied = trapDeniedItems.getOrDefault(p.getUniqueId(), new HashSet<>());

        // --- Логирование (для проверки срабатывания) ---
        plugin.getLogger().info("[DenyItemUse] Executing 'denyitemuse' for player " + p.getName() + ". Items to deny: " + items);

        playerDenied.addAll(items);
        trapDeniedItems.put(p.getUniqueId(), playerDenied);

        plugin.getLogger().info("[DenyItemUse] Player " + p.getName() + " now has " + playerDenied.size() + " denied items. Target: " + target);
    }

    // В DenyItemUseCustomAction.java
    public static void clearForPlayer(UUID uuid) {
        if (trapDeniedItems.containsKey(uuid)) {
            Bukkit.getLogger().info("[DenyItemUse] Clearing items for UUID: " + uuid + ". Denied count: " + trapDeniedItems.get(uuid).size());

            trapDeniedItems.remove(uuid);

            // Проверка удаления
            Bukkit.getLogger().info("[DenyItemUse] Clear successful. Contains key after removal: " + trapDeniedItems.containsKey(uuid));
        } else {
            Bukkit.getLogger().info("[DenyItemUse] Attempted to clear items for UUID: " + uuid + ", but player was not in map.");
        }
    }
    // Добавьте сброс анимации + удалите лишнюю проверку региона

    // Метод onConsume (для Золотого Яблока)
    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();
        Set<Material> denied = trapDeniedItems.get(player.getUniqueId());

        if (denied == null || denied.isEmpty()) return; // Если нет в списке, не блокируем

        Material itemType = e.getItem().getType();

        if (denied.contains(itemType)) {
            e.setCancelled(true);

            // !!! КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: СБРОС АНИМАЦИИ ЕДЫ !!!
            if (player.getInventory().getItemInMainHand().getType() == itemType) {
                player.getInventory().setItemInMainHand(player.getInventory().getItemInMainHand());
            } else if (player.getInventory().getItemInOffHand().getType() == itemType) {
                player.getInventory().setItemInOffHand(player.getInventory().getItemInOffHand());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (!e.hasItem() || e.getItem() == null) return;

        Material itemType = e.getItem().getType();

        if (itemType.isEdible() ||
                itemType == Material.MILK_BUCKET ||
                itemType.name().contains("POTION") ||
                itemType.name().contains("SOUP")) {
            return;
        }


        switch (itemType) {
            case ENDER_PEARL:
            case SNOWBALL:
            case EGG:
            case FIRE_CHARGE:
            case EXPERIENCE_BOTTLE:
            case SPLASH_POTION:
            case LINGERING_POTION:
            case TRIDENT:
            case FIREWORK_ROCKET:
                return;
            default:

        }


        Player player = e.getPlayer();
        Set<Material> denied = trapDeniedItems.get(player.getUniqueId());

        if (denied == null || denied.isEmpty()) {
            return;
        }

        if (denied.contains(itemType)) {
            e.setCancelled(true);
            player.sendMessage("§cНельзя использовать этот предмет в ловушке!");

            // Дополнительная отмена для надежности
            e.setUseItemInHand(Event.Result.DENY);
            e.setUseInteractedBlock(Event.Result.DENY);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) e.getEntity().getShooter();
        Set<Material> denied = trapDeniedItems.get(player.getUniqueId());

        if (denied == null || denied.isEmpty()) return;

        Material launchItem = null;


        if (e.getEntity() instanceof org.bukkit.entity.EnderPearl) {
            launchItem = Material.ENDER_PEARL;
        } else if (e.getEntity() instanceof org.bukkit.entity.Snowball) {
            launchItem = Material.SNOWBALL;
        } else if (e.getEntity() instanceof org.bukkit.entity.Egg) {
            launchItem = Material.EGG;
        }else if (e.getEntity() instanceof org.bukkit.entity.Trident) {
            launchItem = Material.TRIDENT;
        }  else if (e.getEntity() instanceof org.bukkit.entity.ThrownPotion) {

            if (e.getEntity().getType() == org.bukkit.entity.EntityType.SPLASH_POTION) {

                if (denied.contains(Material.SPLASH_POTION) || denied.contains(Material.LINGERING_POTION)) {
                    e.setCancelled(true);
                    player.sendMessage("§cНельзя использовать зелья в ловушке!");
                    return;
                }
            }
        }

        if (launchItem != null && denied.contains(launchItem)) {
            e.setCancelled(true);
            player.sendMessage("§cНельзя использовать этот предмет в ловушке!");
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        trapDeniedItems.remove(e.getPlayer().getUniqueId());
    }
}