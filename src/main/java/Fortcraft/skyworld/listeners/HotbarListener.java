package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.hotbar.HotbarItems;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.PlayerMode;
import Fortcraft.skyworld.logbook.LogbookGUI;
import Fortcraft.skyworld.managers.DataManager;
import Fortcraft.skyworld.managers.HotbarManager;
import Fortcraft.skyworld.menu.GlobalMenu;
import Fortcraft.skyworld.storage.StorageGUI;
import Fortcraft.skyworld.Skyworld;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class HotbarListener implements Listener {

    private final HotbarManager manager;
    private final DataManager dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();

    private static final int MAX_LOCKED_SLOT = 5;

    private final Map<UUID, Long> dropCooldowns = new HashMap<>();
    private final Map<UUID, Long> modeChangeCooldowns = new HashMap<>();
    private static final long MODE_CHANGE_DELAY = 250;

    public HotbarListener(HotbarManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        dataManager.getPlayerData(uuid);
        manager.setMode(player, PlayerMode.GLOBAL);
        manager.applyLoadout(player);
        dropCooldowns.remove(uuid);
        modeChangeCooldowns.remove(uuid);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        dataManager.removePlayerData(uuid);
        dropCooldowns.remove(uuid);
        modeChangeCooldowns.remove(uuid);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;

        if (HotbarItems.isHotbarItem(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
            return;
        }

        int slot = e.getPlayer().getInventory().getHeldItemSlot();
        if (slot <= MAX_LOCKED_SLOT) {
            e.setCancelled(true);
            dropCooldowns.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return; // Permitir admins

        if (HotbarItems.isHotbarItem(e.getCurrentItem()) || HotbarItems.isHotbarItem(e.getCursor())) {
            e.setCancelled(true);
            return;
        }

        if (e.getSlotType() == InventoryType.SlotType.QUICKBAR && e.getSlot() <= MAX_LOCKED_SLOT) {
            e.setCancelled(true);
            return;
        }

        if (e.getClick().isKeyboardClick()) {
            if (e.getHotbarButton() >= 0 && e.getHotbarButton() <= MAX_LOCKED_SLOT) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;

        if (HotbarItems.isHotbarItem(e.getOffHandItem()) || HotbarItems.isHotbarItem(e.getMainHandItem())) {
            e.setCancelled(true);
            return;
        }

        int slot = e.getPlayer().getInventory().getHeldItemSlot();
        if (slot <= MAX_LOCKED_SLOT) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;

        ItemStack item = e.getItem();
        if (!HotbarItems.isHotbarItem(item)) return;

        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (dropCooldowns.containsKey(uuid)) {
            if (System.currentTimeMillis() - dropCooldowns.get(uuid) < 500) return;
        }

        Action action = e.getAction();

        if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)
                && (Objects.equals(HotbarItems.getItemType(item), HotbarItems.TYPE_MENU))) {

            long now = System.currentTimeMillis();
            if (modeChangeCooldowns.containsKey(uuid)) {
                if (now - modeChangeCooldowns.get(uuid) < MODE_CHANGE_DELAY) return;
            }
            modeChangeCooldowns.put(uuid, now);
            manager.nextMode(p);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.8f);
        }

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);

            String type = HotbarItems.getItemType(item);
            PlayerMode currentMode = manager.getMode(p);

            if (type == null) return;

            switch (type) {
                case HotbarItems.TYPE_MENU -> {
                    GlobalMenu.open(p, currentMode);
                    p.playSound(p.getLocation(), Sound.UI_LOOM_TAKE_RESULT, 1f, 1f);
                }
                case HotbarItems.TYPE_STORAGE -> {
                    var playerData = dataManager.getPlayerData(uuid);
                    StorageGUI.open(p, playerData.getStorageBag(), currentMode, 0);
                    p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 0.5f, 1.2f);
                }
                case HotbarItems.TYPE_LOGBOOK -> {
                    LogbookGUI.open(p, currentMode, null);
                    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                }
            }
        }
    }
}