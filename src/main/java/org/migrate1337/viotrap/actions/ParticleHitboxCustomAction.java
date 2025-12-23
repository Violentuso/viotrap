package org.migrate1337.viotrap.actions;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.migrate1337.viotrap.VioTrap;

public class ParticleHitboxCustomAction implements CustomAction {
    private final String target;
    private final String particleType;
    private final int duration;
    private final int updateInterval;

    public ParticleHitboxCustomAction(String target, String particleType, int duration, int updateInterval) {
        this.target = target.toLowerCase();
        this.particleType = particleType.toUpperCase();
        this.duration = duration;
        this.updateInterval = updateInterval;
    }

    public void execute(Player player, Player[] opponents, VioTrap plugin) {
        switch (this.target) {
            case "p":
            case "player":
                this.spawnHitboxParticles(player, plugin);
                break;
            case "o":
                for(Player opponent : opponents) {
                    this.spawnHitboxParticles(opponent, plugin);
                }
                break;
            case "rp":
                Player randomPlayer = CustomActionFactory.getRandomPlayer(player, opponents);
                this.spawnHitboxParticles(randomPlayer, plugin);
                break;
            default:
        }

    }

    private void spawnHitboxParticles(Player player, VioTrap plugin) {
        if (player != null && player.isOnline()) {
            Particle particle;
            try {
                particle = Particle.valueOf(this.particleType);
            } catch (IllegalArgumentException e) {
                return;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    // Если игрок вышел — прекращаем задачу
                    if (player == null || !player.isOnline()) {
                        cancel();
                        return;
                    }

                    // Спавн частиц по центру хитбокса
                    Location loc = player.getLocation().add(0, player.getHeight() / 2, 0);
                    player.getWorld().spawnParticle(
                            particle,
                            loc,
                            5,       // количество частиц
                            0.2,     // offsetX
                            0.5,     // offsetY
                            0.2,     // offsetZ
                            0        // скорость
                    );
                }
            }.runTaskTimer(plugin, 0L, this.updateInterval);
        }
    }


    private void spawnLineParticles(Player player, double[] start, double[] end, Particle particle, int count) {
        double dx = (end[0] - start[0]) / (double)(count - 1);
        double dy = (end[1] - start[1]) / (double)(count - 1);
        double dz = (end[2] - start[2]) / (double)(count - 1);

        for(int i = 0; i < count; ++i) {
            double x = start[0] + dx * (double)i;
            double y = start[1] + dy * (double)i;
            double z = start[2] + dz * (double)i;
            player.getWorld().spawnParticle(particle, x, y, z, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
        }

    }
}