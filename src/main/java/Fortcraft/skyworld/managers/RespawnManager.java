package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.listeners.RespawnListener;
import org.bukkit.Bukkit;

public class RespawnManager implements Manager {

    private final ZoneManager zoneManager;

    public RespawnManager(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(
                new RespawnListener(zoneManager),
                Skyworld.getInstance()
        );
    }

    @Override
    public void unload() {}
}
