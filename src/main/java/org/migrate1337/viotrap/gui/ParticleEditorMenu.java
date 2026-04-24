package org.migrate1337.viotrap.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;

import java.util.Arrays;

public class ParticleEditorMenu implements Listener {
    private final VioTrap plugin;
    private final String menuTitle = "§8Редактор партиклов";

    public ParticleEditorMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, menuTitle);

        // Кнопка пресета: 3x3x3 Куб
        ItemStack cubePreset = new ItemStack(Material.STONE);
        ItemMeta cubeMeta = cubePreset.getItemMeta();
        if (cubeMeta != null) {
            cubeMeta.setDisplayName("§eПресет: Куб 3x3x3");
            cubeMeta.setLore(Arrays.asList(
                    "§7Нажмите, чтобы создать новый",
                    "§7шаблон партиклов в кубе 3x3x3."
            ));
            cubePreset.setItemMeta(cubeMeta);
        }
        inv.setItem(13, cubePreset); // По центру

        // Кнопка закрытия
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§cЗакрыть");
            close.setItemMeta(closeMeta);
        }
        inv.setItem(26, close); // В правом нижнем углу

        // Декоративное стекло для пустого пространства
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Проверяем, что это наше меню
        if (!event.getView().getTitle().equals(menuTitle)) return;

        event.setCancelled(true); // Запрещаем забирать предметы

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.STONE) {
            player.closeInventory();
            // Генерируем уникальное имя шаблона (позже можно будет запрашивать в чате)
            String tempPatternName = player.getName() + "_pattern_" + (System.currentTimeMillis() / 1000);

            // Запускаем сессию из Part 1!
            plugin.getParticleEditorManager().startEditorSession(player, tempPatternName);

        } else if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }
}