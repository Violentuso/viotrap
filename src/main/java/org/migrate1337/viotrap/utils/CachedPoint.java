package org.migrate1337.viotrap.utils;

import org.bukkit.Color;

public class CachedPoint {
    public final double x, y, z;
    public final Color color;

    public CachedPoint(double x, double y, double z, Color color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }
}