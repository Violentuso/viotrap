package org.migrate1337.viotrap.conditions;

import org.bukkit.Material;

public enum ConditionType {
    PERMISSION("Права", Material.NAME_TAG, "Игрок должен иметь permission"),
    BLOCK_BELOW("Блок снизу", Material.GRASS_BLOCK, "Игрок должен стоять на блоке"),
    IS_SNEAKING("На шифте", Material.LEATHER_BOOTS, "Игрок должен (не) сидеть (true/false)"),
    MIN_HEALTH("Мин. здоровье", Material.RED_DYE, "Здоровье игрока больше X"),
    GAMEMODE("Режим игры", Material.COMPASS, "Режим игры (SURVIVAL, CREATIVE...)"),
    ITEM_IN_OFFHAND("Предмет в левой руке", Material.SHIELD, "Предмет во второй руке"),
    IN_REGION("В регионе", Material.EMERALD_BLOCK, "Игрок находится в регионе (например, spawn)"),
    NOT_IN_REGION("Не в регионе", Material.REDSTONE_BLOCK, "Игрок не находится в регионе"),
    IS_FLYING("Летает", Material.FEATHER, "Игрок находится в полете"),
    NOT_FLYING("Не летает", Material.IRON_BOOTS, "Игрок не находится в полете"),
    HAS_EFFECT("Имеет эффект", Material.POTION, "Игрок имеет эффект (например, STRENGTH:1)"),
    NO_EFFECT("Нет эффекта", Material.GLASS_BOTTLE, "Игрок не имеет эффект"),
    IN_BIOME("В биоме", Material.OAK_SAPLING, "Игрок находится в биоме (например, PLAINS)"),
    NOT_IN_BIOME("Не в биоме", Material.DEAD_BUSH, "Игрок не находится в биоме"),
    IS_SWIMMING("Плывет", Material.WATER_BUCKET, "Игрок плывет в воде"),
    NOT_SWIMMING("Не плывет", Material.BUCKET, "Игрок не плывет в воде");

    private final String displayName;
    private final Material icon;
    private final String description;

    ConditionType(String displayName, Material icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }
}