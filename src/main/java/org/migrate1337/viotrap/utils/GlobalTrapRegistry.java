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

    /**
     * Регистрирует использование блока регионом.
     * @param location Локация блока
     * @param regionId ID региона (трапки или пласта)
     * @param currentWorldData Данные блока, считанные прямо сейчас из мира (если это первая установка)
     * @return Данные, которые нужно сохранить как "оригинальные" для этого региона.
     */
    public synchronized TrapBlockData registerAndGetOriginal(Location location, String regionId, TrapBlockData currentWorldData) {


        if (originalBlockStates.containsKey(location)) {
            activeRegionsOnBlock.computeIfAbsent(location, k -> new HashSet<>()).add(regionId);
            return originalBlockStates.get(location);
        }

        originalBlockStates.put(location, currentWorldData);
        activeRegionsOnBlock.computeIfAbsent(location, k -> new HashSet<>()).add(regionId);

        return currentWorldData;
    }

    /**
     * Сообщает реестру, что регион перестает использовать блок.
     * @param location Локация блока
     * @param regionId ID региона
     * @return true, если это был последний регион и блок нужно физически восстановить. false, если блок еще занят.
     */
    public synchronized boolean unregister(Location location, String regionId) {
        Set<String> regions = activeRegionsOnBlock.get(location);
        if (regions == null) return true; // Странная ситуация, но разрешаем восстановление

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