package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.EconomyManager;
import Fortcraft.skyworld.menu.LoadoutGUI;
import Fortcraft.skyworld.menu.MenuItem;
import Fortcraft.skyworld.menu.SkyblockMenu;
import Fortcraft.skyworld.storage.StorageBag;
import Fortcraft.skyworld.utils.HotbarSlot;
import Fortcraft.skyworld.utils.PlayerMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        // --- PRIORIDAD 1: MENÚS DINÁMICOS (Basados en Holder) ---
        // Esto detecta cualquier menú cargado desde menus.yml
        if (e.getInventory().getHolder() instanceof SkyblockMenu) {
            e.setCancelled(true);
            handleDynamicMenu(e);
            return;
        }

        // --- PRIORIDAD 2: MENÚS ESTÁTICOS (Basados en Título/PDC) ---
        String title = e.getView().getTitle();
        Player player = (Player) e.getWhoClicked();

        // 1. MANEJO DE GLOBAL MENU
        if (title.contains("Menú ")) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item.getType() == Material.ANVIL && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Configurar")) {
                LoadoutGUI.open(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        // 2. MANEJO DE LOADOUT GUI
        if (title.startsWith(LoadoutGUI.PREFIX)) {
            e.setCancelled(true);
            var meta = e.getCurrentItem().getItemMeta();
            if (meta == null) return;

            var pdc = meta.getPersistentDataContainer();
            if (pdc.has(LoadoutGUI.KEY_SLOT_INDEX, PersistentDataType.INTEGER)) {
                int slotIndex = pdc.get(LoadoutGUI.KEY_SLOT_INDEX, PersistentDataType.INTEGER);
                LoadoutGUI.openSelector(player, slotIndex);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            }
            return;
        }

        // 3. MANEJO DE SELECTOR DE ITEMS
        if (title.startsWith(LoadoutGUI.SELECTOR_PREFIX)) {
            e.setCancelled(true);
            var meta = e.getCurrentItem().getItemMeta();
            if (meta == null) return;

            var pdc = meta.getPersistentDataContainer();
            if (pdc.has(LoadoutGUI.KEY_ITEM_ID, PersistentDataType.STRING)) {
                String itemId = pdc.get(LoadoutGUI.KEY_ITEM_ID, PersistentDataType.STRING);

                if (itemId.equals("BACK")) {
                    LoadoutGUI.open(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
                    return;
                }

                int slotIndex = pdc.get(LoadoutGUI.KEY_SLOT_INDEX, PersistentDataType.INTEGER);
                var managerHandler = Skyworld.getInstance().getManagerHandler();
                var data = managerHandler.getDataManager().getPlayerData(player.getUniqueId());
                var hotbarManager = managerHandler.getHotbarManager();
                PlayerMode mode = hotbarManager.getMode(player);
                HotbarSlot slotEnum = HotbarSlot.fromIndex(slotIndex);

                if (itemId.equals("REMOVE")) {
                    data.setLoadoutItem(mode, slotEnum, null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
                } else {
                    data.setLoadoutItem(mode, slotEnum, itemId);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.5f);
                }

                hotbarManager.applyLoadout(player);
                LoadoutGUI.open(player);
            }
        }
    }

    /**
     * Lógica para los menús cargados desde menus.yml
     */
    private void handleDynamicMenu(InventoryClickEvent e) {
        SkyblockMenu menu = (SkyblockMenu) e.getInventory().getHolder();
        MenuItem item = menu.getItem(e.getSlot());
        if (item == null) return;

        Player p = (Player) e.getWhoClicked();
        var handler = Skyworld.getInstance().getManagerHandler();
        EconomyManager eco = handler.getEconomyManager();
        StorageBag bag = handler.getDataManager().getPlayerData(p.getUniqueId()).getStorageBag();

        switch (item.getAction()) {
            case BUY -> {
                if (eco.withdraw(p, item.getPrice())) {
                    Material mat = Material.matchMaterial(item.getTargetId());
                    if (mat != null) p.getInventory().addItem(new ItemStack(mat, item.getAmount()));
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    p.sendMessage("§aHas comprado " + item.getAmount() + "x de este objeto.");
                } else {
                    p.sendMessage("§cNo tienes suficiente dinero.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
            case SELL -> {
                if (bag.hasItem(item.getTargetId(), item.getAmount())) {
                    bag.removeItem(item.getTargetId(), item.getAmount());
                    eco.addCoins(p, item.getPrice());
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                    p.sendMessage("§aVendido con éxito por " + item.getPrice() + ".");
                } else {
                    p.sendMessage("§cNo tienes suficientes objetos en tu bolsa.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
            case CLOSE -> p.closeInventory();
            case COMMAND -> p.performCommand(item.getTargetId());
        }
    }
}