package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.listeners.RegionListener;
import Fortcraft.skyworld.zones.RegionZone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.EventHandler;

import java.util.*;

public class RegionManager implements Manager, Listener {

    private final List<RegionZone> zones = new ArrayList<>();
    private final Map<UUID, RegionZone> currentRegions = new HashMap<>();

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(new RegionListener(this), Skyworld.getInstance());
    }

    @Override
    public void unload() {
        zones.clear();
    }

    public void registerZone(RegionZone zone) {
        zones.add(zone);
    }

    public RegionZone getRegion(Location loc) {
        for (RegionZone zone : zones) {
            if (zone.contains(loc)) {
                return zone;
            }
        }
        return null;
    }

    public List<RegionZone> getZones() {
        return zones;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) return;

        RegionZone oldZone = currentRegions.get(player.getUniqueId());
        RegionZone newZone = getRegion(event.getTo());

        if (oldZone == newZone) return;

        // Salida
        if (oldZone != null) oldZone.onExit(player);

        // Entrada
        if (newZone != null) newZone.onEnter(player);

        if (newZone != null) {
            currentRegions.put(player.getUniqueId(), newZone);
        } else {
            currentRegions.remove(player.getUniqueId());
        }
    }
}
