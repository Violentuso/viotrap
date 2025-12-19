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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.Chest.Type;
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
import org.migrate1337.viotrap.items.PlateItem;
import org.migrate1337.viotrap.items.TrapItem;
import org.migrate1337.viotrap.utils.ActiveSkinsManager;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.PVPManagerHandle;
// Импорты новых классов
import org.migrate1337.viotrap.data.TrapData;
import org.migrate1337.viotrap.data.TrapBlockData;

public class TrapItemListener implements Listener {
    private final VioTrap plugin;
    // Используем TrapBlockData вместо BlockData
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

        for(String skin : plugin.getSkinNames()) {
            this.skinActions.put(skin, CustomActionFactory.loadActions(skin, plugin));
        }

        this.loadTrapsFromConfig();
        this.loadCooldownsFromConfig();
        plugin.getLogger().info("[VioTrap] TrapItemListener инициализирован, загружено " + this.activeTrapTimers.size() + " активных трапок");
    }

    @EventHandler
    public void onPlayerUseTrap(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && TrapItem.isTrapItem(item)) {

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

                Location location = player.getLocation();
                String skin = this.activeSkinsManager.getActiveTrapSkin(player.getUniqueId());
                if (skin == null || skin.isEmpty()) {
                    skin = "default";
                }
                if (!player.hasCooldown(item.getType()) && !this.isCooldownActive(player.getUniqueId(), skin)) {


                    String schematic = this.plugin.getSkinSchematic(skin);
                    if (schematic != null && (this.plugin.getConfig().contains("skins." + skin) || skin.equals("default"))) {
                        if (!this.isInBannedRegion(location, location.getWorld().getName()) && !this.hasBannedRegionFlags(location, location.getWorld().getName())) {
                            if (this.isRegionNearby(location, location.getWorld().getName())) {
                                player.sendMessage(this.plugin.getTrapMessageNearby());
                            } else {
                                File schematicFile = new File("plugins/WorldEdit/schematics/" + schematic);
                                if (!schematicFile.exists()) {
                                    player.sendMessage("§cСхематика " + schematic + " не найдена!");
                                } else {
                                    try {
                                        try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                                            Clipboard clipboard = reader.read();
                                            BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                                            BlockVector3 max = clipboard.getRegion().getMaximumPoint();
                                            double sizeX = (double)(max.getBlockX() - min.getBlockX() + 1);
                                            double sizeY = (double)(max.getBlockY() - min.getBlockY() + 1);
                                            double sizeZ = (double)(max.getBlockZ() - min.getBlockZ() + 1);
                                            int offsetX = (int)(-(sizeX / (double)2.0F));
                                            int offsetY = (int)(-(sizeY / (double)2.0F)) + 1;
                                            int offsetZ = (int)(-(sizeZ / (double)2.0F));
                                            if (sizeX == (double)5.0F && sizeY == (double)5.0F && sizeZ == (double)5.0F) {
                                                offsetY = (int)(-(sizeY / (double)2.0F)) + 1;
                                            } else if (sizeX == (double)7.0F && sizeY == (double)7.0F && sizeZ == (double)7.0F) {
                                                offsetY = (int)(-(sizeY / (double)2.0F)) + 2;
                                            } else if (sizeX == (double)9.0F && sizeY == (double)9.0F && sizeZ == (double)9.0F) {
                                                offsetY = (int)(-(sizeY / (double)2.0F)) + 2;
                                            }

                                            Set<Location> locationsToCheck = new HashSet<>();

                                            for(int x = min.getBlockX(); x <= max.getBlockX(); ++x) {
                                                for(int y = min.getBlockY(); y <= max.getBlockY(); ++y) {
                                                    for(int z = min.getBlockZ(); z <= max.getBlockZ(); ++z) {
                                                        Location blockLocation = new Location(location.getWorld(), Math.floor(location.getX() + (double)(x - min.getBlockX() + offsetX)), Math.floor(location.getY() + (double)(y - min.getBlockY() + offsetY)), Math.floor(location.getZ() + (double)(z - min.getBlockZ() + offsetZ)));
                                                        locationsToCheck.add(blockLocation.clone());
                                                    }
                                                }
                                            }


                                            int originalAmount = item.getAmount();
                                            item.setAmount(originalAmount - 1);
                                            Logger var10000 = this.plugin.getLogger();
                                            String var10001 = String.valueOf(player.getUniqueId());
                                            var10000.info("[VioTrap] Trap consumed for player " + var10001 + ", new amount: " + item.getAmount());
                                            BukkitScheduler var70 = Bukkit.getScheduler();
                                            VioTrap var72 = this.plugin;
                                            Objects.requireNonNull(player);
                                            var70.runTaskLater(var72, player::updateInventory, 1L);
                                            int cooldownSeconds = (int)(skin.equals("default") ? (double)this.plugin.getTrapCooldown() : this.plugin.getConfig().getInt("skins." + skin + ".cooldown", (int)this.plugin.getTrapCooldown()));
                                            int cooldownTicks = cooldownSeconds * 20;
                                            player.setCooldown(item.getType(), cooldownTicks);
                                            this.saveCooldownToConfig(player.getUniqueId(), item.getType(), System.currentTimeMillis() + (long)(cooldownTicks * 50));
                                            if (this.plugin.getConfig().getString("trap.enable-pvp", "true").equals("true")) {
                                                enablePvpForPlayer(player);
                                            }

                                            String soundType = skin.equals("default") ? this.plugin.getTrapSoundType() : this.plugin.getConfig().getString("skins." + skin + ".sound.type", this.plugin.getTrapSoundType());
                                            float soundVolume = (float)(skin.equals("default") ? (double)this.plugin.getTrapSoundVolume() : this.plugin.getConfig().getDouble("skins." + skin + ".sound.volume", (double)this.plugin.getTrapSoundVolume()));
                                            float soundPitch = (float)(skin.equals("default") ? (double)this.plugin.getTrapSoundPitch() : this.plugin.getConfig().getDouble("skins." + skin + ".sound.pitch", (double)this.plugin.getTrapSoundPitch()));
                                            player.playSound(location, Sound.valueOf(soundType), soundVolume, soundPitch);
                                            this.applyEffects(player, "skins." + skin + ".effects.player");
                                            player.sendMessage(this.plugin.getConfig().getString("trap.messages.success_used"));
                                            List<CustomAction> actions = CustomActionFactory.loadActions(skin, plugin);
                                            this.skinActions.put(skin, actions);
                                            Player[] opponents = location.getWorld().getNearbyEntities(location, sizeX - (double)3.0F, sizeY, sizeZ - (double)3.0F, (entity) -> entity instanceof Player && !entity.equals(player)).stream().filter((entity) -> entity instanceof Player).toArray(Player[]::new);

                                            for(CustomAction action : actions) {
                                                action.execute(player, opponents, this.plugin);
                                            }

                                            for(Player opponent : opponents) {
                                                this.enablePvpForPlayer(opponent);
                                            }

                                            String var71 = player.getName();
                                            String trapId = var71 + "_trap_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
                                            this.saveTrapToConfig(player, location, skin);
                                            TrapData trapData = new TrapData(player.getUniqueId(), location, skin, (long)this.plugin.getTrapDuration());
                                            this.activeTrapTimers.put(trapId, trapData);
                                            this.plugin.getLogger().info("[VioTrap] Трапка " + trapId + " создана с длительностью " + this.plugin.getTrapDuration() + " секунд");
                                            this.createTrapRegion(player, location, sizeX, sizeY, sizeZ, skin);

                                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
                                                BlockVector3 pastePosition = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                                                this.saveReplacedBlocks(player.getUniqueId(), location, clipboard);
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
                                                        this.plugin.getLogger().info("[VioTrap] Время трапки " + trapId + " истекло, восстанавливаем блоки");
                                                        this.restoreBlocks(data.getPlayerId());
                                                        this.removeTrapRegion(trapId, data.getLocation());
                                                        this.removeTrapFromFile(data.getLocation());
                                                        this.activeTrapTimers.remove(trapId);
                                                        String soundTypeEnded = finalSkin.equals("default") ? this.plugin.getTrapSoundTypeEnded() : this.plugin.getConfig().getString("skins." + finalSkin + ".sound.type-ended", this.plugin.getTrapSoundTypeEnded());
                                                        float soundVolumeEnded = (float)(finalSkin.equals("default") ? (double)this.plugin.getTrapSoundVolumeEnded() : this.plugin.getConfig().getDouble("skins." + finalSkin + ".sound.volume-ended", (double)this.plugin.getTrapSoundVolumeEnded()));
                                                        float soundPitchEnded = (float)(finalSkin.equals("default") ? (double)this.plugin.getTrapSoundPitchEnded() : this.plugin.getConfig().getDouble("skins." + finalSkin + ".sound.pitch-ended", (double)this.plugin.getTrapSoundPitchEnded()));



                                                        this.setCooldownActive(player.getUniqueId(), finalSkin, cooldownSeconds * 1000L);

                                                        player.setCooldown(item.getType(), cooldownSeconds * 20);
                                                        location.getWorld().playSound(location, Sound.valueOf(soundTypeEnded), soundVolumeEnded, soundPitchEnded);
                                                    } else {
                                                        this.plugin.getLogger().warning("[VioTrap] Трапка " + trapId + " не найдена в activeTrapTimers при истечении времени");
                                                    }

                                                }, (int)(skin.equals("default") ? (double)this.plugin.getTrapDuration() * 20 : this.plugin.getConfig().getInt("skins." + skin + ".duration", (int)this.plugin.getTrapCooldown()) * 20));
                                            }
                                        }
                                    } catch (Exception e) {
                                        this.plugin.getLogger().warning("[VioTrap] Ошибка при установке трапки: " + e.getMessage());
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
        String var10000 = location.getWorld().getName();
        String path = "traps." + var10000 + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        this.plugin.getTrapsConfig().set(path, (Object)null);
        this.plugin.saveTrapsConfig();
        this.plugin.getLogger().info("[VioTrap] Трапка удалена из конфига: " + path);
    }

    private void replaceSkinnedTrapsWithNewSkin(Player player, String oldSkin, String newSkin) {
        Inventory inventory = player.getInventory();

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack item = inventory.getItem(i);
            if (item != null && TrapItem.isTrapItem(item) && oldSkin.equals(TrapItem.getSkin(item))) {
                int amount = item.getAmount();
                inventory.setItem(i, TrapItem.getTrapItem(amount, newSkin));
            }
        }

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack item = inventory.getItem(i);
            if (item != null && PlateItem.getUniqueId(item) != null && oldSkin.equals(PlateItem.getSkin(item))) {
                int amount = item.getAmount();
                inventory.setItem(i, PlateItem.getPlateItem(amount, newSkin));
            }
        }

        BukkitScheduler var10000 = Bukkit.getScheduler();
        VioTrap var10001 = this.plugin;
        Objects.requireNonNull(player);
        var10000.runTaskLater(var10001, player::updateInventory, 1L);
    }
    public boolean isCooldownActive(UUID uuid, String skin) {
        Map<String, Long> skinsCooldowns = playerCooldowns.getOrDefault(uuid, new HashMap<>());
        Long endTime = skinsCooldowns.get(skin);
        if (endTime != null) {
            long remaining = (endTime - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                plugin.getLogger().info("[VioTrap] Кулдаун активен для " + skin + ": осталось " + remaining + "с");
                return true;
            }
        }
        return false;
    }
    private void setCooldownActive(UUID uuid, String skin, long durationMs) {
        playerCooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(skin, System.currentTimeMillis() + durationMs);
        plugin.getLogger().info("[VioTrap] Установлен кулдаун для " + skin + ": " + (durationMs / 1000) + "с");
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockY() != event.getTo().getBlockY() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            Location location = event.getTo();
            boolean wasInTrapRegion = this.playersInTrapRegions.contains(player.getUniqueId());
            boolean isNowInTrapRegion = this.isInAnyTrapRegion(location);
            if (!wasInTrapRegion && isNowInTrapRegion) {
                this.playersInTrapRegions.add(player.getUniqueId());
                this.enablePvpForPlayer(player);
            } else if (wasInTrapRegion && !isNowInTrapRegion) {
                this.playersInTrapRegions.remove(player.getUniqueId());
            }

        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        this.plugin.getLogger().info("[VioTrap] Игрок " + String.valueOf(playerId) + " вошёл, проверяем кулдауны");
        Material trapMaterial = Material.getMaterial(this.plugin.getConfig().getString("trap.item", "TRIPWIRE_HOOK"));
        if (trapMaterial != null && this.isCooldownActive(playerId, trapMaterial)) {
            long remainingTime = this.getCooldownRemainingTime(playerId, trapMaterial);
            if (remainingTime > 0L) {
                int remainingTicks = (int)(remainingTime / 50L);
                player.setCooldown(trapMaterial, remainingTicks);
                Logger var10000 = this.plugin.getLogger();
                String var10001 = String.valueOf(playerId);
                var10000.info("[VioTrap] Восстановлен кулдаун для игрока " + var10001 + " на " + remainingTicks + " тиков");
            } else {
                this.removeCooldownFromConfig(playerId, trapMaterial);
            }
        }

    }

    private boolean isInAnyTrapRegion(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null) {
            return false;
        } else {
            BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

            for(ProtectedRegion region : regionManager.getApplicableRegions(vector).getRegions()) {
                if (region.getId().endsWith("_trap")) {
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
        String var10000 = playerId.toString();
        String path = "cooldowns." + var10000 + "." + material.toString();
        this.plugin.getTrapsConfig().set(path + ".endTime", endTime);
        this.plugin.saveTrapsConfig();
        Logger var8 = this.plugin.getLogger();
        String var10001 = String.valueOf(playerId);
        var8.info("[VioTrap] Сохранён кулдаун для игрока " + var10001 + " на предмет " + String.valueOf(material) + ", endTime: " + endTime);
        long remainingTime = endTime - System.currentTimeMillis();
        if (remainingTime > 0L) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.removeCooldownFromConfig(playerId, material), remainingTime / 50L);
        }

    }

    private boolean isCooldownActive(UUID playerId, Material material) {
        String var10000 = playerId.toString();
        String path = "cooldowns." + var10000 + "." + material.toString();
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
        String var10000 = playerId.toString();
        String path = "cooldowns." + var10000 + "." + material.toString();
        long endTime = this.plugin.getTrapsConfig().getLong(path + ".endTime", 0L);
        long currentTime = System.currentTimeMillis();
        return Math.max(0L, endTime - currentTime);
    }

    private void removeCooldownFromConfig(UUID playerId, Material material) {
        String var10000 = playerId.toString();
        String path = "cooldowns." + var10000 + "." + material.toString();
        this.plugin.getTrapsConfig().set(path, (Object)null);
        this.plugin.saveTrapsConfig();
        Logger var4 = this.plugin.getLogger();
        String var10001 = String.valueOf(playerId);
        var4.info("[VioTrap] Удалён кулдаун для игрока " + var10001 + " на предмет " + String.valueOf(material));
    }

    private void loadCooldownsFromConfig() {
        this.plugin.getLogger().info("[VioTrap] Загружаем кулдауны из traps.yml...");
        if (!this.plugin.getTrapsConfig().contains("cooldowns")) {
            this.plugin.getLogger().info("[VioTrap] Раздел cooldowns в конфиге отсутствует");
        } else {
            ConfigurationSection cooldownsSection = this.plugin.getTrapsConfig().getConfigurationSection("cooldowns");

            for(String playerIdStr : cooldownsSection.getKeys(false)) {
                UUID playerId;
                try {
                    playerId = UUID.fromString(playerIdStr);
                } catch (IllegalArgumentException var15) {
                    this.plugin.getLogger().warning("[VioTrap] Некорректный UUID игрока в cooldowns: " + playerIdStr);
                    continue;
                }

                ConfigurationSection playerSection = cooldownsSection.getConfigurationSection(playerIdStr);

                for(String materialName : playerSection.getKeys(false)) {
                    Material material = Material.getMaterial(materialName);
                    if (material == null) {
                        this.plugin.getLogger().warning("[VioTrap] Некорректный материал в cooldowns: " + materialName);
                    } else {
                        long endTime = playerSection.getLong(materialName + ".endTime", 0L);
                        long currentTime = System.currentTimeMillis();
                        long remainingTime = endTime - currentTime;
                        if (remainingTime <= 0L) {
                            this.removeCooldownFromConfig(playerId, material);
                        } else {
                            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.removeCooldownFromConfig(playerId, material), remainingTime / 50L);
                            this.plugin.getLogger().info("[VioTrap] Загружен кулдаун для игрока " + String.valueOf(playerId) + " на предмет " + materialName + ", оставшееся время: " + remainingTime + " мс");
                        }
                    }
                }
            }

        }
    }

    private void saveTrapToConfig(Player player, Location location, String skin) {
        String var10000 = location.getWorld().getName();
        String path = "traps." + var10000 + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        this.plugin.getTrapsConfig().set(path + ".player", player.getUniqueId().toString());
        this.plugin.getTrapsConfig().set(path + ".world", location.getWorld().getName());
        this.plugin.getTrapsConfig().set(path + ".x", location.getBlockX());
        this.plugin.getTrapsConfig().set(path + ".y", location.getBlockY());
        this.plugin.getTrapsConfig().set(path + ".z", location.getBlockZ());
        this.plugin.getTrapsConfig().set(path + ".skin", skin);
        this.plugin.getTrapsConfig().set(path + ".endTime", System.currentTimeMillis() + (long)(this.plugin.getTrapDuration() * 1000));
        this.plugin.saveTrapsConfig();
        this.plugin.getLogger().info("[VioTrap] Трапка сохранена в конфиг: " + path + ", skin: " + skin + ", endTime: " + (System.currentTimeMillis() + (long)(this.plugin.getTrapDuration() * 1000)));
    }

    private void applyEffects(Player player, String configPath) {
        if (this.plugin.getConfig().contains(configPath)) {
            this.plugin.getConfig().getConfigurationSection(configPath).getKeys(false).forEach((effectName) -> {
                try {
                    int duration = this.plugin.getConfig().getInt(configPath + "." + effectName + ".duration") * 20;
                    int amplifier = this.plugin.getConfig().getInt(configPath + "." + effectName + ".amplifier");
                    player.addPotionEffect(new PotionEffect(PotionEffectType.getByName(effectName), duration, amplifier));
                } catch (Exception var6) {
                    this.plugin.getLogger().warning("Ошибка применения эффекта: " + effectName);
                }

            });
        }
    }

    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager == null) {
            this.plugin.getLogger().info("[VioTrap] RegionManager для мира " + worldName + " не найден, трапка разрешена по умолчанию");
            return false;
        } else {
            BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(vector);
            List<ProtectedRegion> regions = new ArrayList(applicableRegions.getRegions());
            if (regions.isEmpty()) {
                this.plugin.getLogger().info("[VioTrap] Регионы не найдены в точке " + location.toString() + ", трапка разрешена");
                return false;
            } else {
                regions.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
                ProtectedRegion highestPriorityRegion = (ProtectedRegion)regions.get(0);
                int highestPriority = highestPriorityRegion.getPriority();
                Logger var10000 = this.plugin.getLogger();
                String var10001 = highestPriorityRegion.getId();
                var10000.info("[VioTrap] Регион с наивысшим приоритетом: " + var10001 + " (приоритет: " + highestPriority + ")");
                boolean disabledAllRegions = this.plugin.getConfig().getBoolean("trap.disabled_all_regions", false);
                List<String> bannedRegions = this.plugin.getConfig().getStringList("trap.banned_regions");
                if (disabledAllRegions && !highestPriorityRegion.getId().equals("__default__")) {
                    this.plugin.getLogger().info("[VioTrap] Трапка запрещена в регионе " + highestPriorityRegion.getId() + " из-за disabled_all_regions");
                    return true;
                } else if (bannedRegions.contains(highestPriorityRegion.getId())) {
                    this.plugin.getLogger().info("[VioTrap] Трапка запрещена в регионе " + highestPriorityRegion.getId() + " из-за banned_regions");
                    return true;
                } else {
                    this.plugin.getLogger().info("[VioTrap] Трапка разрешена в регионе " + highestPriorityRegion.getId());
                    return false;
                }
            }
        }
    }

    private boolean hasBannedRegionFlags(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager == null) {
            this.plugin.getLogger().info("[VioTrap] RegionManager для мира " + worldName + " не найден, флаги не проверяются");
            return false;
        } else {
            ConfigurationSection bannedFlagsSection = this.plugin.getConfig().getConfigurationSection("trap.banned_region_flags");
            if (bannedFlagsSection == null) {
                this.plugin.getLogger().info("[VioTrap] Секция banned_region_flags отсутствует в конфиге");
                return false;
            } else {
                BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(vector);
                List<ProtectedRegion> regions = new ArrayList(applicableRegions.getRegions());
                if (regions.isEmpty()) {
                    this.plugin.getLogger().info("[VioTrap] Регионы не найдены в точке " + location.toString() + ", флаги не проверяются");
                    return false;
                } else {
                    regions.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
                    ProtectedRegion highestPriorityRegion = (ProtectedRegion)regions.get(0);
                    int highestPriority = highestPriorityRegion.getPriority();
                    Logger var10000 = this.plugin.getLogger();
                    String var10001 = highestPriorityRegion.getId();
                    var10000.info("[VioTrap] Проверка флагов в регионе с наивысшим приоритетом: " + var10001 + " (приоритет: " + highestPriority + ")");
                    if (highestPriorityRegion.getId().endsWith("_trap")) {
                        this.plugin.getLogger().info("[VioTrap] Регион " + highestPriorityRegion.getId() + " является трапкой, флаги игнорируются");
                        return false;
                    } else {
                        for(String flagName : bannedFlagsSection.getKeys(false)) {
                            StateFlag flag = (StateFlag)Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                            if (flag == null) {
                                this.plugin.getLogger().warning("[VioTrap] Флаг " + flagName + " не найден в WorldGuard");
                            } else if (highestPriorityRegion.getFlag(flag) != null) {
                                this.plugin.getLogger().info("[VioTrap] Обнаружен запрещённый флаг " + flagName + " в регионе " + highestPriorityRegion.getId());
                                return true;
                            }
                        }

                        this.plugin.getLogger().info("[VioTrap] Запрещённые флаги не найдены в регионе " + highestPriorityRegion.getId());
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

    private void saveReplacedBlocks(UUID playerId, Location center, Clipboard clipboard) {
        Map<Location, TrapBlockData> replacedBlocks = this.playerReplacedBlocks.computeIfAbsent(playerId, k -> new HashMap<>());
        Set<Location> locationsToCheck = new HashSet<>();

        BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        BlockVector3 max = clipboard.getRegion().getMaximumPoint();

        double sizeX = (max.getBlockX() - min.getBlockX() + 1);
        double sizeY = (max.getBlockY() - min.getBlockY() + 1);
        double sizeZ = (max.getBlockZ() - min.getBlockZ() + 1);

        int offsetX = (int) (-(sizeX / 2.0F));
        int offsetY = (int) (-(sizeY / 2.0F)) + 1;
        int offsetZ = (int) (-(sizeZ / 2.0F));

        if (sizeX == 5) offsetY = (int) (-(sizeY / 2.0F)) + 1;
        else if (sizeX == 7) offsetY = (int) (-(sizeY / 2.0F)) + 2;
        else if (sizeX == 9) offsetY = (int) (-(sizeY / 2.0F)) + 2;

        // Собираем все локации
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Location loc = new Location(center.getWorld(),
                            Math.floor(center.getX() + (x - min.getBlockX() + offsetX)),
                            Math.floor(center.getY() + (y - min.getBlockY() + offsetY)),
                            Math.floor(center.getZ() + (z - min.getBlockZ() + offsetZ)));
                    locationsToCheck.add(loc.clone());
                }
            }
        }

        plugin.getLogger().info("§e[VioTrap DEBUG] === НАЧАЛО СОХРАНЕНИЯ БЛОКОВ ДЛЯ " + playerId + " ===");
        plugin.getLogger().info("§e[VioTrap DEBUG] Затронуто локаций: " + locationsToCheck.size());

        int chestCount = 0;
        int doubleChestCount = 0;

        for (Location loc : locationsToCheck) {
            Block block = loc.getBlock();
            BlockState state = block.getState();

            ItemStack[] contents = null;
            String spawnedType = "UNKNOWN";
            boolean isDoubleChest = false;
            Location pairedLocation = null;

            // Спавнеры
            if (state instanceof CreatureSpawner) {
                CreatureSpawner spawner = (CreatureSpawner) state;
                spawnedType = spawner.getSpawnedType().name();
                plugin.getLogger().info("§a[VioTrap DEBUG] Спавнер на " + loc + " → " + spawnedType);
            }

            // Сундуки и другие контейнеры
            if (state instanceof Container) {
                chestCount++;
                Container container = (Container) state;
                Inventory inventory = container.getInventory();
                InventoryHolder holder = inventory.getHolder();

                if (holder instanceof DoubleChest) {
                    DoubleChest doubleChest = (DoubleChest) holder;
                    doubleChestCount++;
                    isDoubleChest = true;

                    Inventory combined = doubleChest.getInventory();
                    contents = combined.getContents() != null ? combined.getContents().clone() : new ItemStack[54];

                    // Ищем вторую половину
                    BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
                    for (BlockFace face : faces) {
                        Location candidate = loc.clone().add(face.getDirection());
                        if (locationsToCheck.contains(candidate)) {
                            Block b = candidate.getBlock();
                            if (b.getState() instanceof Chest) {
                                Chest chest = (Chest) b.getState();
                                if (chest.getInventory().getHolder() instanceof DoubleChest) {
                                    pairedLocation = candidate.clone();
                                    break;
                                }
                            }
                        }
                    }

                    plugin.getLogger().info("§6[VioTrap DEBUG] §lDOUBLE CHEST найден на " + loc +
                            " | Предметов: " + contents.length +
                            " | Парная половина: " + (pairedLocation != null ? pairedLocation.toString() : "НЕ НАЙДЕНА"));
                } else {
                    // Одиночный контейнер
                    contents = inventory.getContents() != null ? inventory.getContents().clone() : new ItemStack[27];
                    plugin.getLogger().info("§b[VioTrap DEBUG] §lОДИНОЧНЫЙ СУНДУК на " + loc + " | Предметов: " + contents.length);
                }
            }

            TrapBlockData blockData = new TrapBlockData(
                    block.getType(),
                    block.getBlockData(),
                    contents,
                    state instanceof CreatureSpawner ? spawnedType : null
            );

            blockData.setDoubleChest(isDoubleChest);
            if (pairedLocation != null) {
                blockData.setPairedChestLocation(pairedLocation);
            }

            replacedBlocks.put(loc.clone(), blockData);
        }

        plugin.getLogger().info("§e[VioTrap DEBUG] === СОХРАНЕНИЕ ЗАВЕРШЕНО ===");
        plugin.getLogger().info("§e[VioTrap DEBUG] Всего блоков сохранено: " + replacedBlocks.size());
        plugin.getLogger().info("§e[VioTrap DEBUG] Сундуков всего: " + chestCount + " | Двойных: " + doubleChestCount);
    }

    public void restoreAllBlocks() {
        this.plugin.getLogger().info("[VioTrap] Вызван restoreAllBlocks!");
        if (this.playerReplacedBlocks.isEmpty()) {
            this.plugin.getLogger().info("[VioTrap] playerReplacedBlocks пуст, загружаем данные из конфига...");
            this.loadTrapsFromConfig();
        }

        for(Object playerId : new HashSet(this.playerReplacedBlocks.keySet())) {
            this.plugin.getLogger().info("[VioTrap] Восстанавливаем блоки для игрока " + String.valueOf(playerId));
            this.restoreBlocks((UUID) playerId);
        }

        this.plugin.getLogger().info("[VioTrap] Все блоки успешно восстановлены.");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.plugin.getLogger().info("[VioTrap] Игрок " + String.valueOf(playerId) + " вышел, удаляем из списка игроков в регионах трапок");
        this.playersInTrapRegions.remove(playerId);

        for(Map.Entry<String, TrapData> entry : this.activeTrapTimers.entrySet()) {
            if (((TrapData)entry.getValue()).getPlayerId().equals(playerId)) {
                Logger var10000 = this.plugin.getLogger();
                String var10001 = (String)entry.getKey();
                var10000.info("[VioTrap] Трапка " + var10001 + " остаётся активной для игрока " + String.valueOf(playerId));
            }
        }

    }

    private void restoreBlocks(UUID playerId) {
        Map<Location, TrapBlockData> replacedBlocks = this.playerReplacedBlocks.get(playerId);
        if (replacedBlocks == null || replacedBlocks.isEmpty()) {
            this.plugin.getLogger().info("[VioTrap] Нет блоков для восстановления у игрока " + playerId);
            return;
        }

        this.plugin.getLogger().info("[VioTrap] Восстановление " + replacedBlocks.size() + " блоков для " + playerId);

        for (Map.Entry<Location, TrapBlockData> entry : replacedBlocks.entrySet()) {
            Location location = entry.getKey();
            TrapBlockData blockData = entry.getValue();

            Block block = location.getBlock();
            block.setType(blockData.getMaterial());
            block.setBlockData(blockData.getBlockData());

            BlockState state = block.getState();

            // Восстановление спавнеров
            if (state instanceof CreatureSpawner) {
                CreatureSpawner spawner = (CreatureSpawner) state;
                if (blockData.getSpawnedType() != null && !"UNKNOWN".equals(blockData.getSpawnedType())) {
                    spawner.setSpawnedType(EntityType.valueOf(blockData.getSpawnedType()));
                    spawner.update();
                }
            }

            // Восстановление контейнеров
            if (state instanceof Container) {
                Container container = (Container) state;
                InventoryHolder holder = container.getInventory().getHolder();

                if (blockData.getContents() == null) continue;

                if (holder instanceof DoubleChest) {
                    DoubleChest doubleChest = (DoubleChest) holder;
                    doubleChest.getInventory().setContents(blockData.getContents());
                    this.plugin.getLogger().info("[VioTrap] Восстановлен двойной сундук на " + location);
                } else {
                    // Одиночный контейнер — обрезаем до 27 слотов
                    ItemStack[] oldContents = blockData.getContents();
                    ItemStack[] newContents = new ItemStack[27];
                    System.arraycopy(oldContents, 0, newContents, 0, Math.min(oldContents.length, 27));
                    container.getInventory().setContents(newContents);
                    this.plugin.getLogger().info("[VioTrap] Восстановлен одиночный контейнер на " + location);
                }
            }

            // Удаляем из конфига (если нужно)
            this.removeTrapFromFile(location);
        }

        this.playerReplacedBlocks.remove(playerId);
        this.plugin.getLogger().info("[VioTrap] Блоки успешно восстановлены для игрока " + playerId);
    }

    private void loadTrapsFromConfig() {
        this.plugin.getLogger().info("[VioTrap] Загружаем ловушки из traps.yml...");
        if (!this.plugin.getTrapsConfig().contains("traps")) {
            this.plugin.getLogger().info("[VioTrap] Раздел traps в конфиге отсутствует");
        } else {
            ConfigurationSection trapsSection = this.plugin.getTrapsConfig().getConfigurationSection("traps");

            for(String worldName : trapsSection.getKeys(false)) {
                ConfigurationSection worldSection = trapsSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    for(String trapKey : worldSection.getKeys(false)) {
                        ConfigurationSection trapSection = worldSection.getConfigurationSection(trapKey);
                        if (trapSection != null) {
                            UUID playerId = UUID.fromString(trapSection.getString("player"));
                            String world = trapSection.getString("world");
                            int x = trapSection.getInt("x");
                            int y = trapSection.getInt("y");
                            int z = trapSection.getInt("z");
                            String skin = trapSection.getString("skin", "default");
                            Location location = new Location(Bukkit.getWorld(world), (double)x, (double)y, (double)z);
                            if (Bukkit.getWorld(world) == null) {
                                this.plugin.getLogger().warning("[VioTrap] Мир " + world + " не загружен, пропускаем трапку " + trapKey);
                            } else {
                                long currentTime = System.currentTimeMillis();
                                long endTime = trapSection.getLong("endTime", currentTime + (long)(this.plugin.getTrapDuration() * 1000));
                                long remainingTicks = (endTime - currentTime) / 50L;
                                if (remainingTicks <= 0L) {
                                    this.plugin.getLogger().info("[VioTrap] Трапка " + trapKey + " истекла, удаляем");
                                    this.restoreBlocks(playerId);
                                    this.removeTrapRegion(String.valueOf(playerId) + "_trap_" + x + "_" + y + "_" + z, location);
                                    this.removeTrapFromFile(location);
                                } else {
                                    this.playerReplacedBlocks.putIfAbsent(playerId, new HashMap<>());
                                    Block block = location.getBlock();
                                    // Используем TrapBlockData
                                    TrapBlockData blockData = new TrapBlockData(block.getType(), block.getBlockData(), (ItemStack[])null, (String)null);
                                    this.playerReplacedBlocks.get(playerId).put(location, blockData);
                                    String schematic = this.plugin.getSkinSchematic(skin);
                                    if (schematic != null && (this.plugin.getConfig().contains("skins." + skin) || skin.equals("default"))) {
                                        try {
                                            File schematicFile = new File("plugins/WorldEdit/schematics/" + schematic);
                                            if (!schematicFile.exists()) {
                                                this.plugin.getLogger().warning("[VioTrap] Схематика " + schematic + " для трапки " + trapKey + " не найдена");
                                                continue;
                                            }

                                            try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                                                Clipboard clipboard = reader.read();
                                                BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                                                BlockVector3 max = clipboard.getRegion().getMaximumPoint();
                                                double sizeX = (double)(max.getBlockX() - min.getBlockX() + 1);
                                                double sizeY = (double)(max.getBlockY() - min.getBlockY() + 1);
                                                double sizeZ = (double)(max.getBlockZ() - min.getBlockZ() + 1);
                                                this.createTrapRegion((Player)null, location, sizeX, sizeY, sizeZ, skin);
                                            }
                                        } catch (Exception e) {
                                            this.plugin.getLogger().warning("[VioTrap] Ошибка при загрузке схематики для трапки " + trapKey + ": " + e.getMessage());
                                            continue;
                                        }

                                        String trapId = String.valueOf(playerId) + "_trap_" + x + "_" + y + "_" + z;
                                        TrapData trapData = new TrapData(playerId, location, skin, remainingTicks / 20L);
                                        this.activeTrapTimers.put(trapId, trapData);
                                        this.plugin.getLogger().info("[VioTrap] Загружена трапка " + trapId + " с оставшимся временем " + remainingTicks + " тиков");
                                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                            TrapData data = this.activeTrapTimers.get(trapId);
                                            if (data != null) {
                                                this.plugin.getLogger().info("[VioTrap] Время трапки " + trapId + " истекло, восстанавливаем блоки");
                                                this.restoreBlocks(data.getPlayerId());
                                                this.removeTrapRegion(trapId, data.getLocation());
                                                this.removeTrapFromFile(data.getLocation());
                                                this.activeTrapTimers.remove(trapId);
                                                String soundTypeEnded = skin.equals("default") ? this.plugin.getTrapSoundTypeEnded() : this.plugin.getConfig().getString("skins." + skin + ".sound.type-ended", this.plugin.getTrapSoundTypeEnded());
                                                float soundVolumeEnded = (float)(skin.equals("default") ? (double)this.plugin.getTrapSoundVolumeEnded() : this.plugin.getConfig().getDouble("skins." + skin + ".sound.volume-ended", (double)this.plugin.getTrapSoundVolumeEnded()));
                                                float soundPitchEnded = (float)(skin.equals("default") ? (double)this.plugin.getTrapSoundPitchEnded() : this.plugin.getConfig().getDouble("skins." + skin + ".sound.pitch-ended", (double)this.plugin.getTrapSoundPitchEnded()));
                                                location.getWorld().playSound(location, Sound.valueOf(soundTypeEnded), soundVolumeEnded, soundPitchEnded);
                                            } else {
                                                this.plugin.getLogger().warning("[VioTrap] Трапка " + trapId + " не найдена в activeTrapTimers при истечении времени");
                                            }

                                        }, remainingTicks);
                                    } else {
                                        this.plugin.getLogger().warning("[VioTrap] Скин " + skin + " для трапки " + trapKey + " не найден, пропускаем");
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.plugin.getLogger().info("[VioTrap] Загружено " + this.activeTrapTimers.size() + " активных ловушек.");
        }
    }

    public void createTrapRegion(Player player, Location location, double sizeX, double sizeY, double sizeZ, String skin) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        String var10000 = player != null ? player.getName() : "offline";
        String trapId = var10000 + "_trap_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        BlockVector3 min = BlockVector3.at((double)location.getBlockX() - sizeX / (double)2.0F, (double)location.getBlockY() - sizeY / (double)2.0F + (double)2.0F, (double)location.getBlockZ() - sizeZ / (double)2.0F);
        BlockVector3 max = BlockVector3.at((double)location.getBlockX() + sizeX / (double)2.0F, (double)location.getBlockY() + sizeY / (double)2.0F + (double)1.0F, (double)location.getBlockZ() + sizeZ / (double)2.0F);
        if (regionManager == null) {
            this.plugin.getLogger().warning("[VioTrap] RegionManager для мира " + location.getWorld().getName() + " не найден");
        } else {
            if (sizeX == (double)5.0F && sizeY == (double)5.0F && sizeZ == (double)5.0F) {
                min = BlockVector3.at((double)location.getBlockX() - sizeX / (double)2.0F, (double)location.getBlockY() - sizeY / (double)2.0F + (double)2.0F, (double)location.getBlockZ() - sizeZ / (double)2.0F);
                max = BlockVector3.at((double)location.getBlockX() + sizeX / (double)2.0F, (double)location.getBlockY() + sizeY / (double)2.0F + (double)1.0F, (double)location.getBlockZ() + sizeZ / (double)2.0F);
            } else if (sizeX == (double)7.0F && sizeY == (double)7.0F && sizeZ == (double)7.0F) {
                min = BlockVector3.at((double)location.getBlockX() - sizeX / (double)2.0F, (double)location.getBlockY() - sizeY / (double)2.0F + (double)3.0F, (double)location.getBlockZ() - sizeZ / (double)2.0F);
                max = BlockVector3.at((double)location.getBlockX() + sizeX / (double)2.0F, (double)location.getBlockY() + sizeY / (double)2.0F + (double)2.0F, (double)location.getBlockZ() + sizeZ / (double)2.0F);
            } else if (sizeX == (double)9.0F && sizeY == (double)9.0F && sizeZ == (double)9.0F) {
                min = BlockVector3.at((double)location.getBlockX() - sizeX / (double)2.0F, (double)location.getBlockY() - sizeY / (double)2.0F + (double)4.0F, (double)location.getBlockZ() - sizeZ / (double)2.0F);
                max = BlockVector3.at((double)location.getBlockX() + sizeX / (double)2.0F, (double)location.getBlockY() + sizeY / (double)2.0F + (double)3.0F, (double)location.getBlockZ() + sizeZ / (double)2.0F);
            }

            ProtectedCuboidRegion region = new ProtectedCuboidRegion(trapId, min, max);
            ConfigurationSection flagsSection;
            if (skin != null && !skin.isEmpty() && !skin.equals("default") && this.plugin.getConfig().contains("skins." + skin + ".flags")) {
                flagsSection = this.plugin.getConfig().getConfigurationSection("skins." + skin + ".flags");
                this.plugin.getLogger().info("[VioTrap] Используются флаги скина " + skin + " для трапки " + trapId);
            } else {
                flagsSection = this.plugin.getConfig().getConfigurationSection("trap.flags");
                this.plugin.getLogger().info("[VioTrap] Используются флаги по умолчанию из trap.flags для трапки " + trapId);
            }

            if (flagsSection != null) {
                Map<String, String> flags = new HashMap<>();

                for(String key : flagsSection.getKeys(false)) {
                    flags.put(key, flagsSection.getString(key));
                }

                this.plugin.getLogger().info("[VioTrap] Обработка флагов для трапки " + trapId + ": " + String.valueOf(flags));

                for(Map.Entry<String, String> flagEntry : flags.entrySet()) {
                    String flagName = flagEntry.getKey();
                    String stateValue = flagEntry.getValue().trim().toUpperCase();

                    try {
                        StateFlag flag = (StateFlag)Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                        if (flag == null) {
                            String errorMsg = "[VioTrap] Флаг '" + flagName + "' не найден в WorldGuard для трапки " + trapId;
                            if (player != null) {
                                player.sendMessage("§c" + errorMsg);
                            }

                            this.plugin.getLogger().warning(errorMsg);
                        } else {
                            StateFlag.State state = State.valueOf(stateValue);
                            region.setFlag(flag, state);
                            this.plugin.getLogger().info("[VioTrap] Установлен флаг " + flagName + ": " + stateValue + " для региона " + trapId);
                        }
                    } catch (IllegalArgumentException var24) {
                        String errorMsg = "[VioTrap] Некорректное значение для флага '" + flagName + "' в трапке " + trapId + ": " + stateValue;
                        if (player != null) {
                            player.sendMessage("§c" + errorMsg);
                        }

                        this.plugin.getLogger().warning(errorMsg);
                    }
                }
            } else {
                this.plugin.getLogger().warning("[VioTrap] Секция флагов не найдена для трапки " + trapId);
            }

            region.setPriority(52);
            regionManager.addRegion(region);
            this.activeTraps.put(trapId, region);
            this.plugin.getLogger().info("[VioTrap] Регион трапки " + trapId + " создан");
        }
    }

    public void removeTrapRegion(String trapId, Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager != null) {
            regionManager.removeRegion(trapId);
            this.activeTraps.remove(trapId);
            this.plugin.getLogger().info("[VioTrap] Регион трапки " + trapId + " удалён");
        } else {
            Logger var10000 = this.plugin.getLogger();
            String var10001 = location.getWorld().getName();
            var10000.warning("[VioTrap] RegionManager для мира " + var10001 + " не найден при удалении трапки " + trapId);
        }

    }

    public void removeAllTraps() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for(World world : Bukkit.getWorlds()) {
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                for(String regionName : regionManager.getRegions().keySet()) {
                    if (regionName.endsWith("_trap")) {
                        regionManager.removeRegion(regionName);
                        this.plugin.getLogger().info("[VioTrap] Удалён регион " + regionName + " при очистке всех трапок");
                    }
                }
            }
        }

        this.restoreAllBlocks();
        this.activeTraps.clear();
        this.playersInTrapRegions.clear();
        this.activeTrapTimers.clear();
        this.plugin.getLogger().info("[VioTrap] Все трапки и связанные данные очищены");
    }

    public Map<String, List<CustomAction>> getSkinActions() {
        return this.skinActions;
    }
}