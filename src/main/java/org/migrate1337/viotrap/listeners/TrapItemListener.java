package org.migrate1337.viotrap.listeners;

import com.github.sirblobman.combatlogx.api.object.TagReason;
import com.github.sirblobman.combatlogx.api.object.TagType;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.actions.CustomAction;
import org.migrate1337.viotrap.actions.CustomActionFactory;
import org.migrate1337.viotrap.actions.DenyItemUseCustomAction;
import org.migrate1337.viotrap.data.TrapBlockData;
import org.migrate1337.viotrap.data.TrapData;
import org.migrate1337.viotrap.items.PlateItem;
import org.migrate1337.viotrap.items.TrapItem;
import org.migrate1337.viotrap.utils.ActiveSkinsManager;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.GlobalTrapRegistry;
import org.migrate1337.viotrap.utils.PVPManagerHandle;

public class TrapItemListener implements Listener {
    private final VioTrap plugin;
    private final Map<UUID, Map<Location, TrapBlockData>> playerReplacedBlocks = new HashMap<>();
    private final Map<String, ProtectedCuboidRegion> activeTraps = new HashMap<>();
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;
    private final Set<UUID> playersInTrapRegions = new HashSet<>();
    private final Map<String, List<CustomAction>> skinActions = new HashMap<>();
    private final ActiveSkinsManager activeSkinsManager;
    private final Map<String, TrapData> activeTrapTimers = new HashMap<>();
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();

    public TrapItemListener(VioTrap plugin) {
        this.plugin = plugin;
        this.combatLogXHandler = new CombatLogXHandler();
        this.pvpManagerHandler = new PVPManagerHandle(plugin);
        this.activeSkinsManager = plugin.getActiveSkinsManager();

        for (String skin : plugin.getSkinNames()) {
            this.skinActions.put(skin, CustomActionFactory.loadActions(skin, plugin));
        }

        this.loadTrapsFromConfig();
        this.loadCooldownsFromConfig();
    }

