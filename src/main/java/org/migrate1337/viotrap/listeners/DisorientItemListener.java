//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.migrate1337.viotrap.listeners;

import com.github.sirblobman.combatlogx.api.object.TagReason;
import com.github.sirblobman.combatlogx.api.object.TagType;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.DisorientItem;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.PVPManagerHandle;

public class DisorientItemListener implements Listener {
    private final VioTrap plugin;
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;

    public DisorientItemListener(VioTrap plugin) {
        this.plugin = plugin;
        this.combatLogXHandler = new CombatLogXHandler();
        this.pvpManagerHandler = new PVPManagerHandle(plugin);
    }

    @EventHandler
    public void onPlayerUseDisorientItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.isSimilar(DisorientItem.getDisorientItem(item.getAmount()))) {
            if (event.getAction().toString().contains("RIGHT_CLICK")) {
                if (player.hasCooldown(item.getType())) {
                    player.sendMessage("§cПодождите перед использованием снова!");
                } else {
                    Location location = player.getLocation();
                    String worldName = location.getWorld().getName();
                    if (!this.isInBannedRegion(location, location.getWorld().getName()) && !this.hasBannedRegionFlags(location, location.getWorld().getName())) {
                        int radius = this.plugin.getDisorientItemRadius();
                        Location playerLocation = player.getLocation();
                        boolean foundOpponent = false;
                        player.sendMessage(this.plugin.getConfig().getString("disorient_item.messages.success_used"));

                        for(Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
                            if (!nearbyPlayer.equals(player) && nearbyPlayer.getLocation().distance(playerLocation) <= (double)radius) {
                                foundOpponent = true;
                                if (this.combatLogXHandler.isCombatLogXEnabled()) {
                                    this.combatLogXHandler.tagPlayer(nearbyPlayer, TagType.DAMAGE, TagReason.ATTACKED);
                                    nearbyPlayer.sendMessage(this.plugin.getConfig().getString("disorient_item.messages.pvp-enabled-for-player"));
                                }

                                if (Bukkit.getPluginManager().isPluginEnabled("PvPManager")) {
                                    Bukkit.getLogger().info("truetrueutre");
                                    this.pvpManagerHandler.tagPlayerForPvP(nearbyPlayer, "disorient_item");
                                    nearbyPlayer.sendMessage(this.plugin.getConfig().getString("disorient_item.messages.pvp-enabled-for-player"));
                                }

                                for(Map<?, ?> effect : this.plugin.getConfig().getMapList("disorient_item.negative_effects")) {
                                    for(Map.Entry<?, ?> entry : effect.entrySet()) {
                                        String effectName = (String)entry.getKey();
                                        Map<?, ?> effectDetails = (Map)entry.getValue();
                                        PotionEffectType effectType = PotionEffectType.getByName(effectName);
                                        if (effectType != null) {
                                            int duration = (Integer)effectDetails.get("duration") * 20;
                                            int amplifier = (Integer)effectDetails.get("amplifier");
                                            nearbyPlayer.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
                                        }
                                    }
                                }
                            }
                        }

                        if (this.combatLogXHandler.isCombatLogXEnabled() && foundOpponent) {
                            this.combatLogXHandler.tagPlayer(player, TagType.DAMAGE, TagReason.UNKNOWN);
                            player.sendMessage(this.plugin.getConfig().getString("disorient_item.messages.pvp-enabled-by-player"));
                        }

                        if (this.pvpManagerHandler.isPvPManagerEnabled() && foundOpponent) {
                            this.pvpManagerHandler.tagPlayerForPvP(player, "disorient_item");
                            player.sendMessage(this.plugin.getConfig().getString("disorient_item.messages.pvp-enabled-by-player"));
                        }

                        item.setAmount(item.getAmount() - 1);
                        int cooldownSeconds = this.plugin.getDisorientItemCooldown();
                        player.setCooldown(item.getType(), cooldownSeconds * 20);
                        String soundType = this.plugin.getDisorientItemSoundType();
                        float volume = this.plugin.getDisorientItemSoundVolume();
                        float pitch = this.plugin.getDisorientItemSoundPitch();
                        player.playSound(playerLocation, Sound.valueOf(soundType), volume, pitch);
                        this.showParticleCircle(playerLocation, (double)radius, Particle.valueOf(VioTrap.getPlugin().getDisorientItemParticleType()));
                    } else {
                        player.sendMessage("§cВы не можете использовать данный предмет в этом регионе!");
                    }
                }
            }
        }
    }

    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (!this.plugin.getConfig().getBoolean("disorient_item.disabled_all_regions", false)) {
            if (regionManager == null) {
                return false;
            } else {
                List<String> bannedRegions = this.plugin.getConfig().getStringList("disorient_item.banned_regions");
                BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                return regionManager.getApplicableRegions(vector).getRegions().stream().anyMatch((region) -> bannedRegions.contains(region.getId()));
            }
        } else {
            return regionManager != null && regionManager.getApplicableRegions(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ())).getRegions().stream().anyMatch((region) -> !"__default__".equals(region.getId()));
        }
    }

    private boolean hasBannedRegionFlags(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (regionManager == null) {
            return false;
        } else {
            ConfigurationSection bannedFlagsSection = this.plugin.getConfig().getConfigurationSection("disorient_item.banned_region_flags");
            if (bannedFlagsSection == null) {
                return false;
            } else {
                BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

                for(ProtectedRegion region : regionManager.getApplicableRegions(vector).getRegions()) {
                    for(String flagName : bannedFlagsSection.getKeys(false)) {
                        StateFlag flag = (StateFlag)Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                        if (flag != null && region.getFlag(flag) != null) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }
    }

    private void showParticleCircle(Location center, double radius, Particle particle) {
        int points = 100;
        double increment = (Math.PI * 2D) / (double)points;

        for(int i = 0; i < points; ++i) {
            double angle = (double)i * increment;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location particleLocation = new Location(center.getWorld(), x, center.getY(), z);
            center.getWorld().spawnParticle(particle, particleLocation, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
        }

    }
}
