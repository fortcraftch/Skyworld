package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.StatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class StatUpdateListener implements Listener {

    // 1. Cuando el jugador entra, calculamos sus stats iniciales
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        StatManager.updateStats(e.getPlayer());
    }

    // 3. Cuando cambia el ítem de su mano usando la rueda del ratón o los números (1-9)
    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent e) {
        // Usamos un pequeño retraso (1 tick) porque en el momento en que se dispara
        // este evento, el ítem en la mano "oficialmente" aún no ha cambiado en el servidor.
        updateWithDelay(e.getPlayer());
    }

    // 4. Cuando mueve ítems dentro de su inventario (Ej: se pone una armadura o mueve un arma)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            updateWithDelay(player);
        }
    }

    // 5. Por seguridad, cuando cierra cualquier inventario actualizamos también
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player player) {
            updateWithDelay(player);
        }
    }

    // 6. Cuando pulsa la tecla 'F' para cambiar el ítem a la mano secundaria
    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        updateWithDelay(e.getPlayer());
    }

    // 7. Cuando suelta un ítem (tecla 'Q')
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        updateWithDelay(e.getPlayer());
    }

    // 8. Cuando recoge un ítem del suelo (podría ir directamente a su mano principal si estaba vacía)
    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {
            updateWithDelay(player);
        }
    }

    /**
     * Método auxiliar para actualizar los stats al siguiente tick (0.05 segundos).
     * Esto es CRÍTICO en Bukkit, porque muchos eventos se disparan ANTES de que
     * el inventario termine de actualizarse internamente.
     */
    private void updateWithDelay(Player player) {
        Bukkit.getScheduler().runTaskLater(Skyworld.getInstance(), () -> {
            if (player.isOnline()) {
                StatManager.updateStats(player);
            }
        }, 1L); // 1 tick de retraso
    }
}