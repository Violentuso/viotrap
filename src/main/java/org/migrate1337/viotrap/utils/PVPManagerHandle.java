package org.migrate1337.viotrap.utils;

import me.chancesd.pvpmanager.PvPManager;
import me.chancesd.pvpmanager.player.CombatPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.migrate1337.viotrap.VioTrap;


public class PVPManagerHandle {
    private final PvPManager pvpManager;
    private final boolean isPvPManagerEnabled;
    private VioTrap plugin;
    public PVPManagerHandle(VioTrap plugin) {
        this.plugin = plugin;
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin pvpManagerPlugin = pluginManager.getPlugin("PvPManager");
        if (pvpManagerPlugin instanceof PvPManager) {
            this.pvpManager = (PvPManager)pvpManagerPlugin;
            this.isPvPManagerEnabled = true;

        } else {
            this.pvpManager = null;
            this.isPvPManagerEnabled = false;
        }

    }

    public boolean isPvPManagerEnabled() {
        return this.isPvPManagerEnabled;
    }

    public void tagPlayerForPvP(Player player, String item) {
        if (Bukkit.getPluginManager().isPluginEnabled("PvPManager")) {

            CombatPlayer pvPlayer = CombatPlayer.get(player);
            if (pvPlayer != null && !pvPlayer.isInCombat()) {
                pvPlayer.tag(true, pvPlayer, plugin.getConfig().getLong(item + ".duration" )* 1000 );
            }
        }

    }
}
