package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.managers.EconomyManager;
import Fortcraft.skyworld.menu.LoadoutGUI;
import Fortcraft.skyworld.menu.MenuItem;
import Fortcraft.skyworld.menu.QuestMenu;
import Fortcraft.skyworld.menu.SkyblockMenu;
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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;

        // 1. Manejo de SkyblockMenu dinámico
        if (e.getInventory().getHolder() instanceof SkyblockMenu) {
            e.setCancelled(true);
            handleDynamicMenu(e);
            return;
        }

        // 2. Manejo de LoadoutGUI mediante Holder (evita fallos de lectura de títulos)
        if (e.getInventory().getHolder() instanceof LoadoutGUI.LoadoutHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item == null || !item.hasItemMeta()) return;

            var meta = item.getItemMeta();
            var pdc = meta.getPersistentDataContainer();

            // A. Si el ítem tiene KEY_ITEM_ID, proviene del Selector
            if (pdc.has(LoadoutGUI.KEY_ITEM_ID, PersistentDataType.STRING)) {
                String itemId = pdc.get(LoadoutGUI.KEY_ITEM_ID, PersistentDataType.STRING);
                int slotIndex = pdc.getOrDefault(LoadoutGUI.KEY_SLOT_INDEX, PersistentDataType.INTEGER, 0);

                if ("BACK".equals(itemId)) {
                    LoadoutGUI.open(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
                    return;
                }

                var managerHandler = Skyworld.getInstance().getManagerHandler();
                var data = managerHandler.getDataManager().getPlayerData(player.getUniqueId());
                var hotbarManager = managerHandler.getHotbarManager();
                PlayerMode mode = hotbarManager.getMode(player);
                HotbarSlot slotEnum = HotbarSlot.fromIndex(slotIndex);

                if ("REMOVE".equals(itemId)) {
                    data.setLoadoutItem(mode, slotEnum, null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
                } else {
                    data.setLoadoutItem(mode, slotEnum, itemId);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.5f);
                }

                hotbarManager.applyLoadout(player);
                LoadoutGUI.open(player);
                return;
            }

            // B. Si el ítem tiene KEY_SLOT_INDEX únicamente, proviene del Loadout principal
            if (pdc.has(LoadoutGUI.KEY_SLOT_INDEX, PersistentDataType.INTEGER)) {
                int slotIndex = pdc.get(LoadoutGUI.KEY_SLOT_INDEX, PersistentDataType.INTEGER);
                LoadoutGUI.openSelector(player, slotIndex);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                return;
            }
            return;
        }

        // 3. Menú principal / Navegación fija por título
        String title = e.getView().getTitle();

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
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        // Cancela arrastrar ítems en cualquier menú guiado por Holder
        if (e.getInventory().getHolder() instanceof SkyblockMenu ||
                e.getInventory().getHolder() instanceof LoadoutGUI.LoadoutHolder) {
            e.setCancelled(true);
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
                    ItemStack itemStack = ItemRegistry.build(targetId);

                    if (itemStack != null) {
                        itemStack.setAmount(item.getAmount());

                        var template = ItemRegistry.getDropTemplates().get(targetId);
                        Rarity rarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

                        if (template != null && template.isEquipment()) {
                            p.sendMessage(ColorUtils.format("&3[Tienda] &fComprado y enviado a la &bArmería&f."));
                        } else {
                            bag.addItemWithoutDiscovery(itemStack, targetId, rarity);
                            p.sendMessage(ColorUtils.format("&a[Tienda] &fComprado y guardado en tu &eInfinibag&f."));
                        }
                    } else {
                        Material mat = Material.matchMaterial(item.getTargetId());
                        if (mat != null) {
                            p.getInventory().addItem(new ItemStack(mat, item.getAmount()));
                            p.sendMessage(ColorUtils.format("&a[Tienda] &fComprado objeto básico de Minecraft."));
                        }
                    }

                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } else {
                    p.sendMessage(ColorUtils.format("&cNo tienes suficiente dinero."));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
            case SELL -> {
                var template = ItemRegistry.getDropTemplates().get(targetId);

                if (bag.hasItem(targetId, item.getAmount())) {
                    bag.removeItem(targetId, item.getAmount());
                    eco.addCoins(p, item.getPrice());

                    Rarity rarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;
                    var formattedName = ColorUtils.getAnimatedName(template != null ? template.displayName() : targetId, rarity);

                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                    p.sendMessage(ColorUtils.format("&a[Tienda] &fVendiste &7" + item.getAmount() + "x ")
                            .append(formattedName)
                            .append(ColorUtils.format(" &fpor &e$" + item.getPrice() + " Monedas&a.")));
                } else {
                    p.sendMessage(ColorUtils.format("&cNo tienes suficientes objetos en tu bolsa."));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
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