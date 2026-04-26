package org.migrate1337.viotrap.listeners;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.gui.ColorPaletteMenu;

public class ColorChatListener implements Listener {
    private final VioTrap plugin;

    public ColorChatListener(VioTrap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Проверяем, ждет ли плагин ввода цвета от этого игрока
        if (!ColorPaletteMenu.waitingForColor.contains(player.getUniqueId())) return;

        event.setCancelled(true); // Блокируем отправку сообщения в глобальный чат

        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("отмена") || message.equalsIgnoreCase("cancel")) {
            ColorPaletteMenu.waitingForColor.remove(player.getUniqueId());
            player.sendMessage("§cВвод цвета отменен.");
            return;
        }

        String rgbToSave = null;

        try {
            // Пробуем распарсить HEX код (например #FF00FF или FF00FF)
            if (message.startsWith("#") || (message.length() == 6 && !message.contains(","))) {
                String hex = message.startsWith("#") ? message : "#" + message;
                java.awt.Color javaColor = java.awt.Color.decode(hex);
                rgbToSave = javaColor.getRed() + "," + javaColor.getGreen() + "," + javaColor.getBlue();
            }
            // Пробуем распарсить RGB (поддерживает запятые и пробелы: "255, 0, 0" или "255 0 0")
            else {
                String[] parts = message.split("[, ]+");
                if (parts.length == 3) {
                    int r = Integer.parseInt(parts[0]);
                    int g = Integer.parseInt(parts[1]);
                    int b = Integer.parseInt(parts[2]);
                    if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
                        rgbToSave = r + "," + g + "," + b;
                    }
                }
            }
        } catch (Exception ignored) {
            // Если игрок написал ерунду, rgbToSave останется null
        }

        if (rgbToSave != null) {
            ColorPaletteMenu.waitingForColor.remove(player.getUniqueId());
            final String finalRgb = rgbToSave; // Переменная для потока

            // Выполняем сохранение в основном потоке, так как чат асинхронный
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.getParticleEditorManager().isEditing(player)) {
                    plugin.getParticleEditorManager().getSession(player).setCurrentBrushColor(finalRgb);
                    player.sendMessage("§aВы обмакнули кисть в новый цвет! §7(RGB: " + finalRgb + ")");
                } else {
                    plugin.getConfig().set("active_player_colors." + player.getUniqueId().toString(), finalRgb);
                    plugin.saveConfig();
                }
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            });
        } else {
            player.sendMessage("§cНеверный формат цвета! §7Используйте HEX (§f#FF0000§7) или RGB (§f255,0,0§7).");
        }
    }
}