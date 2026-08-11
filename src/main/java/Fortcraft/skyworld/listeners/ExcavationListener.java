package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.excavation.ExcavationNode;
import Fortcraft.skyworld.managers.ExcavationManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class ExcavationListener implements Listener {

    private final ExcavationManager manager;

    public ExcavationListener(ExcavationManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onBrush(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getPlayer().getInventory().getItemInMainHand().getType() != Material.BRUSH) return;

        Block clicked = e.getClickedBlock();
        if (clicked == null) return;

        Location loc = clicked.getLocation();
        ExcavationNode node = manager.getNode(loc);

        if (node != null) {
            Player p = e.getPlayer();

            manager.startBrushingSession(p, node, clicked);
        }
    }
}