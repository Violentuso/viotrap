package org.migrate1337.viotrap.editor;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import java.util.HashSet;
import java.util.Set;
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

    // Сет для хранения локальных координат партиклов
    private final Set<Vector> particlePoints = new HashSet<>();

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

    public Set<Vector> getParticlePoints() { return particlePoints; }
}