package Fortcraft.skyworld.menu;

import Fortcraft.skyworld.utils.ColorUtils; // Asumiendo que creaste la clase del paso anterior
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MenuItem {
    private final int slot;
    private final ItemStack icon;
    private final MenuAction action;
    private final String targetId;
    private final int amount;
    private final double price;

    public MenuItem(int slot, Material mat, String name, List<String> lore, String actionStr, String targetId, int amount, double price) {
        this.slot = slot;
        this.action = MenuAction.valueOf(actionStr.toUpperCase());
        this.targetId = targetId;
        this.amount = amount;
        this.price = price;

        this.icon = new ItemStack(mat);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ColorUtils.colorize(name));

        List<String> coloredLore = new ArrayList<>();
        if (lore != null) {
            for (String l : lore) coloredLore.add(ColorUtils.colorize(l));
        }
        meta.setLore(coloredLore);
        icon.setItemMeta(meta);
    }

    public int getSlot() { return slot; }
    public ItemStack getIcon() { return icon; }
    public MenuAction getAction() { return action; }
    public String getTargetId() { return targetId; }
    public int getAmount() { return amount; }
    public double getPrice() { return price; }

    public enum MenuAction {
        BUY, SELL, CLOSE, COMMAND, NONE
    }
}