    @EventHandler
    public void onPlayerUseTrap(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && TrapItem.isTrapItem(item)) {

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (!plugin.getConditionManager().checkConditions(player, "trap")) {
                    return;
                }
                Location location = player.getLocation();
                String skin = this.activeSkinsManager.getActiveTrapSkin(player.getUniqueId());
                if (skin == null || skin.isEmpty()) {
                    skin = "default";
                }
                if (!player.hasCooldown(item.getType()) && !this.isCooldownActive(player.getUniqueId(), skin)) {
                    String schematic = this.plugin.getSkinSchematic(skin);
                    if (schematic != null && (this.plugin.getConfig().contains("skins." + skin) || skin.equals("default"))) {
                        if (!this.isInBannedRegion(location, location.getWorld().getName()) && !this.hasBannedRegionFlags(location, location.getWorld().getName())) {
                            if(true) {
                                File schematicFile = new File("plugins/WorldEdit/schematics/" + schematic);
                                if (!schematicFile.exists()) {
                                    player.sendMessage("§cСхематика " + schematic + " не найдена!");
                                } else {
                                    try {
                                        try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                                            Clipboard clipboard = reader.read();
                                            BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                                            BlockVector3 max = clipboard.getRegion().getMaximumPoint();
                                            double sizeX = (double) (max.getBlockX() - min.getBlockX() + 1);
                                            double sizeY = (double) (max.getBlockY() - min.getBlockY() + 1);
                                            double sizeZ = (double) (max.getBlockZ() - min.getBlockZ() + 1);

                                            int originalAmount = item.getAmount();
                                            item.setAmount(originalAmount - 1);

                                            BukkitScheduler scheduler = Bukkit.getScheduler();
                                            scheduler.runTaskLater(this.plugin, player::updateInventory, 1L);
                                            int cooldownSeconds = (int) (skin.equals("default") ? (double) this.plugin.getTrapCooldown() : this.plugin.getConfig().getInt("skins." + skin + ".cooldown", (int) this.plugin.getTrapCooldown()));
                                            int cooldownTicks = cooldownSeconds * 20;
                                            player.setCooldown(item.getType(), cooldownTicks);
                                            this.saveCooldownToConfig(player.getUniqueId(), item.getType(), System.currentTimeMillis() + (long) (cooldownTicks * 50));
                                            if (this.plugin.getConfig().getString("trap.enable-pvp", "true").equals("true")) {
                                                enablePvpForPlayer(player);
                                            }

                                            String soundType = skin.equals("default") ? this.plugin.getTrapSoundType() : this.plugin.getConfig().getString("skins." + skin + ".sound.type", this.plugin.getTrapSoundType());
                                            float soundVolume = (float) (skin.equals("default") ? (double) this.plugin.getTrapSoundVolume() : this.plugin.getConfig().getDouble("skins." + skin + ".sound.volume", (double) this.plugin.getTrapSoundVolume()));
                                            float soundPitch = (float) (skin.equals("default") ? (double) this.plugin.getTrapSoundPitch() : this.plugin.getConfig().getDouble("skins." + skin + ".sound.pitch", (double) this.plugin.getTrapSoundPitch()));
                                            player.playSound(location, Sound.valueOf(soundType), soundVolume, soundPitch);
                                            this.applyEffects(player, "skins." + skin + ".effects.player");
                                            player.sendMessage(this.plugin.getConfig().getString("trap.messages.success_used"));
                                            List<CustomAction> actions = CustomActionFactory.loadActions(skin, plugin);
                                            this.skinActions.put(skin, actions);
                                            Player[] opponents = location.getWorld().getNearbyEntities(location, sizeX - (double) 3.0F, sizeY, sizeZ - (double) 3.0F, (entity) -> entity instanceof Player && !entity.equals(player)).stream().filter((entity) -> entity instanceof Player).toArray(Player[]::new);

                                            for (CustomAction action : actions) {
                                                action.execute(player, opponents, this.plugin);
                                            }

                                            for (Player opponent : opponents) {
                                                this.enablePvpForPlayer(opponent);
                                            }

                                            // Генерируем ID
                                            String trapId = player.getName() + "_trap_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();

                                            this.saveTrapToConfig(player, location, skin);
                                            TrapData trapData = new TrapData(player.getUniqueId(), location, skin, (long) this.plugin.getTrapDuration());
                                            this.activeTrapTimers.put(trapId, trapData);
                                            this.createTrapRegion(player, location, sizeX, sizeY, sizeZ, skin);

                                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
                                                BlockVector3 pastePosition = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

                                                // ! ВАЖНО: Сохраняем блоки через реестр, передавая trapId
                                                this.saveReplacedBlocks(player.getUniqueId(), location, clipboard, trapId);

                                                ClipboardHolder holder = new ClipboardHolder(clipboard);
                                                Operations.complete(holder.createPaste(editSession).to(pastePosition).build());

                                                if (!skin.equals("default")) {
                                                    int points = plugin.getSkinPointsManager().getPoints(player.getUniqueId(), skin);
                                                    if (points > 1) {
                                                        plugin.getSkinPointsManager().removePoints(player.getUniqueId(), skin, 1);
                                                    } else if (points == 1) {
                                                        plugin.getSkinPointsManager().removePoints(player.getUniqueId(), skin, 1);
                                                        this.activeSkinsManager.setActiveTrapSkin(player.getUniqueId(), "default");
                                                    }
                                                }
                                                String finalSkin = skin;
                                                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                                    TrapData data = this.activeTrapTimers.get(trapId);
                                                    if (data != null) {
                                                        // ! ВАЖНО: Восстановление через реестр
                                                        this.restoreBlocks(data.getPlayerId(), trapId);

                                                        this.removeTrapRegion(trapId, data.getLocation());
                                                        this.removeTrapFromFile(data.getLocation());
                                                        this.activeTrapTimers.remove(trapId);
                                                        String soundTypeEnded = finalSkin.equals("default") ? this.plugin.getTrapSoundTypeEnded() : this.plugin.getConfig().getString("skins." + finalSkin + ".sound.type-ended", this.plugin.getTrapSoundTypeEnded());
                                                        float soundVolumeEnded = (float) (finalSkin.equals("default") ? (double) this.plugin.getTrapSoundVolumeEnded() : this.plugin.getConfig().getDouble("skins." + finalSkin + ".sound.volume-ended", (double) this.plugin.getTrapSoundVolumeEnded()));
                                                        float soundPitchEnded = (float) (finalSkin.equals("default") ? (double) this.plugin.getTrapSoundPitchEnded() : this.plugin.getConfig().getDouble("skins." + finalSkin + ".sound.pitch-ended", (double) this.plugin.getTrapSoundPitchEnded()));

                                                        location.getWorld().playSound(location, Sound.valueOf(soundTypeEnded), soundVolumeEnded, soundPitchEnded);
                                                    }
                                                }, (int) (skin.equals("default") ? (double) this.plugin.getTrapDuration() * 20 : this.plugin.getConfig().getInt("skins." + skin + ".duration", (int) this.plugin.getTrapCooldown()) * 20));
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        player.sendMessage("§cНе удалось установить трапку!");
                                    }
                                }
                            }
                        } else {
                            player.sendMessage("§cВы не можете использовать данный предмет в этом регионе!");
                        }
                    } else {
                        player.sendMessage("§cСкин не найден в конфигурации.");
                    }
                } else {
                    if (this.isCooldownActive(player.getUniqueId(), skin)) {
                        player.sendMessage(plugin.getConfig().getString("trap.messages.cooldown_message", "§cПодождите перед использованием снова!"));
                    }
                }
            }
        }
    }

    private void removeTrapFromFile(Location location) {
        String worldName = location.getWorld().getName();
        String path = "traps." + worldName + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        this.plugin.getTrapsConfig().set(path, null);
        this.plugin.saveTrapsConfig();
    }

    private void replaceSkinnedTrapsWithNewSkin(Player player, String oldSkin, String newSkin) {
        Inventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getSize(); ++i) {
            ItemStack item = inventory.getItem(i);
            if (item != null && TrapItem.isTrapItem(item) && oldSkin.equals(TrapItem.getSkin(item))) {
                int amount = item.getAmount();
                inventory.setItem(i, TrapItem.getTrapItem(amount, newSkin));
            }
        }

        for (int i = 0; i < inventory.getSize(); ++i) {
            ItemStack item = inventory.getItem(i);
            if (item != null && PlateItem.getUniqueId(item) != null && oldSkin.equals(PlateItem.getSkin(item))) {
                int amount = item.getAmount();
                inventory.setItem(i, PlateItem.getPlateItem(amount, newSkin));
            }
        }

        Bukkit.getScheduler().runTaskLater(this.plugin, player::updateInventory, 1L);
    }

    public boolean isCooldownActive(UUID uuid, String skin) {
        Map<String, Long> skinsCooldowns = playerCooldowns.getOrDefault(uuid, new HashMap<>());
        Long endTime = skinsCooldowns.get(skin);
        if (endTime != null) {
            long remaining = (endTime - System.currentTimeMillis()) / 1000;
            return remaining > 0;
        }
        return false;
    }

    private void setCooldownActive(UUID uuid, String skin, long durationMs) {
        playerCooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(skin, System.currentTimeMillis() + durationMs);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final Location from = event.getFrom();
        final Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        final Location location = to;
        final boolean wasInTrapRegion = this.playersInTrapRegions.contains(player.getUniqueId());
        final boolean isNowInTrapRegion = this.isInAnyTrapRegion(location);

        if (!wasInTrapRegion && isNowInTrapRegion) {
            if (this.playersInTrapRegions.add(player.getUniqueId())) {
                this.enablePvpForPlayer(player);
                DenyItemUseCustomAction.applyForPlayer(player.getUniqueId());
            }
        } else if (wasInTrapRegion && !isNowInTrapRegion) {
            if (this.playersInTrapRegions.remove(player.getUniqueId())) {
                DenyItemUseCustomAction.clearForPlayer(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Material trapMaterial = Material.getMaterial(this.plugin.getConfig().getString("trap.item", "TRIPWIRE_HOOK"));
        if (trapMaterial != null && this.isCooldownActive(playerId, trapMaterial)) {
            long remainingTime = this.getCooldownRemainingTime(playerId, trapMaterial);
            if (remainingTime > 0L) {
                int remainingTicks = (int) (remainingTime / 50L);
                player.setCooldown(trapMaterial, remainingTicks);
            } else {
                this.removeCooldownFromConfig(playerId, trapMaterial);
            }
        }
    }

    public boolean isInAnyTrapRegion(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager == null) {
            return false;
        } else {
            BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(vector);

            for (ProtectedRegion region : applicableRegions.getRegions()) {
                if (region.getId().contains("_trap_")) {
                    return true;
                }
            }
            return false;
        }
    }

    private void enablePvpForPlayer(Player player) {
        if (this.combatLogXHandler.isCombatLogXEnabled()) {
            this.combatLogXHandler.tagPlayer(player, TagType.DAMAGE, TagReason.ATTACKER);
            player.sendMessage(this.plugin.getConfig().getString("trap.messages.pvp-enabled", "§cВы вошли в зону ловушки! Режим PVP активирован."));
        }

        if (this.pvpManagerHandler.isPvPManagerEnabled()) {
            this.pvpManagerHandler.tagPlayerForPvP(player, "trap");
            player.sendMessage(this.plugin.getConfig().getString("trap.messages.pvp-enabled", "§cВы вошли в зону ловушки! Режим PVP активирован."));
        }
    }

    private void saveCooldownToConfig(UUID playerId, Material material, long endTime) {
        String path = "cooldowns." + playerId.toString() + "." + material.toString();
        this.plugin.getTrapsConfig().set(path + ".endTime", endTime);
        this.plugin.saveTrapsConfig();
        long remainingTime = endTime - System.currentTimeMillis();
        if (remainingTime > 0L) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.removeCooldownFromConfig(playerId, material), remainingTime / 50L);
        }
    }

    private boolean isCooldownActive(UUID playerId, Material material) {
        String path = "cooldowns." + playerId.toString() + "." + material.toString();
        if (!this.plugin.getTrapsConfig().contains(path)) {
            return false;
        } else {
            long endTime = this.plugin.getTrapsConfig().getLong(path + ".endTime", 0L);
            long currentTime = System.currentTimeMillis();
            if (endTime <= currentTime) {
                this.removeCooldownFromConfig(playerId, material);
                return false;
            } else {
                return true;
            }
        }
    }

    private long getCooldownRemainingTime(UUID playerId, Material material) {
        String path = "cooldowns." + playerId.toString() + "." + material.toString();
        long endTime = this.plugin.getTrapsConfig().getLong(path + ".endTime", 0L);
        long currentTime = System.currentTimeMillis();
        return Math.max(0L, endTime - currentTime);
    }

    private void removeCooldownFromConfig(UUID playerId, Material material) {
        String path = "cooldowns." + playerId.toString() + "." + material.toString();
        this.plugin.getTrapsConfig().set(path, null);
        this.plugin.saveTrapsConfig();
    }

    private void loadCooldownsFromConfig() {
        if (this.plugin.getTrapsConfig().contains("cooldowns")) {
            ConfigurationSection cooldownsSection = this.plugin.getTrapsConfig().getConfigurationSection("cooldowns");

            for (String playerIdStr : cooldownsSection.getKeys(false)) {
                UUID playerId;
                try {
                    playerId = UUID.fromString(playerIdStr);
                } catch (IllegalArgumentException var15) {
                    continue;
                }

                ConfigurationSection playerSection = cooldownsSection.getConfigurationSection(playerIdStr);

                for (String materialName : playerSection.getKeys(false)) {
                    Material material = Material.getMaterial(materialName);
                    if (material != null) {
                        long endTime = playerSection.getLong(materialName + ".endTime", 0L);
                        long currentTime = System.currentTimeMillis();
                        long remainingTime = endTime - currentTime;
                        if (remainingTime <= 0L) {
                            this.removeCooldownFromConfig(playerId, material);
                        } else {
                            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.removeCooldownFromConfig(playerId, material), remainingTime / 50L);
                        }
                    }
                }
            }
        }
    }

    private void saveTrapToConfig(Player player, Location location, String skin) {
        String path = "traps." + location.getWorld().getName() + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        this.plugin.getTrapsConfig().set(path + ".player", player.getUniqueId().toString());
        this.plugin.getTrapsConfig().set(path + ".world", location.getWorld().getName());
        this.plugin.getTrapsConfig().set(path + ".x", location.getBlockX());
        this.plugin.getTrapsConfig().set(path + ".y", location.getBlockY());
        this.plugin.getTrapsConfig().set(path + ".z", location.getBlockZ());
        this.plugin.getTrapsConfig().set(path + ".skin", skin);
        this.plugin.getTrapsConfig().set(path + ".endTime", System.currentTimeMillis() + (long) (this.plugin.getTrapDuration() * 1000));
        this.plugin.saveTrapsConfig();
    }

    private void applyEffects(Player player, String configPath) {
        if (this.plugin.getConfig().contains(configPath)) {
            this.plugin.getConfig().getConfigurationSection(configPath).getKeys(false).forEach((effectName) -> {
                try {
                    int duration = this.plugin.getConfig().getInt(configPath + "." + effectName + ".duration") * 20;
                    int amplifier = this.plugin.getConfig().getInt(configPath + "." + effectName + ".amplifier");
                    player.addPotionEffect(new PotionEffect(PotionEffectType.getByName(effectName), duration, amplifier));
                } catch (Exception var6) {
                }
            });
        }
    }

    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager == null) {
            return false;
        } else {
            BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(vector);
            List<ProtectedRegion> regions = new ArrayList<>(applicableRegions.getRegions());
            if (regions.isEmpty()) {
                return false;
            } else {
                regions.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
                ProtectedRegion highestPriorityRegion = regions.get(0);

                boolean disabledAllRegions = this.plugin.getConfig().getBoolean("trap.disabled_all_regions", false);
                List<String> bannedRegions = this.plugin.getConfig().getStringList("trap.banned_regions");
                if (disabledAllRegions && !highestPriorityRegion.getId().equals("__default__")) {
                    return true;
                } else {
                    return bannedRegions.contains(highestPriorityRegion.getId());
                }
            }
        }
    }

    private boolean hasBannedRegionFlags(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager == null) {
            return false;
        } else {
            ConfigurationSection bannedFlagsSection = this.plugin.getConfig().getConfigurationSection("trap.banned_region_flags");
            if (bannedFlagsSection == null) {
                return false;
            } else {
                BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(vector);
                List<ProtectedRegion> regions = new ArrayList<>(applicableRegions.getRegions());
                if (regions.isEmpty()) {
                    return false;
                } else {
                    regions.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
                    ProtectedRegion highestPriorityRegion = regions.get(0);
                    if (highestPriorityRegion.getId().endsWith("_trap")) {
                        return false;
                    } else {
                        for (String flagName : bannedFlagsSection.getKeys(false)) {
                            StateFlag flag = (StateFlag) Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                            if (flag != null && highestPriorityRegion.getFlag(flag) != null) {
                                return true;
                            }
                        }
                        return false;
                    }
                }
            }
        }
    }

    private boolean isRegionNearby(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager != null) {
            BlockVector3 min = BlockVector3.at(location.getBlockX() - 3, location.getBlockY() - 3, location.getBlockZ() - 3);
            BlockVector3 max = BlockVector3.at(location.getBlockX() + 3, location.getBlockY() + 3, location.getBlockZ() + 3);
            ProtectedCuboidRegion checkRegion = new ProtectedCuboidRegion("checkRegion", min, max);
            return regionManager.getApplicableRegions(checkRegion).getRegions().stream().anyMatch((region) -> region.getId().contains("_trap") || region.getId().contains("plate_"));
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------
    // ВАЖНО: Новая логика сохранения через GlobalTrapRegistry
    // ------------------------------------------------------------------------------------------------
    private void saveReplacedBlocks(UUID playerId, Location center, Clipboard clipboard, String regionId) {
        Map<Location, TrapBlockData> replacedBlocks = this.playerReplacedBlocks.computeIfAbsent(playerId, k -> new HashMap<>());

        BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        BlockVector3 max = clipboard.getRegion().getMaximumPoint();

        double sizeX = (double) (max.getBlockX() - min.getBlockX() + 1);
        double sizeY = (double) (max.getBlockY() - min.getBlockY() + 1);
        double sizeZ = (double) (max.getBlockZ() - min.getBlockZ() + 1);

        int offsetX = (int) (-(sizeX / 2.0F));
        int offsetY = (int) (-(sizeY / 2.0F)) + 1;
        int offsetZ = (int) (-(sizeZ / 2.0F));

        if (sizeX == 5) offsetY = (int) (-(sizeY / 2.0F)) + 1;
        else if (sizeX == 7) offsetY = (int) (-(sizeY / 2.0F)) + 2;
        else if (sizeX == 9) offsetY = (int) (-(sizeY / 2.0F)) + 2;

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Location loc = new Location(center.getWorld(),
                            Math.floor(center.getX() + (double) (x - min.getBlockX() + offsetX)),
                            Math.floor(center.getY() + (double) (y - min.getBlockY() + offsetY)),
                            Math.floor(center.getZ() + (double) (z - min.getBlockZ() + offsetZ)));

                    // Блоки воздуха в схематике не должны трогать реальный мир, если схематика их не ставит.
                    // Но тут мы считываем ВЕСЬ регион, который покроет схематика.

                    Block block = loc.getBlock();
                    BlockState state = block.getState();

                    ItemStack[] contents = null;
                    String spawnedType = "UNKNOWN";
                    boolean isDoubleChest = false;
                    Location pairedLocation = null;

                    if (state instanceof CreatureSpawner) {
                        CreatureSpawner spawner = (CreatureSpawner) state;
                        spawnedType = spawner.getSpawnedType().name();
                    }

                    if (state instanceof Container) {
                        Container container = (Container) state;
                        Inventory inventory = container.getInventory();
                        InventoryHolder holder = inventory.getHolder();

                        if (holder instanceof DoubleChest) {
                            DoubleChest doubleChest = (DoubleChest) holder;
                            isDoubleChest = true;
                            Inventory combined = doubleChest.getInventory();
                            contents = combined.getContents() != null ? combined.getContents().clone() : new ItemStack[54];

                            if (doubleChest.getLeftSide() == holder) {
                                InventoryHolder right = doubleChest.getRightSide();
                                if (right instanceof Chest) {
                                    pairedLocation = ((Chest) right).getLocation();
                                }
                            }
                        } else {
                            contents = inventory.getContents() != null ? inventory.getContents().clone() : new ItemStack[27];
                        }
                    }

                    TrapBlockData currentWorldData = new TrapBlockData(
                            block.getType(),
                            block.getBlockData(),
                            contents,
                            state instanceof CreatureSpawner ? spawnedType : null
                    );
                    currentWorldData.setDoubleChest(isDoubleChest);
                    if (pairedLocation != null) {
                        currentWorldData.setPairedChestLocation(pairedLocation);
                    }

                    // --- МАГИЯ РЕЕСТРА ---
                    // Получаем данные, которые ДОЛЖНЫ быть сохранены (оригинал, если занято, или текущий, если свободно)
                    TrapBlockData finalDataToSave = GlobalTrapRegistry.getInstance().registerAndGetOriginal(loc, regionId, currentWorldData);

                    replacedBlocks.put(loc.clone(), finalDataToSave);
                }
            }
        }
    }

    public void restoreAllBlocks() {
        if (this.playerReplacedBlocks.isEmpty()) {
            this.loadTrapsFromConfig();
        }

        // Очистка всех трапок и реестра
        this.activeTraps.clear();
        this.playersInTrapRegions.clear();
        this.activeTrapTimers.clear();
        GlobalTrapRegistry.getInstance().clearAll();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.playersInTrapRegions.remove(playerId);
    }

    // ------------------------------------------------------------------------------------------------
    // ВАЖНО: Новая логика восстановления через GlobalTrapRegistry
    // ------------------------------------------------------------------------------------------------
    private void restoreBlocks(UUID playerId, String regionId) {
        Map<Location, TrapBlockData> replacedBlocks = this.playerReplacedBlocks.get(playerId);
        if (replacedBlocks == null || replacedBlocks.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Location, TrapBlockData>> iterator = replacedBlocks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Location, TrapBlockData> entry = iterator.next();
            Location location = entry.getKey();
            TrapBlockData blockData = entry.getValue();

            // Проверяем в реестре, можно ли восстанавливать
            boolean shouldRestore = GlobalTrapRegistry.getInstance().unregister(location, regionId);

            if (!shouldRestore) {
                // Блок используется кем-то другим. Просто удаляем запись у себя и идем дальше.
                this.removeTrapFromFile(location);
                iterator.remove();
                continue;
            }

            // Восстанавливаем физически
            Block block = location.getBlock();
            block.setType(blockData.getMaterial());
            block.setBlockData(blockData.getBlockData());

            BlockState state = block.getState();

            if (state instanceof CreatureSpawner) {
                CreatureSpawner spawner = (CreatureSpawner) state;
                if (blockData.getSpawnedType() != null && !"UNKNOWN".equals(blockData.getSpawnedType())) {
                    try {
                        spawner.setSpawnedType(EntityType.valueOf(blockData.getSpawnedType()));
                        spawner.update();
                    } catch (Exception e) {}
                }
            }

            if (state instanceof Container) {
                Container container = (Container) state;
                InventoryHolder holder = container.getInventory().getHolder();

                if (blockData.getContents() != null) {
                    if (holder instanceof DoubleChest) {
                        DoubleChest doubleChest = (DoubleChest) holder;
                        doubleChest.getInventory().setContents(blockData.getContents());
                    } else {
                        ItemStack[] oldContents = blockData.getContents();
                        ItemStack[] newContents = new ItemStack[27];
                        System.arraycopy(oldContents, 0, newContents, 0, Math.min(oldContents.length, 27));
                        container.getInventory().setContents(newContents);
                    }
                }
            }

            this.removeTrapFromFile(location);
            iterator.remove();
        }

        if (replacedBlocks.isEmpty()) {
            this.playerReplacedBlocks.remove(playerId);
        }
    }

    private void loadTrapsFromConfig() {
        if (this.plugin.getTrapsConfig().contains("traps")) {
            ConfigurationSection trapsSection = this.plugin.getTrapsConfig().getConfigurationSection("traps");

            for (String worldName : trapsSection.getKeys(false)) {
                ConfigurationSection worldSection = trapsSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    for (String trapKey : worldSection.getKeys(false)) {
                        ConfigurationSection trapSection = worldSection.getConfigurationSection(trapKey);
                        if (trapSection != null) {
                            UUID playerId = UUID.fromString(trapSection.getString("player"));
                            String world = trapSection.getString("world");
                            int x = trapSection.getInt("x");
                            int y = trapSection.getInt("y");
                            int z = trapSection.getInt("z");
                            String skin = trapSection.getString("skin", "default");
                            Location location = new Location(Bukkit.getWorld(world), (double) x, (double) y, (double) z);

                            // Воссоздаем ID для корректной работы реестра
                            // ВАЖНО: Тут мы не знаем имя игрока, если он оффлайн, но для уникальности trapKey обычно хватает
                            // Лучше использовать конструкцию как при создании, но у нас нет Player name.
                            // Используем playerId в качестве заменителя имени для ID при загрузке, или пытаемся получить имя.
                            String playerName = Bukkit.getOfflinePlayer(playerId).getName();
                            if(playerName == null) playerName = "Unknown";
                            String trapId = playerName + "_trap_" + x + "_" + y + "_" + z;

                            if (Bukkit.getWorld(world) != null) {
                                long currentTime = System.currentTimeMillis();
                                long endTime = trapSection.getLong("endTime", currentTime + (long) (this.plugin.getTrapDuration() * 1000));
                                long remainingTicks = (endTime - currentTime) / 50L;

                                if (remainingTicks <= 0L) {
                                    // Время вышло в оффлайне - нужно восстановить (но мы не можем без реестра,
                                    // так что просто чистим конфиг, блоки останутся как есть или восстановятся если данные есть)
                                    // В идеале тут нужен сложный механизм восстановления из файла, если бы он был.
                                    this.removeTrapRegion(trapId, location);
                                    this.removeTrapFromFile(location);
                                } else {
                                    // Трапка активна. Нам нужно "захватить" блоки в реестр, чтобы новые трапки знали о ней.
                                    String schematic = this.plugin.getSkinSchematic(skin);
                                    if (schematic != null) {
                                        try {
                                            File schematicFile = new File("plugins/WorldEdit/schematics/" + schematic);
                                            if (schematicFile.exists()) {
                                                try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                                                    Clipboard clipboard = reader.read();
                                                    // Сохраняем блоки в реестр. Т.к. это загрузка, мы считаем текущее состояние мира "занятым".
                                                    this.saveReplacedBlocks(playerId, location, clipboard, trapId);

                                                    // Пересоздаем регион
                                                    BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                                                    BlockVector3 max = clipboard.getRegion().getMaximumPoint();
                                                    double sizeX = (double)(max.getBlockX() - min.getBlockX() + 1);
                                                    double sizeY = (double)(max.getBlockY() - min.getBlockY() + 1);
                                                    double sizeZ = (double)(max.getBlockZ() - min.getBlockZ() + 1);
                                                    this.createTrapRegion((Player)null, location, sizeX, sizeY, sizeZ, skin);
                                                }
                                            }
                                        } catch (Exception e) {}
                                    }

                                    TrapData trapData = new TrapData(playerId, location, skin, remainingTicks / 20L);
                                    this.activeTrapTimers.put(trapId, trapData);

                                    Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                        TrapData data = this.activeTrapTimers.get(trapId);
                                        if (data != null) {
                                            this.restoreBlocks(data.getPlayerId(), trapId);
                                            this.removeTrapRegion(trapId, data.getLocation());
                                            this.removeTrapFromFile(data.getLocation());
                                            this.activeTrapTimers.remove(trapId);

                                            String soundTypeEnded = skin.equals("default") ? this.plugin.getTrapSoundTypeEnded() : this.plugin.getConfig().getString("skins." + skin + ".sound.type-ended", this.plugin.getTrapSoundTypeEnded());
                                            float soundVolumeEnded = (float) (skin.equals("default") ? (double) this.plugin.getTrapSoundVolumeEnded() : this.plugin.getConfig().getDouble("skins." + skin + ".sound.volume-ended", (double) this.plugin.getTrapSoundVolumeEnded()));
                                            float soundPitchEnded = (float) (skin.equals("default") ? (double) this.plugin.getTrapSoundPitchEnded() : this.plugin.getConfig().getDouble("skins." + skin + ".sound.pitch-ended", (double) this.plugin.getTrapSoundPitchEnded()));
                                            location.getWorld().playSound(location, Sound.valueOf(soundTypeEnded), soundVolumeEnded, soundPitchEnded);
                                        }
                                    }, remainingTicks);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void createTrapRegion(Player player, Location location, double sizeX, double sizeY, double sizeZ, String skin) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        String playerName = player != null ? player.getName() : "offline";
        // Пытаемся восстановить ID если игрок оффлайн, но это сложно.
        // Здесь используется логика создания. При загрузке выше мы пытались угадать ID.
        String trapId = playerName + "_trap_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();

        BlockVector3 min = BlockVector3.at((double) location.getBlockX() - sizeX / (double) 2.0F, (double) location.getBlockY() - sizeY / (double) 2.0F + (double) 2.0F, (double) location.getBlockZ() - sizeZ / (double) 2.0F);
        BlockVector3 max = BlockVector3.at((double) location.getBlockX() + sizeX / (double) 2.0F, (double) location.getBlockY() + sizeY / (double) 2.0F + (double) 1.0F, (double) location.getBlockZ() + sizeZ / (double) 2.0F);

        // Корректировка высоты (из вашего кода)
        if (sizeX == 5.0) {
            min = BlockVector3.at(location.getX() - sizeX / 2.0, location.getY() - sizeY / 2.0 + 2.0, location.getZ() - sizeZ / 2.0);
            max = BlockVector3.at(location.getX() + sizeX / 2.0, location.getY() + sizeY / 2.0 + 1.0, location.getZ() + sizeZ / 2.0);
        } else if (sizeX == 7.0) {
            min = BlockVector3.at(location.getX() - sizeX / 2.0, location.getY() - sizeY / 2.0 + 3.0, location.getZ() - sizeZ / 2.0);
            max = BlockVector3.at(location.getX() + sizeX / 2.0, location.getY() + sizeY / 2.0 + 2.0, location.getZ() + sizeZ / 2.0);
        } else if (sizeX == 9.0) {
            min = BlockVector3.at(location.getX() - sizeX / 2.0, location.getY() - sizeY / 2.0 + 4.0, location.getZ() - sizeZ / 2.0);
            max = BlockVector3.at(location.getX() + sizeX / 2.0, location.getY() + sizeY / 2.0 + 3.0, location.getZ() + sizeZ / 2.0);
        }

        if (regionManager != null) {
            ProtectedCuboidRegion region = new ProtectedCuboidRegion(trapId, min, max);
            ConfigurationSection flagsSection;
            if (skin != null && !skin.isEmpty() && !skin.equals("default") && this.plugin.getConfig().contains("skins." + skin + ".flags")) {
                flagsSection = this.plugin.getConfig().getConfigurationSection("skins." + skin + ".flags");
            } else {
                flagsSection = this.plugin.getConfig().getConfigurationSection("trap.flags");
            }

            if (flagsSection != null) {
                Map<String, String> flags = new HashMap<>();
                for (String key : flagsSection.getKeys(false)) {
                    flags.put(key, flagsSection.getString(key));
                }

                for (Map.Entry<String, String> flagEntry : flags.entrySet()) {
                    String flagName = flagEntry.getKey();
                    String stateValue = flagEntry.getValue().trim().toUpperCase();

                    try {
                        StateFlag flag = (StateFlag) Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                        if (flag != null) {
                            StateFlag.State state = State.valueOf(stateValue);
                            region.setFlag(flag, state);
                        }
                    } catch (Exception e) {}
                }
            }

            region.setPriority(52);
            regionManager.addRegion(region);
            this.activeTraps.put(trapId, region);
        }
    }

    public void removeTrapRegion(String trapId, Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager != null) {
            regionManager.removeRegion(trapId);
            this.activeTraps.remove(trapId);
        }
    }

    public void removeAllTraps() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                for (String regionName : regionManager.getRegions().keySet()) {
                    if (regionName.endsWith("_trap")) {
                        regionManager.removeRegion(regionName);
                    }
                }
            }
        }

        this.restoreAllBlocks();
    }

    public Map<String, List<CustomAction>> getSkinActions() {
        return this.skinActions;
    }
}