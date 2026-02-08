package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.RegionManager;
import Fortcraft.skyworld.zones.RegionZone;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionListener implements Listener {

    private final Map<UUID, RegionZone> currentRegions = new HashMap<>();

    private final RegionManager manager;

    public RegionListener(RegionManager manager) { // <- debe ser public
        this.manager = manager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();

        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) return;

        RegionZone newZone = Skyworld.getInstance()
                                .getManagerHandler()
                                .getRegionManager()
                                .getRegion(player.getLocation());

        RegionZone oldZone = currentRegions.get(player.getUniqueId());

        if (oldZone == newZone) return;

        // Salida
        if (oldZone != null) {
            oldZone.onExit(player);
        }

        // Entrada
        if (newZone != null) {
            newZone.onEnter(player);
            currentRegions.put(player.getUniqueId(), newZone);
        } else {
            currentRegions.remove(player.getUniqueId());
        }
    }
}

