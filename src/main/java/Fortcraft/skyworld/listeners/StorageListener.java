package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.PlayerMode;
import Fortcraft.skyworld.storage.StorageGUI;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class StorageListener implements Listener {

    private final Skyworld plugin;
    private final NamespacedKey keyAction = new NamespacedKey("fortcraft", StorageGUI.KEY_ACTION);
    private final NamespacedKey keyPage = new NamespacedKey("fortcraft", StorageGUI.KEY_PAGE);

    public StorageListener() {
        this.plugin = Skyworld.getInstance();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        String title = e.getView().getTitle();
        if (!title.startsWith("§") || !title.contains("Almacén:")) return;

        e.setCancelled(true);

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        if (!clickedItem.hasItemMeta()) return;

        var pdc = clickedItem.getItemMeta().getPersistentDataContainer();
        var managerHandler = plugin.getManagerHandler();
        var playerData = managerHandler.getDataManager().getPlayerData(player.getUniqueId());
        var bag = playerData.getStorageBag();

        PlayerMode currentMode = managerHandler.getHotbarManager().getMode(player);

        if (pdc.has(keyAction, PersistentDataType.STRING)) {
            String action = pdc.get(keyAction, PersistentDataType.STRING);
            int currentPage = pdc.getOrDefault(keyPage, PersistentDataType.INTEGER, 0);

            if (action == null) return;

            switch (action) {
                case "NEXT" -> {
                    StorageGUI.open(player, bag, currentMode, currentPage + 1);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                }
                case "PREV" -> {
                    StorageGUI.open(player, bag, currentMode, currentPage - 1);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                }
                case "NEXT_5" -> {
                    StorageGUI.open(player, bag, currentMode, currentPage + 5);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                }
                case "PREV_5" -> {
                    StorageGUI.open(player, bag, currentMode, Math.max(0, currentPage - 5));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                }
                case "EXIT" -> {
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
                }
            }
        }
    }
}