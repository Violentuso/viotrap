package org.migrate1337.viotrap.listeners;

class DirectionInfo {
    final int offsetX;
    final int offsetY;
    final int offsetZ;
    final int angX;
    final int angY;
    final int angZ;
    final int pos1X;
    final int pos1Y;
    final int pos1Z;
    final int pos2X;
    final int pos2Y;
    final int pos2Z;
    final String schematicName;

    DirectionInfo(int offsetX, int offsetY, int offsetZ, int angX, int angY, int angZ, int pos1X, int pos1Y, int pos1Z, int pos2X, int pos2Y, int pos2Z, String schematicName) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.angX = angX;
        this.angY = angY;
        this.angZ = angZ;
        this.pos1X = pos1X;
        this.pos1Y = pos1Y;
        this.pos1Z = pos1Z;
        this.pos2X = pos2X;
        this.pos2Y = pos2Y;
        this.pos2Z = pos2Z;
        this.schematicName = schematicName;
    }
}