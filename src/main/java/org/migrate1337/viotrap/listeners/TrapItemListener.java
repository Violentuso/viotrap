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

import org.bukkit.*;
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
import org.migrate1337.viotrap.utils.BlockDataSerializer;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.GlobalTrapRegistry;
import org.migrate1337.viotrap.utils.PVPManagerHandle;

public class TrapItemListener implements Listener {
    private final VioTrap plugin;
    private final Map<String, Map<Location, TrapBlockData>> regionReplacedBlocks = new HashMap<>();
    private final Map<String, ProtectedCuboidRegion> activeTraps = new HashMap<>();
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;
    private final Set<UUID> playersInTrapRegions = new HashSet<>();
    private final Map<String, List<CustomAction>> skinActions = new HashMap<>();
    private final ActiveSkinsManager activeSkinsManager;
    private final Map<String, TrapData> activeTrapTimers = new HashMap<>();
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();
    private boolean isSaveQueued = false;
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
    public File getSchematicFile(String fileName) {

        File worldEditFile = new File("plugins/WorldEdit/schematics/" + fileName);
        if (worldEditFile.exists()) return worldEditFile;

        if (Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
            File faweFile1 = new File("plugins/FastAsyncWorldEdit/schematics/" + fileName);
            if (faweFile1.exists()) return faweFile1;

        }
        return worldEditFile;
    }
    private void requestConfigSave() {
        if (!isSaveQueued) {
            isSaveQueued = true;
            // Откладываем сохранение на 40 тиков (2 секунды).
            // Все последующие изменения конфигурации в течение этого времени
            // будут сохранены за один проход, не вызывая лагов!
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.saveTrapsConfig();
                isSaveQueued = false;
            }, 40L);
        }
    }
    @EventHandler
    public void onPlayerUseTrap(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && TrapItem.isTrapItem(item)) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                return;
            }
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (!plugin.getConditionManager().checkConditions(player, "trap")) {
                    return;
                }
                Location location = player.getLocation();
                String skin = this.activeSkinsManager.getActiveTrapSkin(player.getUniqueId());
                if (skin == null || skin.isEmpty()) {
                    skin = "default";
                }
                Material itemType = item.getType();
                if (!player.hasCooldown(item.getType()) && !this.isCooldownActive(player.getUniqueId(), skin)) {
                    String schematic = this.plugin.getSkinSchematic(skin);
                    if (schematic != null && (this.plugin.getConfig().contains("skins." + skin) || skin.equals("default"))) {
                        if (!this.isInBannedRegion(location, location.getWorld().getName()) && !this.hasBannedRegionFlags(location, location.getWorld().getName())) {
                            if(true) {
                                File schematicFile = getSchematicFile(schematic);
                                if (!schematicFile.exists()) {
                                    player.sendMessage("§cСхематика " + schematic + " не найдена!");
                                } else {
                                    try {
                                        try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                                            Clipboard clipboard = reader.read();
                                            BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                                            BlockVector3 max = clipboard.getRegion().getMaximumPoint();
                                            int sizeX = max.getBlockX() - min.getBlockX() + 1;
                                            int sizeY = max.getBlockY() - min.getBlockY() + 1;
                                            int sizeZ = max.getBlockZ() - min.getBlockZ() + 1;

                                            int cooldownSeconds = (int) (skin.equals("default") ? (double) this.plugin.getTrapCooldown() : this.plugin.getConfig().getInt("skins." + skin + ".cooldown", (int) this.plugin.getTrapCooldown()));
                                            int cooldownTicks = cooldownSeconds * 20;
                                            player.setCooldown(itemType, cooldownTicks);

                                            int originalAmount = item.getAmount();
                                            item.setAmount(originalAmount - 1);


                                            BukkitScheduler scheduler = Bukkit.getScheduler();
                                            scheduler.runTaskLater(this.plugin, player::updateInventory, 1L);
                                            this.saveCooldownToConfig(player.getUniqueId(), itemType, System.currentTimeMillis() + (long) (cooldownTicks * 50));
                                            if (this.plugin.getConfig().getBoolean("trap.enable-pvp")) {
                                                enablePvpForPlayer(player);
                                            }

                                            String soundType = skin.equals("default") ? this.plugin.getTrapSoundType() : this.plugin.getConfig().getString("skins." + skin + ".sound.type", this.plugin.getTrapSoundType());
                                            float soundVolume = (float) (skin.equals("default") ? (double) this.plugin.getTrapSoundVolume() : this.plugin.getConfig().getDouble("skins." + skin + ".sound.volume", (double) this.plugin.getTrapSoundVolume()));
                                            float soundPitch = (float) (skin.equals("default") ? (double) this.plugin.getTrapSoundPitch() : this.plugin.getConfig().getDouble("skins." + skin + ".sound.pitch", (double) this.plugin.getTrapSoundPitch()));
                                            this.applyEffects(player, "skins." + skin + ".effects.player");
                                            player.sendMessage(this.plugin.getConfig().getString("trap.messages.success_used"));
                                            Player[] opponents = location.getWorld().getNearbyEntities(location, (double) (sizeX - 3), (double) sizeY, (double) (sizeZ - 3), (entity) -> entity instanceof Player && !entity.equals(player)).stream().filter((entity) -> entity instanceof Player).toArray(Player[]::new);

                                            for (Player opponent : opponents) {
                                                if (this.plugin.getConfig().getBoolean("trap.enable-pvp")) {
                                                    enablePvpForPlayer(player);
                                                }
                                            }

                                            String trapId = player.getName() + "_trap_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();

                                            TrapData trapData = new TrapData(player.getUniqueId(), location, skin, (long) this.plugin.getTrapDuration());
                                            this.activeTrapTimers.put(trapId, trapData);
                                            BlockVector3 origin = clipboard.getOrigin();
                                            int minRelX = min.getBlockX() - origin.getBlockX();
                                            int maxRelX = max.getBlockX() - origin.getBlockX();
                                            int minRelY = min.getBlockY() - origin.getBlockY();
                                            int maxRelY = max.getBlockY() - origin.getBlockY();
                                            int minRelZ = min.getBlockZ() - origin.getBlockZ();
                                            int maxRelZ = max.getBlockZ() - origin.getBlockZ();
                                            this.createTrapRegion(player, location, minRelX, maxRelX, minRelY, maxRelY, minRelZ, maxRelZ, skin);

                                            Map<org.bukkit.Location, org.bukkit.block.data.BlockData> voidBlocks = this.captureStructureVoidBlocks(location, clipboard);

                                            this.saveReplacedBlocks(player.getUniqueId(), location, clipboard, trapId);
                                            this.saveTrapToConfig(player, location, skin, trapId);

                                            final String finalSkin = skin;
                                            final Player finalPlayer = player;
                                            final Location finalLocation = location;
                                            final Player[] finalOpponents = opponents;
                                            final String finalSoundType = soundType;
                                            final float finalSoundVolume = soundVolume;
                                            final float finalSoundPitch = soundPitch;
                                            final String finalTrapId = trapId;
                                            final String finalTrapIdForTimer = trapId;
                                            final String finalSkinForTimer = skin;
                                            final Location finalLocationForTimer = location;

                                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
                                                BlockVector3 pastePosition = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

                                                plugin.getLogger().info("[VioTrap][DEBUG] === PASTE START === pastePos=" + pastePosition);

                                                ClipboardHolder holder = new ClipboardHolder(clipboard);
                                                plugin.getLogger().info("[VioTrap][DEBUG] Calling Operations.complete (paste)...");
                                                Operations.complete(holder.createPaste(editSession).to(pastePosition).build());
                                                plugin.getLogger().info("[VioTrap][DEBUG] Paste done.");
                                            }
                                            plugin.getLogger().info("[VioTrap][DEBUG] EditSession closed. Restoring " + voidBlocks.size() + " void block(s)...");
                                            this.restoreStructureVoidBlocks(voidBlocks);
                                            plugin.getLogger().info("[VioTrap][DEBUG] === PASTE END ===");

                                            if (!skin.equals("default")) {
                                                int points = plugin.getSkinPointsManager().getPoints(player.getUniqueId(), skin);
                                                if (points > 1) {
                                                    plugin.getSkinPointsManager().removePoints(player.getUniqueId(), skin, 1);
                                                } else if (points == 1) {
                                                    plugin.getSkinPointsManager().removePoints(player.getUniqueId(), skin, 1);
                                                    this.activeSkinsManager.setActiveTrapSkin(player.getUniqueId(), "default");
                                                }
                                            }
                                            List<CustomAction> actions = CustomActionFactory.loadActions(skin, plugin);
                                            this.skinActions.put(skin, actions);

                                            location.getWorld().playSound(location, Sound.valueOf(soundType), soundVolume, soundPitch);
                                            for (CustomAction action : actions) {
                                                action.execute(player, opponents, this.plugin);
                                            }
                                            this.startTrapParticleTask(player.getUniqueId(), location, trapId, plugin.getTrapDuration() * 20);
                                            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                                TrapData data = this.activeTrapTimers.get(trapId);
                                                if (data != null) {
                                                    this.restoreBlocks(data.getPlayerId(), finalTrapId);

                                                    this.removeTrapRegion(finalTrapId, data.getLocation());
                                                    this.removeTrapFromFile(data.getLocation());
                                                    this.activeTrapTimers.remove(finalTrapId);
                                                    String soundTypeEnded = finalSkin.equals("default") ? this.plugin.getTrapSoundTypeEnded() : this.plugin.getConfig().getString("skins." + finalSkin + ".sound.type-ended", this.plugin.getTrapSoundTypeEnded());
                                                    float soundVolumeEnded = (float) (finalSkin.equals("default") ? (double) this.plugin.getTrapSoundVolumeEnded() : this.plugin.getConfig().getDouble("skins." + finalSkin + ".sound.volume-ended", (double) this.plugin.getTrapSoundVolumeEnded()));
                                                    float soundPitchEnded = (float) (finalSkin.equals("default") ? (double) this.plugin.getTrapSoundPitchEnded() : this.plugin.getConfig().getDouble("skins." + finalSkin + ".sound.pitch-ended", (double) this.plugin.getTrapSoundPitchEnded()));

                                                    finalLocation.getWorld().playSound(finalLocation, Sound.valueOf(soundTypeEnded), soundVolumeEnded, soundPitchEnded);
                                                }
                                            }, (int) (finalSkin.equals("default") ? (double) this.plugin.getTrapDuration() * 20 : this.plugin.getConfig().getInt("skins." + finalSkin + ".duration", (int) this.plugin.getTrapCooldown()) * 20));
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
        this.requestConfigSave();
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
                if (this.plugin.getConfig().getBoolean("trap.enable-pvp")) {
                    enablePvpForPlayer(player);
                }
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
        this.requestConfigSave();
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
        this.requestConfigSave();
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
    private void startTrapParticleTask(UUID ownerId, Location center, String trapId, long durationTicks) {
        new org.bukkit.scheduler.BukkitRunnable() {
            long ticksElapsed = 0;
            // Создаем локацию ОДИН раз
            final Location baseLoc = new Location(center.getWorld(), center.getBlockX(), center.getBlockY(), center.getBlockZ());
            boolean hasPlayersNearby = true;

            @Override
            public void run() {
                if (ticksElapsed >= durationTicks || !activeTraps.containsKey(trapId)) {
                    this.cancel();
                    return;
                }

                // ==========================================
                // ОПТИМИЗАЦИЯ 1: Culling (Проверка дистанции)
                // Раз в 20 тиков (1 сек) проверяем, есть ли игроки в радиусе 35 блоков (1225^2).
                // ==========================================
                if (ticksElapsed % 20 == 0) {
                    hasPlayersNearby = false;
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (p.getWorld().equals(baseLoc.getWorld()) && p.getLocation().distanceSquared(baseLoc) < 1225) {
                            hasPlayersNearby = true;
                            break;
                        }
                    }
                }

                if (!hasPlayersNearby) {
                    ticksElapsed++;
                    return; // Игроков нет - просто скипаем отрисовку, экономя 100% ресурсов!
                }

                // ==========================================
                // ОПТИМИЗАЦИЯ 2: RAM Cache вместо config.yml
                // ==========================================
                String selectedEffect = plugin.getParticleCacheManager().getPlayerEffect(ownerId);

                if (selectedEffect != null) {
                    List<String> frames = plugin.getParticleCacheManager().getAnimation(selectedEffect);

                    if (frames != null && !frames.isEmpty()) {
                        int frameSpeed = 2;
                        if (ticksElapsed % frameSpeed == 0) {
                            int currentFrameIndex = (int) ((ticksElapsed / frameSpeed) % frames.size());
                            String patternToDisplay = frames.get(currentFrameIndex);
                            drawPattern(patternToDisplay, baseLoc);
                        }
                    } else {
                        if (ticksElapsed % 10 == 0) {
                            drawPattern(selectedEffect, baseLoc);
                        }
                    }
                }
                ticksElapsed++;
            }

            private void drawPattern(String patternName, Location baseLoc) {
                // Берем готовый список точек из памяти (Моментально)
                List<org.migrate1337.viotrap.utils.CachedPoint> points = plugin.getParticleCacheManager().getPattern(patternName);
                if (points == null || points.isEmpty()) return;

                // ==========================================
                // ОПТИМИЗАЦИЯ 3: Decimation (Лимит плотности)
                // Если в шаблоне больше 400 точек, рисуем каждую вторую.
                // ==========================================
                int step = points.size() > 400 ? 2 : 1;

                for (int i = 0; i < points.size(); i += step) {
                    org.migrate1337.viotrap.utils.CachedPoint pt = points.get(i);
                    org.bukkit.Particle.DustOptions dustOptions = new org.bukkit.Particle.DustOptions(pt.color, 0.6F);

                    // МАТЕМАТИЧЕСКАЯ МАГИЯ: Уменьшаем разлет партиклов на 2%
                    // Это "втянет" их из стен, пола и потолка в воздушное пространство ловушки
                    double scale = 1.02;
                    double ox = pt.x * scale;
                    double oy = pt.y * scale;
                    double oz = pt.z * scale;

                    // ==========================================
                    // ОПТИМИЗАЦИЯ 4: Object Pooling (Сборка мусора)
                    // ==========================================

                    // Используем отмасштабированные координаты (ox, oy, oz)
                    // И не забываем твой + 1 по высоте!
                    baseLoc.add(ox, oy + 1, oz);
                    baseLoc.getWorld().spawnParticle(org.bukkit.Particle.REDSTONE, baseLoc, 1, 0, 0, 0, 0, dustOptions);
                    baseLoc.subtract(ox, oy + 1, oz);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    private void saveTrapToConfig(Player player, Location location, String skin, String trapId) {
        String path = "traps." + location.getWorld().getName() + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        this.plugin.getTrapsConfig().set(path + ".player", player.getUniqueId().toString());
        this.plugin.getTrapsConfig().set(path + ".world", location.getWorld().getName());
        this.plugin.getTrapsConfig().set(path + ".x", location.getBlockX());
        this.plugin.getTrapsConfig().set(path + ".y", location.getBlockY());
        this.plugin.getTrapsConfig().set(path + ".z", location.getBlockZ());
        this.plugin.getTrapsConfig().set(path + ".skin", skin);
        this.plugin.getTrapsConfig().set(path + ".endTime", System.currentTimeMillis() + (long) (this.plugin.getTrapDuration() * 1000));

        Map<Location, TrapBlockData> blocks = this.regionReplacedBlocks.get(trapId);
        if (blocks != null && !blocks.isEmpty()) {
            ConfigurationSection section = this.plugin.getTrapsConfig().getConfigurationSection(path);
            if (section != null) {
                BlockDataSerializer.saveBlocksToConfig(section, "blocks", blocks);
            }
        }

        this.requestConfigSave();
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



    private void saveReplacedBlocks(UUID playerId, Location center, Clipboard clipboard, String regionId) {
        Map<Location, TrapBlockData> replacedBlocks = this.regionReplacedBlocks.computeIfAbsent(regionId, k -> new HashMap<>());

        BlockVector3 origin = clipboard.getOrigin();
        BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        BlockVector3 max = clipboard.getRegion().getMaximumPoint();

        Set<Location> processedDoubleChestPairs = new HashSet<>();

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Location loc = new Location(center.getWorld(),
                            (double) (center.getBlockX() + (x - origin.getBlockX())),
                            (double) (center.getBlockY() + (y - origin.getBlockY())),
                            (double) (center.getBlockZ() + (z - origin.getBlockZ())));

                    Block block = loc.getBlock();
                    BlockState state = block.getState();

                    ItemStack[] contents = null;
                    String spawnedType = null;
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

                            InventoryHolder leftSide = doubleChest.getLeftSide();
                            InventoryHolder rightSide = doubleChest.getRightSide();
                            Location leftLoc = (leftSide instanceof Chest) ? ((Chest) leftSide).getLocation() : null;
                            Location rightLoc = (rightSide instanceof Chest) ? ((Chest) rightSide).getLocation() : null;

                            if (leftLoc != null && leftLoc.getBlockX() == loc.getBlockX()
                                    && leftLoc.getBlockY() == loc.getBlockY()
                                    && leftLoc.getBlockZ() == loc.getBlockZ()) {
                                pairedLocation = rightLoc;
                            } else {
                                pairedLocation = leftLoc;
                            }

                            if (!processedDoubleChestPairs.contains(loc)) {
                                Inventory combined = doubleChest.getInventory();
                                contents = combined.getContents() != null ? combined.getContents().clone() : new ItemStack[54];
                                if (pairedLocation != null) {
                                    processedDoubleChestPairs.add(pairedLocation);
                                }
                            }

                            doubleChest.getInventory().clear();
                        } else {
                            contents = container.getSnapshotInventory().getContents().clone();
                            container.getInventory().clear();
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

                    TrapBlockData finalDataToSave = GlobalTrapRegistry.getInstance().registerAndGetOriginal(loc, regionId, currentWorldData);

                    replacedBlocks.put(loc.clone(), finalDataToSave);
                }
            }
        }
    }

    public void restoreAllBlocks() {
        if (this.regionReplacedBlocks.isEmpty()) {
            this.loadTrapsFromConfig();
        }

        for (Map.Entry<String, TrapData> entry : new HashMap<>(this.activeTrapTimers).entrySet()) {
            String trapId = entry.getKey();
            TrapData data = entry.getValue();
            this.restoreBlocks(data.getPlayerId(), trapId);
            this.removeTrapRegion(trapId, data.getLocation());
            this.removeTrapFromFile(data.getLocation());
        }

        this.activeTraps.clear();
        this.playersInTrapRegions.clear();
        this.activeTrapTimers.clear();
        GlobalTrapRegistry.getInstance().clearAll();
    }

    public void cleanupOnDisable() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                List<String> toRemove = new ArrayList<>();
                for (String regionName : regionManager.getRegions().keySet()) {
                    if (regionName.contains("_trap_")) {
                        toRemove.add(regionName);
                    }
                }
                for (String regionName : toRemove) {
                    regionManager.removeRegion(regionName);
                }
            }
        }

        this.activeTraps.clear();
        this.playersInTrapRegions.clear();
        this.activeTrapTimers.clear();
        this.regionReplacedBlocks.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.playersInTrapRegions.remove(playerId);
    }



    private void restoreBlocks(UUID playerId, String regionId) {
        Map<Location, TrapBlockData> replacedBlocks = this.regionReplacedBlocks.get(regionId);
        if (replacedBlocks == null || replacedBlocks.isEmpty()) {
            return;
        }

        List<Map.Entry<Location, TrapBlockData>> toRestore = new ArrayList<>();

        // Больше не используем iterator.remove() внутри цикла
        for (Map.Entry<Location, TrapBlockData> entry : replacedBlocks.entrySet()) {
            Location location = entry.getKey();
            boolean shouldRestore = GlobalTrapRegistry.getInstance().unregister(location, regionId);

            if (shouldRestore) {
                toRestore.add(entry);
            }
        }

        // Восстанавливаем физические блоки
        for (Map.Entry<Location, TrapBlockData> entry : toRestore) {
            Location location = entry.getKey();
            TrapBlockData blockData = entry.getValue();

            Block block = location.getBlock();
            block.setType(blockData.getMaterial());
            block.setBlockData(blockData.getBlockData());

            BlockState state = block.getState();
            if (state instanceof CreatureSpawner) {
                if (blockData.getSpawnedType() != null) {
                    try {
                        ((CreatureSpawner) state).setSpawnedType(EntityType.valueOf(blockData.getSpawnedType()));
                        state.update(true, false);
                    } catch (Exception e) {}
                }
            }
        }

        // Восстанавливаем инвентари
        for (Map.Entry<Location, TrapBlockData> entry : toRestore) {
            TrapBlockData blockData = entry.getValue();
            if (blockData.getContents() == null) continue;

            Block block = entry.getKey().getBlock();
            BlockState freshState = block.getState();

            if (freshState instanceof Container) {
                Container container = (Container) freshState;
                Inventory inv = container.getInventory();
                ItemStack[] contents = blockData.getContents();

                if (contents.length != inv.getSize()) {
                    ItemStack[] adjusted = new ItemStack[inv.getSize()];
                    System.arraycopy(contents, 0, adjusted, 0, Math.min(contents.length, inv.getSize()));
                    inv.setContents(adjusted);
                } else {
                    inv.setContents(contents);
                }
            }
        }

        this.regionReplacedBlocks.remove(regionId);
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
                            UUID playerId;
                            try {
                                playerId = UUID.fromString(trapSection.getString("player"));
                            } catch (Exception e) {
                                continue;
                            }
                            String world = trapSection.getString("world");
                            int x = trapSection.getInt("x");
                            int y = trapSection.getInt("y");
                            int z = trapSection.getInt("z");
                            String skin = trapSection.getString("skin", "default");

                            if (Bukkit.getWorld(world) == null) continue;
                            Location location = new Location(Bukkit.getWorld(world), (double) x, (double) y, (double) z);

                            String playerName = Bukkit.getOfflinePlayer(playerId).getName();
                            if (playerName == null) playerName = "Unknown";
                            String trapId = playerName + "_trap_" + x + "_" + y + "_" + z;

                            long currentTime = System.currentTimeMillis();
                            long endTime = trapSection.getLong("endTime", currentTime + (long) (this.plugin.getTrapDuration() * 1000));
                            long remainingTicks = (endTime - currentTime) / 50L;

                            Map<Location, TrapBlockData> persistedBlocks = BlockDataSerializer.loadBlocksFromConfig(trapSection, "blocks");

                            if (remainingTicks <= 0L) {
                                if (!persistedBlocks.isEmpty()) {
                                    this.regionReplacedBlocks.put(trapId, persistedBlocks);
                                    for (Map.Entry<Location, TrapBlockData> entry : persistedBlocks.entrySet()) {
                                        GlobalTrapRegistry.getInstance().registerAndGetOriginal(entry.getKey(), trapId, entry.getValue());
                                    }
                                    this.restoreBlocks(playerId, trapId);
                                }
                                this.removeTrapRegion(trapId, location);
                                this.removeTrapFromFile(location);
                            } else {
                                if (!persistedBlocks.isEmpty()) {
                                    this.regionReplacedBlocks.put(trapId, persistedBlocks);
                                    for (Map.Entry<Location, TrapBlockData> entry : persistedBlocks.entrySet()) {
                                        GlobalTrapRegistry.getInstance().registerAndGetOriginal(entry.getKey(), trapId, entry.getValue());
                                    }
                                } else {
                                    String schematic = this.plugin.getSkinSchematic(skin);
                                    if (schematic != null) {
                                        try {
                                            File schematicFile = getSchematicFile(schematic);
                                            if (schematicFile.exists()) {
                                                try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                                                    Clipboard clipboard = reader.read();
                                                    this.saveReplacedBlocks(playerId, location, clipboard, trapId);
                                                }
                                            }
                                        } catch (Exception e) {}
                                    }
                                }

                                String schematic = this.plugin.getSkinSchematic(skin);
                                if (schematic != null) {
                                    try {
                                        File schematicFile = getSchematicFile(schematic);
                                        if (schematicFile.exists()) {
                                            try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                                                Clipboard clipboard = reader.read();
                                                BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                                                BlockVector3 max = clipboard.getRegion().getMaximumPoint();
                                                BlockVector3 origin = clipboard.getOrigin();
                                                int minRelX = min.getBlockX() - origin.getBlockX();
                                                int maxRelX = max.getBlockX() - origin.getBlockX();
                                                int minRelY = min.getBlockY() - origin.getBlockY();
                                                int maxRelY = max.getBlockY() - origin.getBlockY();
                                                int minRelZ = min.getBlockZ() - origin.getBlockZ();
                                                int maxRelZ = max.getBlockZ() - origin.getBlockZ();
                                                this.createTrapRegion((Player) null, location, minRelX, maxRelX, minRelY, maxRelY, minRelZ, maxRelZ, skin);
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

    public void createTrapRegion(Player player, Location location, int minRelX, int maxRelX, int minRelY, int maxRelY, int minRelZ, int maxRelZ, String skin) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        String playerName = player != null ? player.getName() : "offline";
        String trapId = playerName + "_trap_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();

        BlockVector3 min = BlockVector3.at(
                location.getBlockX() + minRelX,
                location.getBlockY() + minRelY,
                location.getBlockZ() + minRelZ
        );
        BlockVector3 max = BlockVector3.at(
                location.getBlockX() + maxRelX,
                location.getBlockY() + maxRelY,
                location.getBlockZ() + maxRelZ
        );

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
                List<String> toRemove = new ArrayList<>();
                for (String regionName : regionManager.getRegions().keySet()) {
                    if (regionName.contains("_trap_")) {
                        toRemove.add(regionName);
                    }
                }
                for (String regionName : toRemove) {
                    regionManager.removeRegion(regionName);
                }
            }
        }

        this.restoreAllBlocks();
    }

    public Map<String, List<CustomAction>> getSkinActions() {
        return this.skinActions;
    }

    private Map<Location, org.bukkit.block.data.BlockData> captureStructureVoidBlocks(Location center, Clipboard clipboard) {
        Map<Location, org.bukkit.block.data.BlockData> voidBlocks = new HashMap<>();
        BlockVector3 origin = clipboard.getOrigin();
        BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        BlockVector3 max = clipboard.getRegion().getMaximumPoint();
        World world = center.getWorld();

        plugin.getLogger().info("[VioTrap][DEBUG] captureStructureVoidBlocks START");
        plugin.getLogger().info("[VioTrap][DEBUG]   center=" + center.getBlockX() + "," + center.getBlockY() + "," + center.getBlockZ());
        plugin.getLogger().info("[VioTrap][DEBUG]   origin=" + origin.getBlockX() + "," + origin.getBlockY() + "," + origin.getBlockZ());
        plugin.getLogger().info("[VioTrap][DEBUG]   region min=" + min + " max=" + max);

        // Выводим все уникальные типы блоков в схематике — чтобы увидеть точный ID structure_void
        java.util.Set<String> foundTypes = new java.util.LinkedHashSet<>();
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    foundTypes.add(clipboard.getBlock(BlockVector3.at(x, y, z)).getBlockType().getId());
                }
            }
        }
        plugin.getLogger().info("[VioTrap][DEBUG]   All block type IDs in clipboard: " + foundTypes);

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    String blockId = clipboard.getBlock(BlockVector3.at(x, y, z)).getBlockType().getId();
                    if (blockId.equals("minecraft:structure_void") || blockId.equals("structure_void")) {
                        Location worldLoc = new Location(
                                world,
                                center.getBlockX() + (x - origin.getBlockX()),
                                center.getBlockY() + (y - origin.getBlockY()),
                                center.getBlockZ() + (z - origin.getBlockZ())
                        );

                        Block block = worldLoc.getBlock();
                        plugin.getLogger().info("[VioTrap][DEBUG]   Found structure_void at clipboard(" + x + "," + y + "," + z + ")"
                                + " -> world(" + worldLoc.getBlockX() + "," + worldLoc.getBlockY() + "," + worldLoc.getBlockZ() + ")"
                                + " current world block=" + block.getType().name());
                        voidBlocks.put(worldLoc.clone(), block.getBlockData());
                    }
                }
            }
        }

        plugin.getLogger().info("[VioTrap][DEBUG] captureStructureVoidBlocks END: captured " + voidBlocks.size() + " void block(s)");
        return voidBlocks;
    }

    private void restoreStructureVoidBlocks(Map<Location, org.bukkit.block.data.BlockData> voidBlocks) {
        plugin.getLogger().info("[VioTrap][DEBUG] restoreStructureVoidBlocks: restoring " + voidBlocks.size() + " block(s)");
        for (Map.Entry<Location, org.bukkit.block.data.BlockData> entry : voidBlocks.entrySet()) {
            Block block = entry.getKey().getBlock();
            String before = block.getType().name();
            block.setBlockData(entry.getValue());
            plugin.getLogger().info("[VioTrap][DEBUG]   Restored at ("
                    + entry.getKey().getBlockX() + "," + entry.getKey().getBlockY() + "," + entry.getKey().getBlockZ() + ")"
                    + " " + before + " -> " + entry.getValue().getMaterial().name());
        }
    }
}