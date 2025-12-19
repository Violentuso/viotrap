package org.migrate1337.viotrap.data;

import org.bukkit.Location;
import java.util.UUID;

public class TrapData {
    private final UUID playerId;
    private final Location location;
    private final String skin;
    private final long endTime;

    public TrapData(UUID playerId, Location location, String skin, long duration) {
        this.playerId = playerId;
        this.location = location.clone();
        this.skin = skin;
        this.endTime = System.currentTimeMillis() + duration * 1000L;
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public Location getLocation() {
        return this.location;
    }

    public String getSkin() {
        return this.skin;
    }

    public long getEndTime() {
        return this.endTime;
    }
}