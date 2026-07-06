package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.EconomyManager;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.menu.LoadoutGUI;
import Fortcraft.skyworld.menu.MenuItem;
import Fortcraft.skyworld.menu.SkyblockMenu;
import Fortcraft.skyworld.menu.QuestMenu;
import Fortcraft.skyworld.storage.StorageBag;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.HotbarSlot;
import Fortcraft.skyworld.utils.PlayerMode;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        if (e.getInventory().getHolder() instanceof SkyblockMenu) {
            e.setCancelled(true);
            handleDynamicMenu(e);
            return;
        }

        String title = e.getView().getTitle();
        Player player = (Player) e.getWhoClicked();

        if (title.contains("Menú ")) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item.hasItemMeta()) {
                if (item.getType() == Material.ANVIL && item.getItemMeta().getDisplayName().contains("Configurar")) {
                    LoadoutGUI.open(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    return;
                }
                if (item.getType() == Material.BOOK && item.getItemMeta().getDisplayName().contains("Diario de Misiones")) {
                    QuestMenu.open(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.1f);
                    return;
                }
            }
            return;
        }

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

    private void handleDynamicMenu(InventoryClickEvent e) {
        SkyblockMenu menu = (SkyblockMenu) e.getInventory().getHolder();
        MenuItem item = menu.getItem(e.getSlot());
        if (item == null) return;

        Player p = (Player) e.getWhoClicked();
        var handler = Skyworld.getInstance().getManagerHandler();
        EconomyManager eco = handler.getEconomyManager();

        var playerData = handler.getDataManager().getPlayerData(p.getUniqueId());
        StorageBag bag = playerData.getStorageBag();
        String targetId = item.getTargetId().toLowerCase();

        switch (item.getAction()) {
            case BUY -> {
                if (eco.withdraw(p, item.getPrice())) {
                    var template = ItemRegistry.getDropTemplates().get(targetId);
                    if (template != null) {
                        Rarity rarity = Rarity.fromString(template.rarity());
                        ItemStack itemStack = new ItemStack(template.material(), item.getAmount());

                        var formattedName = ColorUtils.getAnimatedName(template.displayName(), rarity);
                        ItemMeta meta = itemStack.getItemMeta();
                        if (meta != null) {
                            meta.displayName(formattedName);
                            itemStack.setItemMeta(meta);
                        }

                        if (template.isEquipment()) {
                            // playerData.getArmory().addItem(itemStack, targetId, rarity);
                            p.sendMessage(ColorUtils.format("&3[Tienda] &fComprado y enviado a la &bArmería&f."));
                        } else {
                            // Mantenemos tu estándar del resto del plugin: Guardar usando el displayName
                            bag.addItem(itemStack, template.displayName(), rarity);
                            p.sendMessage(ColorUtils.format("&a[Tienda] &fComprado y guardado en tu &eInfinibag&f."));
                        }
                    } else {
                        Material mat = Material.matchMaterial(item.getTargetId());
                        if (mat != null) p.getInventory().addItem(new ItemStack(mat, item.getAmount()));
                    }

                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } else {
                    p.sendMessage("§cNo tienes suficiente dinero.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
            case SELL -> {
                // 1. Buscamos la plantilla usando el targetId del menú para conocer su displayName real
                var template = ItemRegistry.getDropTemplates().get(targetId);

                if (template != null) {
                    String finalBagKey = template.displayName(); // El nombre exacto con el que se guarda en la bolsa

                    // 2. Comprobamos y removemos en la StorageBag usando el nombre visual como clave
                    if (bag.hasItem(finalBagKey, item.getAmount())) {
                        bag.removeItem(finalBagKey, item.getAmount());
                        eco.addCoins(p, item.getPrice());

                        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                        p.sendMessage(ColorUtils.format("&a[Tienda] &fVendiste &7" + item.getAmount() + "x " + finalBagKey + " &fpor &e$" + item.getPrice() + " Monedas&a."));
                    } else {
                        p.sendMessage("§cNo tienes suficientes objetos en tu bolsa.");
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                } else {
                    // Fallback por si la id del menú no está registrada en el drops/items.yml
                    p.sendMessage("§cError: No se pudo encontrar la información del ítem para procesar la venta.");
                }
            }
            case CLOSE -> p.closeInventory();
            case COMMAND -> {
                p.closeInventory();
                String cmd = item.getTargetId();
                if (cmd.startsWith("pathtool goto ")) {
                    String questId = cmd.replace("pathtool goto ", "");
                    handler.getQuestManager().setTrackingQuest(p, questId);
                    p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1f, 1.3f);
                } else {
                    p.performCommand(cmd);
                }
            }
            case NONE -> {}
        }
    }
}