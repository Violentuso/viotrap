package org.migrate1337.viotrap.editor;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.migrate1337.viotrap.VioTrap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParticleEditorManager {
    private final VioTrap plugin;
    private final Map<UUID, EditorSession> activeSessions = new HashMap<>();

    public ParticleEditorManager(VioTrap plugin) {
        this.plugin = plugin;
        startParticleTask();
    }

    // Запуск сессии
    public void startEditorSession(Player player, String patternName) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage("§cВы уже находитесь в режиме редактирования!");
            return;
        }

        Location originalLoc = player.getLocation();
        // ФИКС: Берем строго координаты целого блока, чтобы избавиться от десятых долей!
        Location arenaCenter = new Location(originalLoc.getWorld(), originalLoc.getBlockX(), 200, originalLoc.getBlockZ());

        // 1. Сохраняем текущее состояние игрока
        EditorSession session = new EditorSession(
                player.getUniqueId(),
                patternName,
                originalLoc,
                arenaCenter,
                player.getInventory().getContents().clone(),
                player.getInventory().getArmorContents().clone(),
                player.getAllowFlight(),
                player.isFlying()
        );
        activeSessions.put(player.getUniqueId(), session);

        // 2. Очищаем инвентарь
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // 3. Выдаем инструменты
        player.getInventory().setItem(0, getBrushItem());
        player.getInventory().setItem(7, getSaveItem());
        player.getInventory().setItem(8, getCancelItem());

        // 4. Генерируем куб 3х3х3 из камня
        buildCube(arenaCenter, Material.STONE);

        // 5. Включаем полет, чтобы удобно было рисовать
        player.setAllowFlight(true);
        player.setFlying(true);

        // 6. Телепортируем к арене (отступаем на 5 блоков по Z, чтобы игрок смотрел на куб)
        Location tpLoc = arenaCenter.clone().add(0, 0, -5);
        tpLoc.setYaw(0); // Смотрим прямо (на юг)
        tpLoc.setPitch(0);
        player.teleport(tpLoc);

        player.sendMessage("§aВы вошли в редактор партиклов!");
        player.sendMessage("§7Используйте кисть для рисования по пресету.");
    }
    private void startParticleTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (EditorSession session : activeSessions.values()) {
                Player player = Bukkit.getPlayer(session.getPlayerId());
                if (player == null || !player.isOnline()) continue;

                // Отображаем каждую точку
                for (Vector relPos : session.getParticlePoints()) {
                    // +0.5 чтобы партикл был ровно в центре блока
                    Location particleLoc = session.getArenaCenter().clone()
                            .add(relPos).add(0.5, 0.5, 0.5);

                    // Спавним красивую частицу (пока что стандартную)
                    player.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                }
            }
        }, 0L, 10L);
    }
    public void handleBrushClick(Player player, Block clickedBlock, boolean addPoint) {
        EditorSession session = activeSessions.get(player.getUniqueId());
        if (session == null || clickedBlock == null) return;

        Location center = session.getArenaCenter();

        // Проверяем, кликнул ли игрок по нашему кубу 3x3x3 (чтобы не рисовал за его пределами)
        if (Math.abs(clickedBlock.getX() - center.getBlockX()) > 1 ||
                Math.abs(clickedBlock.getY() - center.getBlockY()) > 1 ||
                Math.abs(clickedBlock.getZ() - center.getBlockZ()) > 1) {
            return;
        }

        // Вычисляем относительную координату (например, 1, 1, -1)
        Vector relativePos = clickedBlock.getLocation().toVector().subtract(center.toVector());

        if (addPoint) {
            if (session.getParticlePoints().add(relativePos)) {
                clickedBlock.setType(Material.LIME_STAINED_GLASS); // Красим блок в зеленый
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            }
        } else {
            if (session.getParticlePoints().remove(relativePos)) {
                clickedBlock.setType(Material.STONE); // Возвращаем камень
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            }
        }
    }
    // Завершение сессии (сохранение или отмена)
    public void stopEditorSession(Player player, boolean save) {
        EditorSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        if (save) {
            // TODO: В Part 4 здесь будет логика сохранения координат партиклов в конфиг
            player.sendMessage("§aШаблон партиклов '" + session.getPatternName() + "' успешно сохранен!");
        } else {
            player.sendMessage("§cСоздание шаблона отменено.");
        }

        // 1. Возвращаем инвентарь
        player.getInventory().setContents(session.getSavedInventory());
        player.getInventory().setArmorContents(session.getSavedArmor());

        // 2. Удаляем временный 3x3x3 куб (заменяем на воздух)
        buildCube(session.getArenaCenter(), Material.AIR);

        // 3. Возвращаем статус полета
        player.setAllowFlight(session.isWasAllowFlight());
        player.setFlying(session.isWasFlying());

        // 4. Возвращаем на исходную точку
        player.teleport(session.getOriginalLocation());
    }

    // Вспомогательный метод для строительства/удаления куба
    private void buildCube(Location center, Material material) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    center.clone().add(x, y, z).getBlock().setType(material);
                }
            }
        }
    }

    public boolean isEditing(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    // --- Предметы для редактора ---

    public ItemStack getBrushItem() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dВолшебная кисть");
        meta.setLore(Arrays.asList("§7ПКМ по блоку, чтобы добавить партикл", "§7ЛКМ по блоку, чтобы удалить"));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getSaveItem() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aСохранить шаблон");
        meta.setLore(Arrays.asList("§7Нажмите ПКМ, чтобы сохранить", "§7и выйти из редактора."));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getCancelItem() {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cОтменить и выйти");
        meta.setLore(Arrays.asList("§7Нажмите ПКМ, чтобы выйти", "§7без сохранения."));
        item.setItemMeta(meta);
        return item;
    }
}