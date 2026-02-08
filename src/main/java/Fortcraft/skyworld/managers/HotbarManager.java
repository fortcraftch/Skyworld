package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.hotbar.HotbarItems;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.utils.PlayerMode;
import Fortcraft.skyworld.listeners.HotbarListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HotbarManager implements Manager {

    private final Map<UUID, PlayerMode> playerModes = new HashMap<>();

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(
                new HotbarListener(this),
                Skyworld.getInstance()
        );
    }

    @Override
    public void unload() {
        playerModes.clear();
    }

    public PlayerMode getMode(Player player) {
        return playerModes.getOrDefault(player.getUniqueId(), PlayerMode.GLOBAL);
    }

    public void setMode(Player player, PlayerMode mode) {
        playerModes.put(player.getUniqueId(), mode);
        applyLoadout(player);
        giveHotbarItems(player);
    }

    public void nextMode(Player player) {
        PlayerMode current = getMode(player);
        applyLoadout(player);
        setMode(player, current.next());
    }

    public void giveHotbarItems(Player player) {
        PlayerMode mode = getMode(player);

        // Slot 6: Bitácora
        player.getInventory().setItem(6, HotbarItems.getLogbook(mode));
        // Slot 7: Bolsa
        player.getInventory().setItem(7, HotbarItems.getStorage(mode));
        // Slot 8: Menú
        player.getInventory().setItem(8, HotbarItems.getMenu(mode));
    }

    public void applyLoadout(Player player) {
        var data = Skyworld.getInstance().getManagerHandler().getDataManager().getPlayerData(player.getUniqueId());
        PlayerMode mode = getMode(player);
        Map<Integer, String> loadout = data.getLoadoutForMode(mode);

        // Limpiar slots 0-5
        for (int i = 0; i <= 5; i++) player.getInventory().setItem(i, null);

        // Entregar ítems custom
        if (loadout != null) {
            loadout.forEach((slotIndex, itemId) -> {
                ItemStack item = ItemRegistry.build(itemId); // <--- Usa el nuevo sistema
                if (item != null) {
                    player.getInventory().setItem(slotIndex, item);
                }
            });
        }
        player.updateInventory();
    }
}
