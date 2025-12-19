package org.migrate1337.viotrap.data;

import java.util.UUID;
import org.bukkit.Location;

public class PlateData {
    private UUID playerId;
    private Location location;
    private String skin;
    private long endTime;

    public PlateData(UUID playerId, Location location, String skin, long endTime) {
        this.playerId = playerId;
        this.location = location.clone();
        this.skin = skin;
        this.endTime = endTime;
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
