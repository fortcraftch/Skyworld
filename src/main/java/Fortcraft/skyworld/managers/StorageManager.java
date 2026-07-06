package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.listeners.HotbarListener;
import Fortcraft.skyworld.listeners.StorageListener;
import Fortcraft.skyworld.storage.StorageBag;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageManager implements Manager {

    private final Map<UUID, StorageBag> playerBags = new HashMap<>();

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(
                new StorageListener(),
                Skyworld.getInstance()
        );    }

    @Override
    public void unload() {
        // Aquí guardarías los datos
        playerBags.clear();
    }

    public StorageBag getStorageBag(UUID uuid){
        return playerBags.get(uuid);
    }
}
