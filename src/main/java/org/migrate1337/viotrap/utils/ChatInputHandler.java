package org.migrate1337.viotrap.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatInputHandler implements Listener {
    private final Map<Player, Consumer<String>> inputMap = new HashMap();

    public void waitForInput(Player player, Consumer<String> callback) {
        this.inputMap.put(player, callback);
    }

    public boolean handleChatInput(Player player, String message) {
        if (this.inputMap.containsKey(player)) {
            Consumer<String> callback = (Consumer)this.inputMap.remove(player);
            callback.accept(message);
            return true;
        } else {
            return false;
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (this.inputMap.containsKey(player)) {
            event.setCancelled(true);
            String message = event.getMessage();
            Consumer<String> callback = (Consumer)this.inputMap.remove(player);
            callback.accept(message);
        }

    }
}
