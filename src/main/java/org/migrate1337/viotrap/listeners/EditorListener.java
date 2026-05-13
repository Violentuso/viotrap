package org.migrate1337.viotrap.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.editor.EditorSession;
import org.migrate1337.viotrap.gui.TemplateImportMenu;

public class EditorListener implements Listener {
    private final VioTrap plugin;

    public EditorListener(VioTrap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

         
        if (!plugin.getParticleEditorManager().isEditing(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        Action action = event.getAction();

        boolean isShift = player.isSneaking();

         
        org.bukkit.util.RayTraceResult result = player.rayTraceBlocks(6.0, org.bukkit.FluidCollisionMode.NEVER);
        Vector exactHitPos = (result != null && result.getHitPosition() != null) ? result.getHitPosition() : null;
        org.bukkit.block.BlockFace hitFace = (result != null) ? result.getHitBlockFace() : org.bukkit.block.BlockFace.UP;


        if (item.getType() == Material.NETHERITE_HOE) {
            event.setCancelled(true);

            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                if (isShift) {
                    plugin.getParticleEditorManager().getSession(player).cycleBrushSize();
                    player.sendMessage("§eРазмер кисти изменен на: §a" + plugin.getParticleEditorManager().getSession(player).getBrushSize());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                } else {
                    if (exactHitPos != null) plugin.getParticleEditorManager().handleBrushClick(player, exactHitPos, hitFace, false);
                }
            } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                if (exactHitPos != null) plugin.getParticleEditorManager().handleBrushClick(player, exactHitPos, hitFace, true);
            }
            return;
        }

         
        String shapeName = null;
        if (item.getType() == Material.SLIME_BALL) shapeName = "Круг";
        else if (item.getType() == Material.BRICK) shapeName = "Квадрат";
        else if (item.getType() == Material.ARROW) shapeName = "Треугольник";

        if (shapeName != null) {
            event.setCancelled(true);

            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                 
                EditorSession session = plugin.getParticleEditorManager().getSession(player);
                if (shapeName.equals("Круг")) { session.cycleCircleRadius(); player.sendMessage("§eРадиус круга: §a" + session.getCircleRadius()); }
                if (shapeName.equals("Квадрат")) { session.cycleSquareSize(); player.sendMessage("§eРазмер квадрата: §a" + session.getSquareSize()); }
                if (shapeName.equals("Треугольник")) { session.cycleTriangleSize(); player.sendMessage("§eРазмер треугольника: §a" + session.getTriangleSize()); }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);

            } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                 
                if (exactHitPos != null) {
                    plugin.getParticleEditorManager().handleShapeClick(player, exactHitPos, hitFace, shapeName);
                } else {
                    player.sendMessage("§cВы должны смотреть на блок, чтобы нарисовать фигуру!");
                }
            }
            return;
        }
        if (item.getType() == Material.PAINTING) {
            event.setCancelled(true);
            new org.migrate1337.viotrap.gui.ColorPaletteMenu(plugin).open(player);
            return;
        }
        if (item.getType() == Material.PAPER) {
            event.setCancelled(true);

            new TemplateImportMenu(plugin).open(player);
        }
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