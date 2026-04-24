package org.migrate1337.viotrap.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.migrate1337.viotrap.VioTrap;

public class EditorListener implements Listener {
    private final VioTrap plugin;

    public EditorListener(VioTrap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Если игрок не в редакторе - игнорируем
        if (!plugin.getParticleEditorManager().isEditing(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        Action action = event.getAction();

        // --- ОБРАБОТКА КИСТИ (ПАРТ 3) ---
        if (item.getType() == Material.STICK) {
            event.setCancelled(true); // Запрещаем ломать блоки палкой

            if (event.getClickedBlock() != null) {
                if (action == Action.RIGHT_CLICK_BLOCK) {
                    plugin.getParticleEditorManager().handleBrushClick(player, event.getClickedBlock(), true);
                } else if (action == Action.LEFT_CLICK_BLOCK) {
                    plugin.getParticleEditorManager().handleBrushClick(player, event.getClickedBlock(), false);
                }
            }
            return; // Прерываем дальнейшую проверку, если в руках кисть
        }

        // --- ОБРАБОТКА КРАСИТЕЛЕЙ ---
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (item.getType() == Material.LIME_DYE) {
                event.setCancelled(true);
                plugin.getParticleEditorManager().stopEditorSession(player, true);
            } else if (item.getType() == Material.RED_DYE) {
                event.setCancelled(true);
                plugin.getParticleEditorManager().stopEditorSession(player, false);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getParticleEditorManager().isEditing(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (plugin.getParticleEditorManager().isEditing(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getParticleEditorManager().isEditing(player)) {
            plugin.getParticleEditorManager().stopEditorSession(player, false);
        }
    }
}