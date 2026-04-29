package org.migrate1337.viotrap.editor;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.migrate1337.viotrap.VioTrap;

import java.util.*;

public class ParticleEditorManager {
    private final VioTrap plugin;
    private final Map<UUID, EditorSession> activeSessions = new HashMap<>();

    public ParticleEditorManager(VioTrap plugin) {
        this.plugin = plugin;
        startParticleTask();
    }

    public void startEditorSession(Player player, String patternName, String existingTemplate) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage("§cВы уже находитесь в режиме редактирования!");
            return;
        }

        Location originalLoc = player.getLocation();

        // Строгие координаты центра (без десятых долей)
        Location arenaCenter = new Location(originalLoc.getWorld(), originalLoc.getBlockX(), 200, originalLoc.getBlockZ());

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
        if (existingTemplate != null) {
            loadPatternIntoSession(session, existingTemplate);
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        player.getInventory().setItem(0, getBrushItem());
        player.getInventory().setItem(7, getSaveItem());
        player.getInventory().setItem(8, getCancelItem());
        player.getInventory().setItem(1, getPaletteItem());
        player.getInventory().setItem(2, getImportItem());
        buildCube(arenaCenter, Material.STONE);
        player.getInventory().setItem(3, getShapeItem(Material.SLIME_BALL, "Круг"));
        player.getInventory().setItem(4, getShapeItem(Material.BRICK, "Квадрат"));
        player.getInventory().setItem(5, getShapeItem(Material.ARROW, "Треугольник"));
        player.setAllowFlight(true);
        player.setFlying(true);

        // Отступаем чуть дальше (-5.5), так как куб стал больше
        Location tpLoc = arenaCenter.clone().add(0.5, 0, -5.5);
        tpLoc.setYaw(0);
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

                // Перебираем все цветные точки
                for (Map.Entry<Vector, String> entry : session.getColoredPoints().entrySet()) {
                    Vector v = entry.getKey();

                    // Тот самый фикс с тенями (Z-fighting)
                    // Стягиваем партиклы на 2% к центру, чтобы оторвать их от граней каменного куба
                    double scale = 1.02;
                    Location particleLoc = session.getArenaCenter().clone().add(v.getX() * scale, v.getY() * scale, v.getZ() * scale);

                    // Парсим цвет конкретной точки
                    String[] rgb = entry.getValue().split(",");
                    org.bukkit.Color color = org.bukkit.Color.fromRGB(
                            Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2])
                    );
                    org.bukkit.Particle.DustOptions dust = new org.bukkit.Particle.DustOptions(color, 0.8F);

                    player.spawnParticle(Particle.REDSTONE, particleLoc, 1, 0, 0, 0, 0, dust);
                }
            }
        }, 0L, 5L); // Обновляем чаще для редактора
    }
    private ItemStack getShapeItem(Material mat, String shapeName) {
        ItemStack item = new ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bФигура: " + shapeName);
        meta.setLore(java.util.Arrays.asList(
                "§7ПКМ - Нарисовать фигуру",
                "§7ЛКМ - Изменить размер"
        ));
        item.setItemMeta(meta);
        return item;
    }
    // --- ОБНОВЛЕННАЯ КИСТЬ (ТЕПЕРЬ ПЛОСКИЙ КРУГ, БЕЗ ЛИМИТОВ) ---
    public void handleBrushClick(Player player, Vector exactHitPos, org.bukkit.block.BlockFace face, boolean addPoint) {
        EditorSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        // Получаем позицию клика относительно центра арены
        Vector relativePos = exactHitPos.clone().subtract(session.getArenaCenter().toVector());

        // УДАЛЕНО: Ограничитель Math.abs(relativePos.getX()) > 3.5 ...

        int size = session.getBrushSize();
        // Максимальный размер кисти 15 будет давать радиус 1.4 блока
        double radius = (size - 1) * 0.1;

        // Определяем плоскость, на которую кликнул игрок
        boolean isFlat = (face == org.bukkit.block.BlockFace.UP || face == org.bukkit.block.BlockFace.DOWN);
        boolean isXPlane = (face == org.bukkit.block.BlockFace.EAST || face == org.bukkit.block.BlockFace.WEST);

        if (addPoint) {
            // Теперь это двойной цикл (2D), а не тройной (3D)
            for (double a = -radius; a <= radius; a += 0.2) {
                for (double b = -radius; b <= radius; b += 0.2) {
                    if (a*a + b*b <= radius*radius) {
                        // Подстраиваем оси в зависимости от стены или пола
                        Vector pt = isFlat ? new Vector(a, 0, b) :
                                (isXPlane ? new Vector(0, a, b) : new Vector(a, b, 0));

                        addPointRounded(session, relativePos.clone().add(pt));
                    }
                }
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
        } else {
            // Ластик оставляем объемным (сферой), чтобы он стирал всё вокруг курсора
            // Это гораздо удобнее, чем пытаться попасть точно в плоскость партикла
            double eraseRadius = Math.max(0.4, radius + 0.2);
            session.getColoredPoints().keySet().removeIf(pt -> pt.distance(relativePos) <= eraseRadius);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
        }
    }

    // --- ОБНОВЛЕННАЯ ГЕОМЕТРИЯ (С плавным масштабированием) ---
    public void handleShapeClick(Player player, Vector exactHitPos, org.bukkit.block.BlockFace face, String shapeType) {
        EditorSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        Vector relCenter = exactHitPos.clone().subtract(session.getArenaCenter().toVector());
        boolean isFlat = (face == org.bukkit.block.BlockFace.UP || face == org.bukkit.block.BlockFace.DOWN);
        boolean isXPlane = (face == org.bukkit.block.BlockFace.EAST || face == org.bukkit.block.BlockFace.WEST);

        if (shapeType.equals("Круг")) {
            // Делим на 3.0, чтобы при размере 15 радиус был ровно 5.0
            double r = session.getCircleRadius() / 3.0;
            // Шаг 0.05 вместо 0.1, чтобы большой круг был плотным
            for (double angle = 0; angle < Math.PI * 2; angle += 0.05) {
                double a = Math.cos(angle) * r;
                double b = Math.sin(angle) * r;
                Vector pt = isFlat ? relCenter.clone().add(new Vector(a, 0, b)) :
                        (isXPlane ? relCenter.clone().add(new Vector(0, a, b)) : relCenter.clone().add(new Vector(a, b, 0)));
                addPointRounded(session, pt);
            }
        }
        else if (shapeType.equals("Квадрат")) {
            double s = session.getSquareSize() / 3.0;
            // Шаг 0.1 вместо 0.2
            for (double i = -s; i <= s; i += 0.1) {
                addPointRounded(session, isFlat ? relCenter.clone().add(new Vector(i, 0, s)) : (isXPlane ? relCenter.clone().add(new Vector(0, i, s)) : relCenter.clone().add(new Vector(i, s, 0))));
                addPointRounded(session, isFlat ? relCenter.clone().add(new Vector(i, 0, -s)) : (isXPlane ? relCenter.clone().add(new Vector(0, i, -s)) : relCenter.clone().add(new Vector(i, -s, 0))));
                addPointRounded(session, isFlat ? relCenter.clone().add(new Vector(s, 0, i)) : (isXPlane ? relCenter.clone().add(new Vector(0, s, i)) : relCenter.clone().add(new Vector(s, i, 0))));
                addPointRounded(session, isFlat ? relCenter.clone().add(new Vector(-s, 0, i)) : (isXPlane ? relCenter.clone().add(new Vector(0, -s, i)) : relCenter.clone().add(new Vector(-s, i, 0))));
            }
        }
        else if (shapeType.equals("Треугольник")) {
            double s = session.getTriangleSize() / 3.0;
            // Шаг 0.02 вместо 0.05 (треугольник рисуется линиями)
            for (double t = 0; t <= 1; t += 0.02) {
                Vector v1 = new Vector(0, s, 0);
                Vector v2 = new Vector(s * 0.866, -s * 0.5, 0);
                Vector v3 = new Vector(-s * 0.866, -s * 0.5, 0);

                drawLine(session, relCenter, v1, v2, t, isFlat, isXPlane);
                drawLine(session, relCenter, v2, v3, t, isFlat, isXPlane);
                drawLine(session, relCenter, v3, v1, t, isFlat, isXPlane);
            }
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f);
    }

    // Вспомогательный метод для округления и добавления цвета
    private void addPointRounded(EditorSession session, Vector pt) {
        double rx = Math.round(pt.getX() * 10.0) / 10.0;
        double ry = Math.round(pt.getY() * 10.0) / 10.0;
        double rz = Math.round(pt.getZ() * 10.0) / 10.0;
        session.getColoredPoints().put(new Vector(rx, ry, rz), session.getCurrentBrushColor());
    }

    // Вспомогательный метод для линий треугольника
    private void drawLine(EditorSession session, Vector center, Vector a, Vector b, double t, boolean flat, boolean xPlane) {
        Vector pt = a.clone().add(b.clone().subtract(a).multiply(t));
        Vector rotated = flat ? new Vector(pt.getX(), 0, pt.getY()) : (xPlane ? new Vector(0, pt.getX(), pt.getY()) : pt);
        addPointRounded(session, center.clone().add(rotated));
    }

    public void stopEditorSession(Player player, boolean save) {
        EditorSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        if (save) {
            // Преобразуем векторы в строки для конфига
            java.util.List<String> serializedPoints = new java.util.ArrayList<>();
            for (Map.Entry<Vector, String> entry : session.getColoredPoints().entrySet()) {
                Vector v = entry.getKey();
                // Сохраняем в формате: 1.05,2.50,-0.05:255,0,0
                String pointStr = String.format(java.util.Locale.US, "%.2f,%.2f,%.2f:%s", v.getX(), v.getY(), v.getZ(), entry.getValue());
                serializedPoints.add(pointStr);
            }

            // Сохраняем в config.yml
            plugin.getConfig().set("custom_patterns." + session.getPatternName(), serializedPoints);
            plugin.saveConfig();

            player.sendMessage("§aШаблон партиклов '" + session.getPatternName() + "' успешно сохранен!");
        } else {
            player.sendMessage("§cСоздание шаблона отменено.");
        }

        player.getInventory().setContents(session.getSavedInventory());
        player.getInventory().setArmorContents(session.getSavedArmor());

        buildCube(session.getArenaCenter(), Material.AIR);

        player.setAllowFlight(session.isWasAllowFlight());
        player.setFlying(session.isWasFlying());

        player.teleport(session.getOriginalLocation());
    }
    public ItemStack getImportItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eИмпорт шаблона");
            meta.setLore(java.util.Arrays.asList("§7Нажмите ПКМ, чтобы скопировать", "§7старый рисунок в эту область."));
            item.setItemMeta(meta);
        }
        return item;
    }
    private void buildCube(Location center, Material material) {
        // Увеличиваем размер до 5x5x5
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    center.clone().add(x, y, z).getBlock().setType(material);
                }
            }
        }
    }
    public void loadPatternIntoSession(EditorSession session, String patternToLoad) {
        List<String> points = plugin.getConfig().getStringList("custom_patterns." + patternToLoad);
        if (points == null || points.isEmpty()) return;

        for (String point : points) {
            try {
                String[] data = point.split(":");
                String[] coords = data[0].split(",");
                Vector v = new Vector(
                        Double.parseDouble(coords[0]),
                        Double.parseDouble(coords[1]),
                        Double.parseDouble(coords[2])
                );
                String color = data.length > 1 ? data[1] : "0,255,0";

                session.getColoredPoints().put(v, color);
            } catch (Exception ignored) {}
        }
    }
    public boolean isEditing(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public ItemStack getBrushItem() {
        ItemStack item = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dВолшебная кисть");
        meta.setLore(Arrays.asList("§7ПКМ по блоку, чтобы добавить партикл", "§7ЛКМ по блоку, чтобы удалить"));
        item.setItemMeta(meta);
        return item;
    }
    public ItemStack getPaletteItem() {
        ItemStack item = new ItemStack(Material.PAINTING);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dПалитра цветов");
        meta.setLore(Arrays.asList("§7Нажмите ПКМ, чтобы выбрать", "§7цвет для вашей кисти."));
        item.setItemMeta(meta);
        return item;
    }
    public EditorSession getSession(Player player) { return activeSessions.get(player.getUniqueId()); }
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