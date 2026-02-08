package Fortcraft.skyworld.menu;

import Fortcraft.skyworld.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SkyblockMenu implements InventoryHolder {

    private final String id;
    private final String title;
    private final int size;
    private final Map<Integer, MenuItem> items = new HashMap<>();

    public SkyblockMenu(String id, String title, int size) {
        this.id = id;
        this.title = ColorUtils.colorize(title);
        this.size = size;
    }

    public void addItem(MenuItem item) {
        items.put(item.getSlot(), item);
    }

    public MenuItem getItem(int slot) {
        return items.get(slot);
    }

    public void open(Player p) {
        // Al pasar 'this' como holder, Bukkit sabe que este inventario pertenece a esta clase
        Inventory inv = Bukkit.createInventory(this, size, ColorUtils.format(title));

        for (MenuItem item : items.values()) {
            inv.setItem(item.getSlot(), item.getIcon());
        }

        p.openInventory(inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null; // No necesario para este uso
    }
}