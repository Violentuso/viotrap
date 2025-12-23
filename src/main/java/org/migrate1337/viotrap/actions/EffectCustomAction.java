package org.migrate1337.viotrap.actions;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.migrate1337.viotrap.VioTrap;

public class EffectCustomAction implements CustomAction {
    private final String target;
    private final String effectName;
    private final int amplifier;
    private final int duration;

    public EffectCustomAction(String target, String effectName, int amplifier, int duration) {
        this.target = target.toLowerCase();
        this.effectName = effectName.toUpperCase();
        this.amplifier = amplifier;
        this.duration = duration;
    }

    public void execute(Player player, Player[] opponents, VioTrap plugin) {
        PotionEffectType effectType;
        try {
            effectType = PotionEffectType.getByName(this.effectName);
            if (effectType == null) {
                return;
            }
        } catch (Exception var11) {
            return;
        }

        switch (this.target) {
            case "p":
            case "player":
                this.applyEffect(player, effectType);
                break;
            case "o":
                for(Player opponent : opponents) {
                    this.applyEffect(opponent, effectType);
                }
                break;
            case "rp":
                Player randomPlayer = CustomActionFactory.getRandomPlayer(player, opponents);
                this.applyEffect(randomPlayer, effectType);
                break;
            default:
        }

    }

    private void applyEffect(Player player, PotionEffectType effectType) {
        if (player != null && player.isOnline()) {
            player.addPotionEffect(new PotionEffect(effectType, this.duration * 20, this.amplifier));
        }

    }
}
