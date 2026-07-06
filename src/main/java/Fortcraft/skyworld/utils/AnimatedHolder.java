package Fortcraft.skyworld.utils;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class AnimatedHolder implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
        return null; // No es necesario instanciarlo directamente aquí
    }
}