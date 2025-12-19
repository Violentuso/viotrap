//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.data.PlateData;
import org.migrate1337.viotrap.data.TrapBlockData;
import org.migrate1337.viotrap.data.TrapData;
import org.migrate1337.viotrap.items.PlateItem;
import org.migrate1337.viotrap.items.TrapItem;
import org.migrate1337.viotrap.utils.ActiveSkinsManager;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.PVPManagerHandle;

public class PlateItemListener implements Listener {
    private final VioTrap plugin;
    private final Map<UUID, Map<Location, TrapBlockData>> playerReplacedBlocks = new HashMap();
    private final Map<String, ProtectedCuboidRegion> activePlates = new HashMap();
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;
    private final ActiveSkinsManager activeSkinsManager;
    private final Map<String, PlateData> activePlateTimers = new HashMap();

    public PlateItemListener(VioTrap plugin) {
        this.plugin = plugin;
        this.combatLogXHandler = new CombatLogXHandler();
        this.pvpManagerHandler = new PVPManagerHandle(plugin);
        this.activeSkinsManager = plugin.getActiveSkinsManager();
        plugin.getLogger().info("[VioTrap] PlateItemListener initialized");
        this.loadPlatesFromConfig();
    }

    @EventHandler
    public void onPlayerUsePlate(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && PlateItem.getUniqueId(item) != null && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (this.plugin.getConfig().getBoolean("plate.enable-pvp", false)) {
                this.plugin.getLogger().info("[VioTrap] PVP enabled for plate usage by " + player.getName());
                if (this.combatLogXHandler.isCombatLogXEnabled()) {
                    this.combatLogXHandler.tagPlayer(player, TagType.DAMAGE, TagReason.ATTACKER);
                    player.sendMessage(this.plugin.getConfig().getString("plate.messages.pvp-enabled", "§cВы использовали пласт и получили режим пвп!"));
                }

                if (this.pvpManagerHandler.isPvPManagerEnabled()) {
                    this.pvpManagerHandler.tagPlayerForPvP(player, "plate");
                    player.sendMessage(this.plugin.getConfig().getString("plate.messages.pvp-enabled", "§cВы использовали пласт и получили режим пвп!"));
                }
            }

            Location location = player.getLocation();
            String skin = this.activeSkinsManager.getActivePlateSkin(player.getUniqueId()); // ← новый метод!
            if (skin == null || skin.isEmpty() || skin.equals("default")) {
                skin = "default";
            }

            org.migrate1337.viotrap.listeners.DirectionInfo directionInfo = this.getOffsetsAndSchematic(player, skin);
            String schematic = directionInfo.schematicName;
            Logger var10000 = this.plugin.getLogger();
            String var10001 = player.getName();
            var10000.info("[VioTrap] Selected schematic for " + var10001 + ": " + schematic);
            if (!this.plugin.getConfig().contains("plate_skins." + skin) && !skin.equals("default")) {
                this.plugin.getLogger().warning("[VioTrap] Skin " + skin + " not found in configuration for " + player.getName());
                player.sendMessage("§cСкин не найден в конфигурации.");
            } else if (this.isInBannedRegion(location, location.getWorld().getName())) {
                var10000 = this.plugin.getLogger();
                var10001 = player.getName();
                var10000.info("[VioTrap] Player " + var10001 + " attempted to use plate in banned region at " + String.valueOf(location));
                player.sendMessage("§cВы не можете использовать данный предмет в этом регионе!");
            } else if (this.hasBannedRegionFlags(location, location.getWorld().getName())) {
                var10000 = this.plugin.getLogger();
                var10001 = player.getName();
                var10000.info("[VioTrap] Player " + var10001 + " attempted to use plate in region with banned flags at " + String.valueOf(location));
                player.sendMessage("§cВы не можете использовать данный предмет в этом регионе!");
            } else if (player.hasCooldown(item.getType())) {
                var10000 = this.plugin.getLogger();
                var10001 = player.getName();
                var10000.info("[VioTrap] Player " + var10001 + " is on cooldown for " + String.valueOf(item.getType()));
                player.sendMessage(this.plugin.getConfig().getString("plate.messages.cooldown_message", "§cПодождите перед использованием снова!"));
            } else if (this.isRegionNearby(location, player.getWorld().getName())) {
                var10000 = this.plugin.getLogger();
                var10001 = player.getName();
                var10000.info("[VioTrap] Player " + var10001 + " attempted to use plate near existing plate/trap at " + String.valueOf(location));
                player.sendMessage(this.plugin.getConfig().getString("plate.messages.already_nearby", "Рядом уже установлен активный пласт или трапка!"));
            } else {
                String sound = skin.equals("default") ? this.plugin.getConfig().getString("plate.sound.type", "BLOCK_ANVIL_PLACE") : this.plugin.getConfig().getString("plate_skins." + skin + ".sound.type", "BLOCK_ANVIL_PLACE");
                float soundVolume = skin.equals("default") ? (float)this.plugin.getConfig().getDouble("plate.sound.volume", (double)10.0F) : (float)this.plugin.getConfig().getDouble("plate_skins." + skin + ".sound.volume", (double)1.0F);
                float soundPitch = skin.equals("default") ? (float)this.plugin.getConfig().getDouble("plate.sound.pitch", (double)1.0F) : (float)this.plugin.getConfig().getDouble("plate_skins." + skin + ".sound.pitch", (double)1.0F);
                this.plugin.getLogger().info("[VioTrap] Playing sound " + sound + " for " + player.getName() + " at volume " + soundVolume + ", pitch " + soundPitch);
                Map<Location, TrapBlockData> replaced = (Map)this.playerReplacedBlocks.get(player.getUniqueId());
                int durationTicks = (skin.equals("default") ? this.plugin.getConfig().getInt("plate.duration", 5) : this.plugin.getConfig().getInt("plate_skins." + skin + ".duration", 5)) * 20;
                if (replaced != null) {
                    for(Location loc : replaced.keySet()) {
                        this.savePlateToConfig(player, loc, skin, (long)(durationTicks / 20));
                    }
                }

                try {
                    player.playSound(location, Sound.valueOf(sound), soundVolume, soundPitch);
                } catch (IllegalArgumentException var24) {
                    this.plugin.getLogger().warning("[VioTrap] Invalid sound type " + sound + " for skin " + skin);
                    player.sendMessage("§cОшибка: некорректный звук в конфигурации скина.");
                    return;
                }

                int cooldownTicks = (skin.equals("default") ? this.plugin.getConfig().getInt("plate.cooldown", 20) : this.plugin.getConfig().getInt("plate_skins." + skin + ".cooldown", 20)) * 20;
                player.setCooldown(item.getType(), cooldownTicks);
                item.setAmount(item.getAmount() - 1);

                try {
                    File schematicFile = new File("plugins/WorldEdit/schematics/" + directionInfo.schematicName);
                    this.plugin.getLogger().info("[VioTrap] Loading schematic: " + schematicFile.getAbsolutePath());
                    if (!schematicFile.exists()) {
                        this.plugin.getLogger().warning("[VioTrap] Schematic file does not exist: " + schematicFile.getAbsolutePath());
                        player.sendMessage(this.plugin.getConfig().getString("plate.messages.placement_failed", "Не удалось загрузить пласт!"));
                        return;
                    }

                    Clipboard clipboard;
                    try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                        clipboard = reader.read();
                    } catch (Exception e) {
                        var10000 = this.plugin.getLogger();
                        var10001 = schematicFile.getAbsolutePath();
                        var10000.warning("[VioTrap] Failed to load schematic " + var10001 + ": " + e.getMessage());
                        player.sendMessage(this.plugin.getConfig().getString("plate.messages.placement_failed", "Не удалось загрузить пласт!"));
                        return;
                    }
                    BlockVector3 minPoint = clipboard.getRegion().getMinimumPoint();
                    BlockVector3 maxPoint = clipboard.getRegion().getMaximumPoint();
                    BlockVector3 origin = clipboard.getOrigin();

                    int pos1X = minPoint.getBlockX() - origin.getBlockX();
                    int pos1Y = minPoint.getBlockY() - origin.getBlockY();
                    int pos1Z = minPoint.getBlockZ() - origin.getBlockZ();
                    int pos2X = maxPoint.getBlockX() - origin.getBlockX();
                    int pos2Y = maxPoint.getBlockY() - origin.getBlockY();
                    int pos2Z = maxPoint.getBlockZ() - origin.getBlockZ();

                    this.plugin.getLogger().info("[VioTrap] Computed region offsets: pos1=(" + pos1X + "," + pos1Y + "," + pos1Z + "), pos2=(" + pos2X + "," + pos2Y + "," + pos2Z + ")");
                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
                        BlockVector3 pastePosition = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                        ClipboardHolder holder = new ClipboardHolder(clipboard);
                        Operations.complete(holder.createPaste(editSession).to(pastePosition).ignoreAirBlocks(true).build());
                        if (!skin.equals("default")) {
                            int points = plugin.getSkinPointsManager().getPoints(player.getUniqueId(), skin);
                            if (points > 1) {
                                plugin.getSkinPointsManager().removePoints(player.getUniqueId(), skin, 1);
                            } else if (points == 1) {
                                plugin.getSkinPointsManager().removePoints(player.getUniqueId(), skin, 1);
                                this.activeSkinsManager.setActivePlateSkin(player.getUniqueId(), "default");
                            }
                        }
                        var10000 = this.plugin.getLogger();
                        var10001 = player.getName();
                        var10000.info("[VioTrap] Pasted schematic for " + var10001 + " at " + String.valueOf(location));
                        this.saveReplacedBlocks(player.getUniqueId(), location, clipboard);
                        this.createPlateRegion(player, location, pos1X, pos1Y, pos1Z, pos2X, pos2Y, pos2Z);
                        player.sendMessage(this.plugin.getConfig().getString("plate.messages.success_used", "§aВы успешно использовали предмет."));

                        String var36 = String.valueOf(player.getUniqueId());
                        String plateId = "plate_" + var36 + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
                        this.activePlateTimers.put(plateId, new PlateData(player.getUniqueId(), location, skin, System.currentTimeMillis() + (long)durationTicks * 50L));
                        String finalSkin = skin;
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                            this.activePlateTimers.remove(plateId);
                            Logger var10002 = this.plugin.getLogger();
                            String var10003 = player.getName();
                            var10002.info("[VioTrap] Restoring blocks for " + var10003 + " at " + String.valueOf(location));
                            this.restoreBlocks(player.getUniqueId());
                            this.removePlateRegion(player, location);
                            this.removePlateFromFile(location);
                            String soundEnded = finalSkin.equals("default") ? this.plugin.getConfig().getString("plate.sound.type-ended", "BLOCK_PISTON_EXTEND") : this.plugin.getConfig().getString("plate_skins." + finalSkin + ".sound.type-ended", "BLOCK_PISTON_EXTEND");
                            float soundVolumeEnded = finalSkin.equals("default") ? (float)this.plugin.getConfig().getDouble("plate.sound.volume-ended", (double)10.0F) : (float)this.plugin.getConfig().getDouble("plate_skins." + finalSkin + ".sound.volume-ended", (double)1.0F);
                            float soundPitchEnded = finalSkin.equals("default") ? (float)this.plugin.getConfig().getDouble("plate.sound.pitch-ended", (double)1.0F) : (float)this.plugin.getConfig().getDouble("plate_skins." + finalSkin + ".sound.pitch-ended", (double)1.0F);

                            try {
                                player.playSound(location, Sound.valueOf(soundEnded), soundVolumeEnded, soundPitchEnded);
                            } catch (IllegalArgumentException var9) {
                                this.plugin.getLogger().warning("[VioTrap] Invalid end sound type " + soundEnded + " for skin " + finalSkin);
                            }

                        }, (long)durationTicks);
                    }
                } catch (Exception e) {
                    var10000 = this.plugin.getLogger();
                    var10001 = player.getName();
                    var10000.warning("[VioTrap] Failed to place plate for " + var10001 + ": " + e.getMessage());
                    e.printStackTrace();
                    player.sendMessage(this.plugin.getConfig().getString("plate.messages.placement_failed", "Не удалось загрузить пласт!"));
                }

            }
        }
    }

    private void replaceSkinnedPlatesWithNewSkin(Player player, String oldSkin, String newSkin) {
        Inventory inventory = player.getInventory();

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack item = inventory.getItem(i);
            if (item != null && PlateItem.getUniqueId(item) != null && oldSkin.equals(PlateItem.getSkin(item))) {
                int amount = item.getAmount();
                inventory.setItem(i, PlateItem.getPlateItem(amount, newSkin));
            }
        }

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack item = inventory.getItem(i);
            if (item != null && TrapItem.isTrapItem(item) && oldSkin.equals(TrapItem.getSkin(item))) {
                int amount = item.getAmount();
                inventory.setItem(i, TrapItem.getTrapItem(amount, newSkin));
            }
        }

        BukkitScheduler var10000 = Bukkit.getScheduler();
        VioTrap var10001 = this.plugin;
        Objects.requireNonNull(player);
        var10000.runTaskLater(var10001, player::updateInventory, 1L);
    }

    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager == null) {
            this.plugin.getLogger().info("[VioTrap] No region manager for world " + worldName);
            return false;
        } else if (this.plugin.getConfig().getBoolean("plate.disabled_all_regions", false)) {
            boolean inRegion = regionManager.getApplicableRegions(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ())).getRegions().stream().anyMatch((region) -> !"__default__".equals(region.getId()));
            Logger var9 = this.plugin.getLogger();
            String var10 = String.valueOf(location);
            var9.info("[VioTrap] Checking banned regions (disabled_all_regions=true) at " + var10 + ": " + inRegion);
            return inRegion;
        } else {
            List<String> bannedRegions = this.plugin.getConfig().getStringList("plate.banned_regions");
            BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            boolean inBannedRegion = regionManager.getApplicableRegions(vector).getRegions().stream().anyMatch((region) -> bannedRegions.contains(region.getId()));
            Logger var10000 = this.plugin.getLogger();
            String var10001 = String.valueOf(bannedRegions);
            var10000.info("[VioTrap] Checking banned regions " + var10001 + " at " + String.valueOf(location) + ": " + inBannedRegion);
            return inBannedRegion;
        }
    }

    private boolean hasBannedRegionFlags(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager == null) {
            this.plugin.getLogger().info("[VioTrap] No region manager for world " + worldName);
            return false;
        } else {
            ConfigurationSection bannedFlagsSection = this.plugin.getConfig().getConfigurationSection("plate.banned_region_flags");
            if (bannedFlagsSection == null) {
                this.plugin.getLogger().info("[VioTrap] No banned region flags configured");
                return false;
            } else {
                BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

                for(ProtectedRegion region : regionManager.getApplicableRegions(vector).getRegions()) {
                    for(String flagName : bannedFlagsSection.getKeys(false)) {
                        StateFlag flag = (StateFlag)Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                        if (flag == null) {
                            this.plugin.getLogger().warning("[VioTrap] Invalid flag " + flagName + " in banned_region_flags");
                        } else if (region.getFlag(flag) != null) {
                            this.plugin.getLogger().info("[VioTrap] Found banned flag " + flagName + " in region " + region.getId() + " at " + String.valueOf(location));
                            return true;
                        }
                    }
                }

                this.plugin.getLogger().info("[VioTrap] No banned flags found at " + String.valueOf(location));
                return false;
            }
        }
    }

    public void removeAllPlates() {
        this.plugin.getLogger().info("[VioTrap] Removing all plates");
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for(World world : Bukkit.getWorlds()) {
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                for(String regionName : regionManager.getRegions().keySet()) {
                    if (regionName.endsWith("plate_")) {
                        regionManager.removeRegion(regionName);
                        this.plugin.getLogger().info("[VioTrap] Removed region " + regionName);
                    }
                }
            }
        }

        this.restoreAllBlocks();
        this.activePlates.clear();
        this.plugin.getLogger().info("[VioTrap] All plates removed");
    }

    private boolean isRegionNearby(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager == null) {
            this.plugin.getLogger().info("[VioTrap] No region manager for world " + worldName);
            return false;
        } else {
            BlockVector3 min = BlockVector3.at(location.getBlockX() - 3, location.getBlockY() - 3, location.getBlockZ() - 3);
            BlockVector3 max = BlockVector3.at(location.getBlockX() + 3, location.getBlockY() + 3, location.getBlockZ() + 3);
            ProtectedCuboidRegion checkRegion = new ProtectedCuboidRegion("checkRegion", min, max);
            boolean nearby = regionManager.getApplicableRegions(checkRegion).getRegions().stream().anyMatch((region) -> region.getId().endsWith("_trap") || region.getId().startsWith("plate_"));
            Logger var10000 = this.plugin.getLogger();
            String var10001 = String.valueOf(location);
            var10000.info("[VioTrap] Checking nearby regions at " + var10001 + ": " + nearby);
            return nearby;
        }
    }

    private org.migrate1337.viotrap.listeners.DirectionInfo getOffsetsAndSchematic(Player player, String skin) {
        float yaw = player.getLocation().getYaw() % 360.0F;
        float pitch = player.getLocation().getPitch();
        if (yaw > 180.0F) {
            yaw -= 360.0F;
        }
        if (yaw < -180.0F) {
            yaw += 360.0F;
        }

        String configPath = skin.equals("default") ? "plate" : "plate_skins." + skin;
        String schematicName;

        if (pitch < -45.0F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".up_schematic", "plate_up.schem");
        } else if (pitch > 45.0F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".down_schematic", "plate.schem");
        } else if ((double)yaw >= (double)-22.5F && (double)yaw <= (double)22.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".forward_schematic", "plate_forward.schem");
        } else if ((double)yaw > (double)22.5F && (double)yaw <= (double)67.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".forward_left_schematic", "plate_forward_left.schem");
        } else if ((double)yaw > (double)-67.5F && (double)yaw <= (double)-22.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".forward_right_schematic", "plate_forward_right.schem");
        } else if ((double)yaw > (double)67.5F && (double)yaw <= (double)112.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".left_schematic", "plate_left.schem");
        } else if ((double)yaw > (double)112.5F && (double)yaw <= (double)157.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".backward_left_schematic", "plate_backward_left.schem");
        } else if ((double)yaw < (double)-112.5F && (double)yaw >= (double)-157.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".backward_right_schematic", "plate_backward_right.schem");
        } else if ((double)yaw > (double)157.5F && yaw <= 180.0F || (double)yaw < (double)-157.5F && yaw >= -180.0F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".backward_schematic", "plate_backward.schem");
        } else {
            schematicName = this.plugin.getConfig().getString(configPath + ".right_schematic", "plate_right.schem");
        }

        // angX/Y/Z оставляем, если они используются где-то ещё (в вашем коде они не используются для региона)
        int angX = 0; // Можно удалить, если не нужны
        int angY = 0;
        int angZ = 0;

        // Теперь DirectionInfo не содержит pos1/pos2, их вычислим позже
        return new org.migrate1337.viotrap.listeners.DirectionInfo(0, 0, 0, angX, angY, angZ, 0, 0, 0, 0, 0, 0, schematicName);
    }



    private void createPlateRegion(Player player, Location location, int pos1X, int pos1Y, int pos1Z, int pos2X, int pos2Y, int pos2Z) {
        String regionName = "plate_" + player.getName();
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, BlockVector3.at(location.getBlockX() + pos1X, location.getBlockY() + pos1Y, location.getBlockZ() + pos1Z), BlockVector3.at(location.getBlockX() + pos2X, location.getBlockY() + pos2Y, location.getBlockZ() + pos2Z));
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        region.setPriority(52);
        if (regionManager != null) {
            regionManager.addRegion(region);
            this.activePlates.put(regionName, region);
            this.plugin.getLogger().info("[VioTrap] Created region " + regionName + " at " + String.valueOf(location) + ", bounds: min=(" + (location.getBlockX() + pos1X) + "," + (location.getBlockY() + pos1Y) + "," + (location.getBlockZ() + pos1Z) + "), max=(" + (location.getBlockX() + pos2X) + "," + (location.getBlockY() + pos2Y) + "," + (location.getBlockZ() + pos2Z) + ")");
        } else {
            this.plugin.getLogger().warning("[VioTrap] Failed to create region " + regionName + ": no region manager for world " + location.getWorld().getName());
        }

        ConfigurationSection flagsSection = this.plugin.getConfig().getConfigurationSection("plate.flags");
        if (flagsSection != null) {
            for(String flagName : flagsSection.getKeys(false)) {
                try {
                    StateFlag flag = (StateFlag)Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                    if (flag != null) {
                        String value = flagsSection.getString(flagName);
                        StateFlag.State state = State.valueOf(value.toUpperCase());
                        region.setFlag(flag, state);
                        this.plugin.getLogger().info("[VioTrap] Set flag " + flagName + "=" + String.valueOf(state) + " for region " + regionName);
                    }
                } catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("[VioTrap] Invalid flag value for " + flagName + ": " + e.getMessage());
                    player.sendMessage("§cНекорректное значение для флага " + flagName + " в конфиге.");
                }
            }
        }

    }

    private void removePlateRegion(Player player, Location location) {
        String regionName = "plate_" + player.getName();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager != null) {
            regionManager.removeRegion(regionName);
            this.activePlates.remove(regionName);
            this.plugin.getLogger().info("[VioTrap] Removed region " + regionName + " at " + String.valueOf(location));
        } else {
            this.plugin.getLogger().warning("[VioTrap] Failed to remove region " + regionName + ": no region manager for world " + location.getWorld().getName());
        }

    }

    public void restoreAllBlocks() {
        this.plugin.getLogger().info("[VioTrap] Restoring all blocks");
        if (this.playerReplacedBlocks.isEmpty()) {
            this.plugin.getLogger().info("[VioTrap] playerReplacedBlocks is empty, loading from config");
            this.loadPlatesFromConfig();
        }

        for(UUID playerId : new HashSet<UUID>(this.playerReplacedBlocks.keySet())) {
            this.restoreBlocks(playerId);
        }

        this.plugin.getLogger().info("[VioTrap] All blocks restored");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.plugin.getLogger().info("[VioTrap] Player " + event.getPlayer().getName() + " quit, restoring blocks");
        this.restoreBlocks(playerId);
        this.playerReplacedBlocks.remove(playerId);
    }

    private void saveReplacedBlocks(UUID playerId, Location startLocation, Clipboard clipboard) {
        Map<Location, TrapBlockData> replacedBlocks = new HashMap<>();
        BlockVector3 origin = clipboard.getOrigin();

        int offsetX = startLocation.getBlockX() - origin.getBlockX();
        int offsetY = startLocation.getBlockY() - origin.getBlockY();
        int offsetZ = startLocation.getBlockZ() - origin.getBlockZ();

        for (BlockVector3 vec : clipboard.getRegion()) {
            // Пропускаем воздух — его не нужно сохранять
            if (clipboard.getBlock(vec).getBlockType().getMaterial().isAir()) {
                continue;
            }

            Location worldLoc = new Location(
                    startLocation.getWorld(),
                    vec.getBlockX() + offsetX,
                    vec.getBlockY() + offsetY,
                    vec.getBlockZ() + offsetZ
            );

            Block block = worldLoc.getBlock();
            BlockState state = block.getState();

            TrapBlockData oldData = new TrapBlockData(
                    block.getType(),
                    block.getBlockData(),
                    null,
                    null
            );

            // === Сохранение содержимого контейнеров ===
            if (state instanceof Container) {
                Container container = (Container) state;
                Inventory inventory = container.getInventory();
                InventoryHolder holder = inventory.getHolder();

                if (holder instanceof DoubleChest) {
                    DoubleChest doubleChest = (DoubleChest) holder;
                    oldData.setDoubleChest(true);
                    oldData.setContents(inventory.getContents().clone());

                    // Сохраняем пару только для левой стороны
                    if (doubleChest.getLeftSide() == holder) {
                        InventoryHolder right = doubleChest.getRightSide();
                        if (right instanceof Chest) {
                            oldData.setPairedChestLocation(((Chest) right).getLocation());
                        }
                    }
                } else {
                    oldData.setContents(inventory.getContents().clone());
                }
            }

            // === Сохранение спавнеров ===
            if (state instanceof CreatureSpawner) {
                CreatureSpawner spawner = (CreatureSpawner) state;
                EntityType type = spawner.getSpawnedType();
                if (type != null) {
                    oldData.setSpawnedType(type.name());
                }
            }

            replacedBlocks.put(worldLoc.clone(), oldData);
        }

        this.playerReplacedBlocks.put(playerId, replacedBlocks);
        this.plugin.getLogger().info("[VioTrap] Сохранено " + replacedBlocks.size() + " реально изменённых блоков для " + playerId);
    }

    private void restoreBlocks(UUID playerId) {
        Map<Location, TrapBlockData> replacedBlocks = this.playerReplacedBlocks.get(playerId);
        if (replacedBlocks == null || replacedBlocks.isEmpty()) {
            this.plugin.getLogger().info("[VioTrap] Нет блоков для восстановления у " + playerId);
            return;
        }

        int restoredCount = 0;

        for (Map.Entry<Location, TrapBlockData> entry : replacedBlocks.entrySet()) {
            Location loc = entry.getKey();
            TrapBlockData oldData = entry.getValue();

            Block currentBlock = loc.getBlock();

            // Проверяем, изменился ли блок вообще
            boolean blockSame = currentBlock.getType() == oldData.getMaterial() &&
                    currentBlock.getBlockData().matches(oldData.getBlockData());

            boolean contentsSame = true;
            ItemStack[] savedContents = oldData.getContents();

            if (savedContents != null && currentBlock.getState() instanceof Container) {
                Container currentContainer = (Container) currentBlock.getState();
                ItemStack[] currentContents = currentContainer.getInventory().getContents();

                contentsSame = java.util.Arrays.equals(currentContents, savedContents);
            }

            // Если блок и содержимое не изменились — пропускаем восстановление
            if (blockSame && contentsSame) {
                this.removePlateFromFile(loc);
                continue;
            }

            // === Восстанавливаем только если нужно ===
            currentBlock.setType(oldData.getMaterial());
            currentBlock.setBlockData(oldData.getBlockData());

            BlockState state = currentBlock.getState();

            // Восстановление спавнера
            if (state instanceof CreatureSpawner && oldData.getSpawnedType() != null) {
                CreatureSpawner spawner = (CreatureSpawner) state;
                try {
                    spawner.setSpawnedType(EntityType.valueOf(oldData.getSpawnedType()));
                    spawner.update(true);
                } catch (Exception ignored) {}
            }

            // Восстановление содержимого сундуков
            if (savedContents != null && state instanceof Container) {
                Container container = (Container) state;
                Inventory inv = container.getInventory();

                if (inv.getHolder() instanceof DoubleChest) {
                    inv.setContents(savedContents);
                } else {
                    // Одиночный сундук — обрезаем до 27 слотов
                    ItemStack[] trimmed = new ItemStack[27];
                    System.arraycopy(savedContents, 0, trimmed, 0, Math.min(savedContents.length, 27));
                    inv.setContents(trimmed);
                }
            }

            restoredCount++;
            this.removePlateFromFile(loc);
        }

        this.playerReplacedBlocks.remove(playerId);
        this.plugin.getLogger().info("[VioTrap] Восстановлено только " + restoredCount + " изменённых блоков для " + playerId + " (остальные остались как есть)");
    }

    private void loadPlatesFromConfig() {
        this.plugin.getLogger().info("[VioTrap] Loading plates from plats.yml");
        if (!this.plugin.getPlatesConfig().contains("plates")) {
            this.plugin.getLogger().info("[VioTrap] No plates found in plats.yml");
        } else {
            ConfigurationSection platesSection = this.plugin.getPlatesConfig().getConfigurationSection("plates");

            for(String worldName : platesSection.getKeys(false)) {
                ConfigurationSection worldSection = platesSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    for(String plateKey : worldSection.getKeys(false)) {
                        ConfigurationSection plateSection = worldSection.getConfigurationSection(plateKey);
                        if (plateSection != null) {
                            UUID playerId = UUID.fromString(plateSection.getString("player"));
                            String world = plateSection.getString("world");
                            int x = plateSection.getInt("x");
                            int y = plateSection.getInt("y");
                            int z = plateSection.getInt("z");
                            String skin = plateSection.getString("skin", "default");
                            long endTime = plateSection.getLong("endTime", 0L);
                            Location location = new Location(Bukkit.getWorld(world), (double)x, (double)y, (double)z);
                            Block block = location.getBlock();
                            TrapBlockData blockData = new TrapBlockData(block.getType(), block.getBlockData(), (ItemStack[])null, (String)null);
                            this.playerReplacedBlocks.putIfAbsent(playerId, new HashMap());
                            ((Map)this.playerReplacedBlocks.get(playerId)).put(location, blockData);
                            Logger var10000 = this.plugin.getLogger();
                            String var10001 = String.valueOf(location);
                            var10000.info("[VioTrap] Loaded plate at " + var10001 + " for player " + String.valueOf(playerId));
                            long currentTime = System.currentTimeMillis();
                            long remainingMillis = endTime - currentTime;
                            if (remainingMillis > 0L) {
                                String plateId = "plate_" + String.valueOf(playerId) + "_" + x + "_" + y + "_" + z;
                                this.activePlateTimers.put(plateId, new PlateData(playerId, location, skin, endTime));
                                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                    PlateData data = (PlateData)this.activePlateTimers.remove(plateId);
                                    if (data != null) {
                                        this.restoreBlocks(data.getPlayerId());
                                        this.removePlateRegion(Bukkit.getPlayer(data.getPlayerId()), data.getLocation());
                                        this.removePlateFromFile(data.getLocation());
                                        Location loc = data.getLocation();
                                        String soundEnded = data.getSkin().equals("default") ? this.plugin.getConfig().getString("plate.sound.type-ended", "BLOCK_PISTON_EXTEND") : this.plugin.getConfig().getString("plate_skins." + data.getSkin() + ".sound.type-ended", "BLOCK_PISTON_EXTEND");
                                        float soundVolumeEnded = skin.equals("default") ? (float)this.plugin.getConfig().getDouble("plate.sound.volume-ended", (double)10.0F) : (float)this.plugin.getConfig().getDouble("plate_skins." + skin + ".sound.volume-ended", (double)1.0F);
                                        float soundPitchEnded = skin.equals("default") ? (float)this.plugin.getConfig().getDouble("plate.sound.pitch-ended", (double)1.0F) : (float)this.plugin.getConfig().getDouble("plate_skins." + skin + ".sound.pitch-ended", (double)1.0F);
                                        loc.getWorld().playSound(loc, Sound.valueOf(soundEnded), soundVolumeEnded, soundPitchEnded);
                                    }

                                }, remainingMillis / 50L);
                            } else {
                                this.restoreBlocks(playerId);
                                this.removePlateFromFile(location);
                            }
                        }
                    }
                }
            }

            this.plugin.getLogger().info("[VioTrap] Loaded " + this.playerReplacedBlocks.size() + " active plates");
        }
    }

    private void savePlateToConfig(Player player, Location location, String skin, long durationSeconds) {
        String var10000 = location.getWorld().getName();
        String path = "plates." + var10000 + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        this.plugin.getPlatesConfig().set(path + ".player", player.getUniqueId().toString());
        this.plugin.getPlatesConfig().set(path + ".world", location.getWorld().getName());
        this.plugin.getPlatesConfig().set(path + ".x", location.getBlockX());
        this.plugin.getPlatesConfig().set(path + ".y", location.getBlockY());
        this.plugin.getPlatesConfig().set(path + ".z", location.getBlockZ());
        this.plugin.getPlatesConfig().set(path + ".skin", skin);
        this.plugin.getPlatesConfig().set(path + ".endTime", System.currentTimeMillis() + durationSeconds * 1000L);
        this.plugin.savePlatesConfig();
        Logger var7 = this.plugin.getLogger();
        String var10001 = String.valueOf(location);
        var7.info("[VioTrap] Saved plate to config at " + var10001 + " for " + player.getName() + " with skin " + skin);
    }

    private void removePlateFromFile(Location location) {
        String var10000 = location.getWorld().getName();
        String path = "plates." + var10000 + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        this.plugin.getPlatesConfig().set(path, (Object)null);
        this.plugin.savePlatesConfig();
        this.plugin.getLogger().info("[VioTrap] Removed plate from config at " + String.valueOf(location));
    }
}
