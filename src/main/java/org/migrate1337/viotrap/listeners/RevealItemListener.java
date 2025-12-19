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
import org.bukkit.scheduler.BukkitRunnable;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.RevealItem;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.PVPManagerHandle;

public class RevealItemListener implements Listener {
    private final VioTrap plugin;
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;

    public RevealItemListener(VioTrap plugin) {
        this.plugin = plugin;
        this.combatLogXHandler = new CombatLogXHandler();
        this.pvpManagerHandler = new PVPManagerHandle(plugin);
    }

    @EventHandler
    public void onPlayerUseRevealItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.isSimilar(RevealItem.getRevealItem(item.getAmount()))) {

            if (event.getAction().toString().contains("RIGHT_CLICK")) {
                if (player.hasCooldown(item.getType())) {
                    player.sendMessage("§cПодождите перед использованием снова!");
                } else {
                    item.setAmount(item.getAmount() - 1);
                    player.sendMessage(this.plugin.getConfig().getString("reveal_item.messages.success_used"));
                    Location location = player.getLocation();
                    String worldName = location.getWorld().getName();
                    if (!this.isInBannedRegion(location, location.getWorld().getName()) && !this.hasBannedRegionFlags(location, location.getWorld().getName())) {
                        int cooldownSeconds = this.plugin.getRevealItemCooldown();
                        int durationSeconds = this.plugin.getRevealItemGlowDuration();
                        player.setCooldown(item.getType(), cooldownSeconds * 20);
                        int radius = this.plugin.getRevealItemRadius();
                        Location playerLocation = player.getLocation();
                        boolean foundOpponent = false;

                        for(Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
                            if (!nearbyPlayer.equals(player) && nearbyPlayer.getLocation().distance(playerLocation) <= (double)radius) {
                                foundOpponent = true;
                                if (this.combatLogXHandler.isCombatLogXEnabled()) {
                                    nearbyPlayer.sendMessage(this.plugin.getConfig().getString("reveal_item.messages.pvp-enabled-for-player"));
                                    this.combatLogXHandler.tagPlayer(nearbyPlayer, TagType.DAMAGE, TagReason.ATTACKED);
                                }

                                if (this.pvpManagerHandler.isPvPManagerEnabled()) {
                                    this.pvpManagerHandler.tagPlayerForPvP(nearbyPlayer, "reveal_item");
                                    nearbyPlayer.sendMessage(this.plugin.getConfig().getString("reveal_item.messages.pvp-enabled-for-player"));
                                }

                                boolean wasInvisible = nearbyPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY);
                                int remainingInvisibilityTime = 0;
                                if (wasInvisible) {
                                    PotionEffect invisibilityEffect = nearbyPlayer.getPotionEffect(PotionEffectType.INVISIBILITY);
                                    remainingInvisibilityTime = invisibilityEffect.getDuration();
                                    nearbyPlayer.removePotionEffect(PotionEffectType.INVISIBILITY);
                                }

                                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationSeconds * 20, 0));
                                if (wasInvisible) {
                                    int finalRemainingInvisibilityTime = remainingInvisibilityTime - durationSeconds * 20;

                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            // Если игрок ещё онлайн и жив
                                            if (nearbyPlayer != null && nearbyPlayer.isOnline() && !nearbyPlayer.isDead()) {

                                                // Если нужно — можно проверить, что время положительное
                                                if (finalRemainingInvisibilityTime > 0) {
                                                    // Вернуть невидимость игроку на оставшееся время
                                                    nearbyPlayer.addPotionEffect(
                                                            new PotionEffect(PotionEffectType.INVISIBILITY, finalRemainingInvisibilityTime, 0, true, false)
                                                    );
                                                }
                                            }
                                        }
                                    }.runTaskLater(this.plugin, durationSeconds * 20L);
                                }

                            }
                        }

                        if (this.combatLogXHandler.isCombatLogXEnabled() && foundOpponent) {
                            this.combatLogXHandler.tagPlayer(player, TagType.DAMAGE, TagReason.UNKNOWN);
                            player.sendMessage(this.plugin.getConfig().getString("reveal_item.messages.pvp-enabled-by-player"));
                        }

                        if (this.pvpManagerHandler.isPvPManagerEnabled() && foundOpponent) {
                            this.pvpManagerHandler.tagPlayerForPvP(player, "reveal_item");
                            player.sendMessage(this.plugin.getConfig().getString("reveal_item.messages.pvp-enabled-by-player"));
                        }

                        String soundType = this.plugin.getRevealItemSoundType();
                        float volume = this.plugin.getRevealItemSoundVolume();
                        float pitch = this.plugin.getRevealItemSoundPitch();
                        player.playSound(playerLocation, Sound.valueOf(soundType), volume, pitch);
                        this.showParticleCircle(playerLocation, (double)radius, Particle.valueOf(VioTrap.getPlugin().getRevealItemParticleType()));
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
        if (!this.plugin.getConfig().getBoolean("reveal_item.disabled_all_regions", false)) {
            if (regionManager == null) {
                return false;
            } else {
                List<String> bannedRegions = this.plugin.getConfig().getStringList("reveal_item.banned_regions");
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
            ConfigurationSection bannedFlagsSection = this.plugin.getConfig().getConfigurationSection("reveal_item.banned_region_flags");
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
