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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
import org.migrate1337.viotrap.items.PlateItem;
import org.migrate1337.viotrap.items.TrapItem;
import org.migrate1337.viotrap.utils.ActiveSkinsManager;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.GlobalTrapRegistry;
import org.migrate1337.viotrap.utils.PVPManagerHandle;

public class PlateItemListener implements Listener {
    private final VioTrap plugin;
    private final Map<UUID, Map<Location, TrapBlockData>> playerReplacedBlocks = new HashMap<>();
    private final Map<String, ProtectedCuboidRegion> activePlates = new HashMap<>();
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;
    private final ActiveSkinsManager activeSkinsManager;
    private final Map<String, PlateData> activePlateTimers = new HashMap<>();

    public PlateItemListener(VioTrap plugin) {
        this.plugin = plugin;
        this.combatLogXHandler = new CombatLogXHandler();
        this.pvpManagerHandler = new PVPManagerHandle(plugin);
        this.activeSkinsManager = plugin.getActiveSkinsManager();
        this.loadPlatesFromConfig();
    }

    @EventHandler
    public void onPlayerUsePlate(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && PlateItem.getUniqueId(item) != null && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (!plugin.getConditionManager().checkConditions(player, "trap")) {
                return;
            }
            if (this.plugin.getConfig().getBoolean("plate.enable-pvp", false)) {
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
            String skin = this.activeSkinsManager.getActivePlateSkin(player.getUniqueId());
            if (skin == null || skin.isEmpty() || skin.equals("default")) {
                skin = "default";
            }

            org.migrate1337.viotrap.listeners.DirectionInfo directionInfo = this.getOffsetsAndSchematic(player, skin);
            if (!this.plugin.getConfig().contains("plate_skins." + skin) && !skin.equals("default")) {
                player.sendMessage("§cСкин не найден в конфигурации.");
            } else if (this.isInBannedRegion(location, location.getWorld().getName())) {
                player.sendMessage("§cВы не можете использовать данный предмет в этом регионе!");
            } else if (this.hasBannedRegionFlags(location, location.getWorld().getName())) {
                player.sendMessage("§cВы не можете использовать данный предмет в этом регионе!");
            } else if (player.hasCooldown(item.getType())) {
                player.sendMessage(this.plugin.getConfig().getString("plate.messages.cooldown_message", "§cПодождите перед использованием снова!"));
            }  else {
                String sound = skin.equals("default") ? this.plugin.getConfig().getString("plate.sound.type", "BLOCK_ANVIL_PLACE") : this.plugin.getConfig().getString("plate_skins." + skin + ".sound.type", "BLOCK_ANVIL_PLACE");
                float soundVolume = skin.equals("default") ? (float) this.plugin.getConfig().getDouble("plate.sound.volume", (double) 10.0F) : (float) this.plugin.getConfig().getDouble("plate_skins." + skin + ".sound.volume", (double) 1.0F);
                float soundPitch = skin.equals("default") ? (float) this.plugin.getConfig().getDouble("plate.sound.pitch", (double) 1.0F) : (float) this.plugin.getConfig().getDouble("plate_skins." + skin + ".sound.pitch", (double) 1.0F);

                int durationTicks = (skin.equals("default") ? this.plugin.getConfig().getInt("plate.duration", 5) : this.plugin.getConfig().getInt("plate_skins." + skin + ".duration", 5)) * 20;

                try {
                    player.playSound(location, Sound.valueOf(sound), soundVolume, soundPitch);
                } catch (IllegalArgumentException var24) {
                    player.sendMessage("§cОшибка: некорректный звук в конфигурации скина.");
                    return;
                }

                int cooldownTicks = (skin.equals("default") ? this.plugin.getConfig().getInt("plate.cooldown", 20) : this.plugin.getConfig().getInt("plate_skins." + skin + ".cooldown", 20)) * 20;
                player.setCooldown(item.getType(), cooldownTicks);
                item.setAmount(item.getAmount() - 1);

                try {
                    File schematicFile = new File("plugins/WorldEdit/schematics/" + directionInfo.schematicName);
                    if (!schematicFile.exists()) {
                        player.sendMessage(this.plugin.getConfig().getString("plate.messages.placement_failed", "Не удалось загрузить пласт!"));
                        return;
                    }

                    Clipboard clipboard;
                    try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                        clipboard = reader.read();
                    } catch (Exception e) {
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

                    String plateId = "plate_" + player.getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();

                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
                        BlockVector3 pastePosition = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

                        // ! ВАЖНО: Сохранение через реестр
                        this.saveReplacedBlocks(player.getUniqueId(), location, clipboard, plateId);

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

                        this.createPlateRegion(player, location, pos1X, pos1Y, pos1Z, pos2X, pos2Y, pos2Z);
                        player.sendMessage(this.plugin.getConfig().getString("plate.messages.success_used", "§aВы успешно использовали предмет."));

                        this.activePlateTimers.put(plateId, new PlateData(player.getUniqueId(), location, skin, System.currentTimeMillis() + (long) durationTicks * 50L));
                        this.savePlateToConfig(player, location, skin, (long) (durationTicks / 20));

                        String finalSkin = skin;
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                            this.activePlateTimers.remove(plateId);

                            // ! ВАЖНО: Восстановление через реестр
                            this.restoreBlocks(player.getUniqueId(), plateId);

                            this.removePlateRegion(player, location);
                            this.removePlateFromFile(location);
                            String soundEnded = finalSkin.equals("default") ? this.plugin.getConfig().getString("plate.sound.type-ended", "BLOCK_PISTON_EXTEND") : this.plugin.getConfig().getString("plate_skins." + finalSkin + ".sound.type-ended", "BLOCK_PISTON_EXTEND");
                            float soundVolumeEnded = finalSkin.equals("default") ? (float) this.plugin.getConfig().getDouble("plate.sound.volume-ended", (double) 10.0F) : (float) this.plugin.getConfig().getDouble("plate_skins." + finalSkin + ".sound.volume-ended", (double) 1.0F);
                            float soundPitchEnded = finalSkin.equals("default") ? (float) this.plugin.getConfig().getDouble("plate.sound.pitch-ended", (double) 1.0F) : (float) this.plugin.getConfig().getDouble("plate_skins." + finalSkin + ".sound.pitch-ended", (double) 1.0F);

                            try {
                                player.playSound(location, Sound.valueOf(soundEnded), soundVolumeEnded, soundPitchEnded);
                            } catch (IllegalArgumentException var9) {
                            }

                        }, (long) durationTicks);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    player.sendMessage(this.plugin.getConfig().getString("plate.messages.placement_failed", "Не удалось загрузить пласт!"));
                }
            }
        }
    }

    private void replaceSkinnedPlatesWithNewSkin(Player player, String oldSkin, String newSkin) {
        Inventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getSize(); ++i) {
            ItemStack item = inventory.getItem(i);
            if (item != null && PlateItem.getUniqueId(item) != null && oldSkin.equals(PlateItem.getSkin(item))) {
                int amount = item.getAmount();
                inventory.setItem(i, PlateItem.getPlateItem(amount, newSkin));
            }
        }

        for (int i = 0; i < inventory.getSize(); ++i) {
            ItemStack item = inventory.getItem(i);
            if (item != null && TrapItem.isTrapItem(item) && oldSkin.equals(TrapItem.getSkin(item))) {
                int amount = item.getAmount();
                inventory.setItem(i, TrapItem.getTrapItem(amount, newSkin));
            }
        }

        Bukkit.getScheduler().runTaskLater(this.plugin, player::updateInventory, 1L);
    }

    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager == null) {
            return false;
        } else if (this.plugin.getConfig().getBoolean("plate.disabled_all_regions", false)) {
            return regionManager.getApplicableRegions(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ())).getRegions().stream().anyMatch((region) -> !"__default__".equals(region.getId()));
        } else {
            List<String> bannedRegions = this.plugin.getConfig().getStringList("plate.banned_regions");
            BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            return regionManager.getApplicableRegions(vector).getRegions().stream().anyMatch((region) -> bannedRegions.contains(region.getId()));
        }
    }

    private boolean hasBannedRegionFlags(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager == null) {
            return false;
        } else {
            ConfigurationSection bannedFlagsSection = this.plugin.getConfig().getConfigurationSection("plate.banned_region_flags");
            if (bannedFlagsSection == null) {
                return false;
            } else {
                BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

                for (ProtectedRegion region : regionManager.getApplicableRegions(vector).getRegions()) {
                    for (String flagName : bannedFlagsSection.getKeys(false)) {
                        StateFlag flag = (StateFlag) Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                        if (flag != null && region.getFlag(flag) != null) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
    }

    public void removeAllPlates() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                for (String regionName : regionManager.getRegions().keySet()) {
                    if (regionName.endsWith("plate_")) {
                        regionManager.removeRegion(regionName);
                    }
                }
            }
        }

        this.restoreAllBlocks();
        this.activePlates.clear();
        GlobalTrapRegistry.getInstance().clearAll();
    }

    private boolean isRegionNearby(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager == null) {
            return false;
        } else {
            BlockVector3 min = BlockVector3.at(location.getBlockX() - 3, location.getBlockY() - 3, location.getBlockZ() - 3);
            BlockVector3 max = BlockVector3.at(location.getBlockX() + 3, location.getBlockY() + 3, location.getBlockZ() + 3);
            ProtectedCuboidRegion checkRegion = new ProtectedCuboidRegion("checkRegion", min, max);
            return regionManager.getApplicableRegions(checkRegion).getRegions().stream().anyMatch((region) -> region.getId().endsWith("_trap") || region.getId().startsWith("plate_"));
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
        } else if ((double) yaw >= (double) -22.5F && (double) yaw <= (double) 22.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".forward_schematic", "plate_forward.schem");
        } else if ((double) yaw > (double) 22.5F && (double) yaw <= (double) 67.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".forward_left_schematic", "plate_forward_left.schem");
        } else if ((double) yaw > (double) -67.5F && (double) yaw <= (double) -22.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".forward_right_schematic", "plate_forward_right.schem");
        } else if ((double) yaw > (double) 67.5F && (double) yaw <= (double) 112.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".left_schematic", "plate_left.schem");
        } else if ((double) yaw > (double) 112.5F && (double) yaw <= (double) 157.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".backward_left_schematic", "plate_backward_left.schem");
        } else if ((double) yaw < (double) -112.5F && (double) yaw >= (double) -157.5F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".backward_right_schematic", "plate_backward_right.schem");
        } else if ((double) yaw > (double) 157.5F && yaw <= 180.0F || (double) yaw < (double) -157.5F && yaw >= -180.0F) {
            schematicName = this.plugin.getConfig().getString(configPath + ".backward_schematic", "plate_backward.schem");
        } else {
            schematicName = this.plugin.getConfig().getString(configPath + ".right_schematic", "plate_right.schem");
        }

        return new org.migrate1337.viotrap.listeners.DirectionInfo(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, schematicName);
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
        }

        ConfigurationSection flagsSection = this.plugin.getConfig().getConfigurationSection("plate.flags");
        if (flagsSection != null) {
            for (String flagName : flagsSection.getKeys(false)) {
                try {
                    StateFlag flag = (StateFlag) Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                    if (flag != null) {
                        String value = flagsSection.getString(flagName);
                        StateFlag.State state = State.valueOf(value.toUpperCase());
                        region.setFlag(flag, state);
                    }
                } catch (IllegalArgumentException e) {
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
        }
    }

    public void restoreAllBlocks() {
        if (this.playerReplacedBlocks.isEmpty()) {
            this.loadPlatesFromConfig();
        }

        // Очистка всех пластов (регистров)
        this.activePlates.clear();
        GlobalTrapRegistry.getInstance().clearAll();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Мы не должны восстанавливать блоки при выходе игрока мгновенно, если таймер еще идет, 
        // но по вашей старой логике это происходило. Я закомментировал немедленное восстановление,
        // так как таймеры (runTaskLater) все равно сработают.
        // Если вы хотите восстанавливать при выходе:
        // UUID playerId = event.getPlayer().getUniqueId();
        // this.restoreBlocks(playerId, ...); // Требуется ID региона, которого тут нет
        // this.playerReplacedBlocks.remove(playerId);
    }

    // ------------------------------------------------------------------------------------------------
    // ВАЖНО: Новая логика сохранения
    // ------------------------------------------------------------------------------------------------
    private void saveReplacedBlocks(UUID playerId, Location startLocation, Clipboard clipboard, String regionId) {
        Map<Location, TrapBlockData> replacedBlocks = new HashMap<>();
        BlockVector3 origin = clipboard.getOrigin();

        int offsetX = startLocation.getBlockX() - origin.getBlockX();
        int offsetY = startLocation.getBlockY() - origin.getBlockY();
        int offsetZ = startLocation.getBlockZ() - origin.getBlockZ();

        for (BlockVector3 vec : clipboard.getRegion()) {
            // Пропускаем воздух
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

            ItemStack[] contents = null;
            String spawnedType = null;
            boolean isDoubleChest = false;
            Location pairedLocation = null;

            if (state instanceof Container) {
                Container container = (Container) state;
                Inventory inventory = container.getInventory();
                InventoryHolder holder = inventory.getHolder();

                if (holder instanceof DoubleChest) {
                    DoubleChest doubleChest = (DoubleChest) holder;
                    isDoubleChest = true;
                    contents = inventory.getContents().clone();

                    if (doubleChest.getLeftSide() == holder) {
                        InventoryHolder right = doubleChest.getRightSide();
                        if (right instanceof Chest) {
                            pairedLocation = ((Chest) right).getLocation();
                        }
                    }
                } else {
                    contents = inventory.getContents().clone();
                }
            }

            if (state instanceof CreatureSpawner) {
                CreatureSpawner spawner = (CreatureSpawner) state;
                EntityType type = spawner.getSpawnedType();
                if (type != null) {
                    spawnedType = type.name();
                }
            }

            TrapBlockData currentWorldData = new TrapBlockData(
                    block.getType(),
                    block.getBlockData(),
                    contents,
                    spawnedType
            );
            currentWorldData.setDoubleChest(isDoubleChest);
            if(pairedLocation != null) currentWorldData.setPairedChestLocation(pairedLocation);

            // Регистрация в глобальном реестре
            TrapBlockData finalData = GlobalTrapRegistry.getInstance().registerAndGetOriginal(worldLoc, regionId, currentWorldData);

            replacedBlocks.put(worldLoc.clone(), finalData);
        }

        this.playerReplacedBlocks.put(playerId, replacedBlocks);
    }

    // ------------------------------------------------------------------------------------------------
    // ВАЖНО: Новая логика восстановления
    // ------------------------------------------------------------------------------------------------
    private void restoreBlocks(UUID playerId, String regionId) {
        Map<Location, TrapBlockData> replacedBlocks = this.playerReplacedBlocks.get(playerId);
        if (replacedBlocks == null || replacedBlocks.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Location, TrapBlockData>> iterator = replacedBlocks.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<Location, TrapBlockData> entry = iterator.next();
            Location loc = entry.getKey();
            TrapBlockData oldData = entry.getValue();

            // Проверка через реестр
            boolean shouldRestore = GlobalTrapRegistry.getInstance().unregister(loc, regionId);

            if (!shouldRestore) {
                this.removePlateFromFile(loc);
                iterator.remove();
                continue;
            }

            // Физическое восстановление
            Block currentBlock = loc.getBlock();
            currentBlock.setType(oldData.getMaterial());
            currentBlock.setBlockData(oldData.getBlockData());

            BlockState state = currentBlock.getState();

            if (state instanceof CreatureSpawner && oldData.getSpawnedType() != null) {
                CreatureSpawner spawner = (CreatureSpawner) state;
                try {
                    spawner.setSpawnedType(EntityType.valueOf(oldData.getSpawnedType()));
                    spawner.update(true);
                } catch (Exception ignored) {}
            }

            if (oldData.getContents() != null && state instanceof Container) {
                Container container = (Container) state;
                Inventory inv = container.getInventory();

                if (inv.getHolder() instanceof DoubleChest) {
                    inv.setContents(oldData.getContents());
                } else {
                    ItemStack[] trimmed = new ItemStack[27];
                    System.arraycopy(oldData.getContents(), 0, trimmed, 0, Math.min(oldData.getContents().length, 27));
                    inv.setContents(trimmed);
                }
            }

            this.removePlateFromFile(loc);
            iterator.remove();
        }

        if (replacedBlocks.isEmpty()) {
            this.playerReplacedBlocks.remove(playerId);
        }
    }

    private void loadPlatesFromConfig() {
        if (this.plugin.getPlatesConfig().contains("plates")) {
            ConfigurationSection platesSection = this.plugin.getPlatesConfig().getConfigurationSection("plates");

            for (String worldName : platesSection.getKeys(false)) {
                ConfigurationSection worldSection = platesSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    for (String plateKey : worldSection.getKeys(false)) {
                        ConfigurationSection plateSection = worldSection.getConfigurationSection(plateKey);
                        if (plateSection != null) {
                            UUID playerId = UUID.fromString(plateSection.getString("player"));
                            String world = plateSection.getString("world");
                            int x = plateSection.getInt("x");
                            int y = plateSection.getInt("y");
                            int z = plateSection.getInt("z");
                            String skin = plateSection.getString("skin", "default");
                            long endTime = plateSection.getLong("endTime", 0L);
                            Location location = new Location(Bukkit.getWorld(world), (double) x, (double) y, (double) z);

                            // Формируем ID для реестра
                            String playerName = Bukkit.getOfflinePlayer(playerId).getName();
                            if(playerName == null) playerName = "Unknown";
                            String plateId = "plate_" + playerName + "_" + x + "_" + y + "_" + z;

                            long currentTime = System.currentTimeMillis();
                            long remainingMillis = endTime - currentTime;

                            if (remainingMillis > 0L) {
                                // Нужно зарегистрировать блоки в реестре.
                                // Поскольку у нас нет схематики в памяти при загрузке пластов так же легко как в трапках (тут логика DirectionInfo),
                                // мы попытаемся получить схематику.
                                // (Внимание: здесь возможна неточность, так как мы не знаем Yaw/Pitch игрока, который ставил пласт.
                                // Но мы должны хотя бы попытаться зарегистрировать центральный блок или использовать сохраненные данные).

                                // В текущей реализации пластов (PlateItemListener) вы сохраняете каждый блок в playerReplacedBlocks при Load.
                                // Но вы берете блок из мира: Block block = location.getBlock();
                                // Это ОШИБКА, если там уже стоит пласт (обсидиан).
                                // Чтобы исправить это полностью, вам нужно сохранять схематику или оригинальные данные в файл.
                                // Сейчас я добавлю регистрацию в реестр ТЕКУЩЕГО блока, чтобы хотя бы пересечения работали.

                                Block block = location.getBlock();
                                TrapBlockData currentData = new TrapBlockData(block.getType(), block.getBlockData(), null, null);
                                TrapBlockData finalData = GlobalTrapRegistry.getInstance().registerAndGetOriginal(location, plateId, currentData);

                                this.playerReplacedBlocks.putIfAbsent(playerId, new HashMap<>());
                                this.playerReplacedBlocks.get(playerId).put(location, finalData);

                                this.activePlateTimers.put(plateId, new PlateData(playerId, location, skin, endTime));

                                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                    PlateData data = this.activePlateTimers.remove(plateId);
                                    if (data != null) {
                                        this.restoreBlocks(data.getPlayerId(), plateId);
                                        this.removePlateRegion(Bukkit.getPlayer(data.getPlayerId()), data.getLocation());
                                        this.removePlateFromFile(data.getLocation());
                                        Location loc = data.getLocation();
                                        String soundEnded = data.getSkin().equals("default") ? this.plugin.getConfig().getString("plate.sound.type-ended", "BLOCK_PISTON_EXTEND") : this.plugin.getConfig().getString("plate_skins." + data.getSkin() + ".sound.type-ended", "BLOCK_PISTON_EXTEND");
                                        float soundVolumeEnded = skin.equals("default") ? (float) this.plugin.getConfig().getDouble("plate.sound.volume-ended", (double) 10.0F) : (float) this.plugin.getConfig().getDouble("plate_skins." + skin + ".sound.volume-ended", (double) 1.0F);
                                        float soundPitchEnded = skin.equals("default") ? (float) this.plugin.getConfig().getDouble("plate.sound.pitch-ended", (double) 1.0F) : (float) this.plugin.getConfig().getDouble("plate_skins." + skin + ".sound.pitch-ended", (double) 1.0F);
                                        loc.getWorld().playSound(loc, Sound.valueOf(soundEnded), soundVolumeEnded, soundPitchEnded);
                                    }
                                }, remainingMillis / 50L);
                            } else {
                                // Время вышло
                                this.removePlateFromFile(location);
                            }
                        }
                    }
                }
            }
        }
    }

    private void savePlateToConfig(Player player, Location location, String skin, long durationSeconds) {
        String path = "plates." + location.getWorld().getName() + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        this.plugin.getPlatesConfig().set(path + ".player", player.getUniqueId().toString());
        this.plugin.getPlatesConfig().set(path + ".world", location.getWorld().getName());
        this.plugin.getPlatesConfig().set(path + ".x", location.getBlockX());
        this.plugin.getPlatesConfig().set(path + ".y", location.getBlockY());
        this.plugin.getPlatesConfig().set(path + ".z", location.getBlockZ());
        this.plugin.getPlatesConfig().set(path + ".skin", skin);
        this.plugin.getPlatesConfig().set(path + ".endTime", System.currentTimeMillis() + durationSeconds * 1000L);
        this.plugin.savePlatesConfig();
    }

    private void removePlateFromFile(Location location) {
        String path = "plates." + location.getWorld().getName() + "." + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        this.plugin.getPlatesConfig().set(path, null);
        this.plugin.savePlatesConfig();
    }
}