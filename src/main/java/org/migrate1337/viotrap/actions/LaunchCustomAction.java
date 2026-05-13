package org.migrate1337.viotrap.actions;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.migrate1337.viotrap.VioTrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LaunchCustomAction implements CustomAction {
    private final String targetType;
    private final double radius;
    private final double upwardForce;
    private final double horizontalForce;

    public LaunchCustomAction(String targetType, double radius, double upwardForce, double horizontalForce) {
        this.targetType = targetType;
        this.radius = radius;
        this.upwardForce = upwardForce;
        this.horizontalForce = horizontalForce;
    }

    @Override
    public void execute(Player primaryPlayer, Player[] opponents, VioTrap plugin) {
        for (Player t : CustomActionFactory.getTargets(this.targetType, primaryPlayer, opponents, this.radius)) {
            if (t != null && t.isOnline()) {
                applyVelocity(primaryPlayer, t);
            }
        }
    }

    private void applyVelocity(Player center, Player target) {
        Vector direction = target.getLocation().toVector().subtract(center.getLocation().toVector());

         
        if (direction.lengthSquared() == 0) {
            direction = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
        }

        direction = direction.normalize().multiply(horizontalForce);
        direction.setY(upwardForce);

        target.setVelocity(direction);
    }

    private List<Player> getStandardTargets(Player primaryPlayer, Player[] opponents) {
        List<Player> targets = new ArrayList<>();
        if (targetType.equalsIgnoreCase("p") || targetType.equalsIgnoreCase("player")) {
            targets.add(primaryPlayer);
        } else if (targetType.equalsIgnoreCase("o") && opponents != null) {
            targets.addAll(Arrays.asList(opponents));
        } else if (targetType.equalsIgnoreCase("rp")) {
            Player p = CustomActionFactory.getRandomPlayer(primaryPlayer, opponents);
            if (p != null) targets.add(p);
        }
        return targets;
    }
}