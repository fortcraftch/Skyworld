package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.managers.FarmManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

// 1. El Listener independiente
public class FarmPhysicsListener implements Listener {

    private final FarmManager farmManager;

    public FarmPhysicsListener(FarmManager farmManager) {
        this.farmManager = farmManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent e) {
        Block block = e.getBlock();

        if (farmManager.isChainableMaterial(block.getType())) {
            if (farmManager.getZoneAt(block.getLocation()) != null) {
                e.setCancelled(true);
            }
        }
    }
}