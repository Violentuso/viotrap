package org.migrate1337.viotrap.utils;

import org.bukkit.Location;
import org.migrate1337.viotrap.data.TrapBlockData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GlobalTrapRegistry {
    private static final GlobalTrapRegistry instance = new GlobalTrapRegistry();

    private final Map<Location, TrapBlockData> originalBlockStates = new HashMap<>();

    private final Map<Location, Set<String>> activeRegionsOnBlock = new HashMap<>();

    public static GlobalTrapRegistry getInstance() {
        return instance;
    }

     
    public synchronized TrapBlockData registerAndGetOriginal(Location location, String regionId, TrapBlockData currentWorldData) {


        if (originalBlockStates.containsKey(location)) {
            activeRegionsOnBlock.computeIfAbsent(location, k -> new HashSet<>()).add(regionId);
            return originalBlockStates.get(location);
        }

        originalBlockStates.put(location, currentWorldData);
        activeRegionsOnBlock.computeIfAbsent(location, k -> new HashSet<>()).add(regionId);

        return currentWorldData;
    }

     
    public synchronized boolean unregister(Location location, String regionId) {
        Set<String> regions = activeRegionsOnBlock.get(location);
        if (regions == null) return true;  

        regions.remove(regionId);

        if (regions.isEmpty()) {

            activeRegionsOnBlock.remove(location);
            originalBlockStates.remove(location);
            return true;
        }

        return false;
    }

    public synchronized void clearAll() {
        originalBlockStates.clear();
        activeRegionsOnBlock.clear();
    }
}