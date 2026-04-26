package org.migrate1337.viotrap.editor;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditorSession {
    private final UUID playerId;
    private final String patternName;
    private final Location originalLocation;
    private final Location arenaCenter;
    private final ItemStack[] savedInventory;
    private final ItemStack[] savedArmor;
    private final boolean wasAllowFlight;
    private final boolean wasFlying;
    private int brushSize = 1;    // От 1 до 15
    private int circleRadius = 6; // От 1 до 15 (Физический размер от 0.3 до 5.0 блоков)
    private int squareSize = 6;   // От 1 до 15
    private int triangleSize = 6;
    // ФИКС: Теперь мы храним Вектор (позицию) -> RGB Цвет (строку)
    private final Map<Vector, String> coloredPoints = new HashMap<>();

    // Цвет кисти по умолчанию (Лаймовый)
    private String currentBrushColor = "0,255,0";

    public EditorSession(UUID playerId, String patternName, Location originalLocation, Location arenaCenter, ItemStack[] savedInventory, ItemStack[] savedArmor, boolean wasAllowFlight, boolean wasFlying) {
        this.playerId = playerId;
        this.patternName = patternName;
        this.originalLocation = originalLocation;
        this.arenaCenter = arenaCenter;
        this.savedInventory = savedInventory;
        this.savedArmor = savedArmor;
        this.wasAllowFlight = wasAllowFlight;
        this.wasFlying = wasFlying;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPatternName() { return patternName; }
    public Location getOriginalLocation() { return originalLocation; }
    public Location getArenaCenter() { return arenaCenter; }
    public ItemStack[] getSavedInventory() { return savedInventory; }
    public ItemStack[] getSavedArmor() { return savedArmor; }
    public boolean isWasAllowFlight() { return wasAllowFlight; }
    public boolean isWasFlying() { return wasFlying; }
    public int getBrushSize() { return brushSize; }
    public void cycleBrushSize() { brushSize = (brushSize % 15) + 1; }

    // Методы для круга
    public int getCircleRadius() { return circleRadius; }
    public void cycleCircleRadius() { circleRadius = (circleRadius % 15) + 1; }

    // Методы для квадрата
    public int getSquareSize() { return squareSize; }
    public void cycleSquareSize() { squareSize = (squareSize % 15) + 1; }

    // Методы для треугольника
    public int getTriangleSize() { return triangleSize; }
    public void cycleTriangleSize() { triangleSize = (triangleSize % 15) + 1; }
    public Map<Vector, String> getColoredPoints() { return coloredPoints; }
    public String getCurrentBrushColor() { return currentBrushColor; }
    public void setCurrentBrushColor(String rgb) { this.currentBrushColor = rgb; }
}