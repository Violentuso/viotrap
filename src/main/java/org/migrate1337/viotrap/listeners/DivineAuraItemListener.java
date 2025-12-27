package org.migrate1337.viotrap.listeners;

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
import org.migrate1337.viotrap.items.DivineAuraItem;

public class DivineAuraItemListener implements Listener {
    private final VioTrap plugin;

    public DivineAuraItemListener(VioTrap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseDivineAuraItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.isSimilar(DivineAuraItem.getDivineAuraItem(item.getAmount(), this.plugin))) {
            if (event.getAction().toString().contains("RIGHT_CLICK")) {
                if (!plugin.getConditionManager().checkConditions(player, "divine_aura")) {
                    return; // Условия не выполнены, сообщение уже отправлено менеджером
                }
                if (player.hasCooldown(item.getType())) {
                    player.sendMessage("§cПодождите перед использованием снова!");
                } else {
                    Location location = player.getLocation();
                    String worldName = location.getWorld().getName();
                    if (!this.isInBannedRegion(location, location.getWorld().getName()) && !this.hasBannedRegionFlags(location, location.getWorld().getName())) {
                        item.setAmount(item.getAmount() - 1);
                        player.sendMessage(this.plugin.getConfig().getString("divine_aura.messages.success_used"));
                        int cooldownSeconds = this.plugin.getDivineAuraItemCooldown();
                        player.setCooldown(item.getType(), cooldownSeconds * 20);
                        Location playerLocation = player.getLocation();
                        String particleType = this.plugin.getDivineAuraItemParticleType();
                        String soundType = this.plugin.getDivineAuraItemSoundType();
                        float volume = this.plugin.getDivineAuraItemSoundVolume();
                        float pitch = this.plugin.getDivineAuraItemSoundPitch();
                        player.getWorld().spawnParticle(Particle.valueOf(particleType), playerLocation, 50, (double)0.5F, (double)1.0F, (double)0.5F, 0.05);
                        player.getWorld().playSound(playerLocation, Sound.valueOf(soundType), volume, pitch);

                        for(String effect : this.plugin.getConfig().getStringList("divine_aura.negative_effects")) {
                            PotionEffectType effectType = PotionEffectType.getByName(effect);
                            if (effectType != null) {
                                player.removePotionEffect(effectType);
                            }
                        }

                        for(Map<?, ?> effect : this.plugin.getConfig().getMapList("divine_aura.positive_effects")) {
                            for(Map.Entry<?, ?> entry : effect.entrySet()) {
                                String effectName = (String)entry.getKey();
                                Map<?, ?> effectDetails = (Map)entry.getValue();
                                PotionEffectType effectType = PotionEffectType.getByName(effectName);
                                if (effectType != null) {
                                    int duration = (Integer)effectDetails.get("duration") * 20;
                                    int amplifier = (Integer)effectDetails.get("amplifier");
                                    player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
                                }
                            }
                        }

                        player.sendMessage("§aВы сняли с себя негативные эффекты и получили благословение!");
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
        if (!this.plugin.getConfig().getBoolean("divine_aura.disabled_all_regions", false)) {
            if (regionManager == null) {
                return false;
            } else {
                List<String> bannedRegions = this.plugin.getConfig().getStringList("divine_aura.banned_regions");
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
            ConfigurationSection bannedFlagsSection = this.plugin.getConfig().getConfigurationSection("divine_aura.banned_region_flags");
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
}
