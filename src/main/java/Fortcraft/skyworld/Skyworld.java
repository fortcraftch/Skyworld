package Fortcraft.skyworld;

import org.bukkit.plugin.java.JavaPlugin;

import Fortcraft.skyworld.managers.ManagerHandler;
import Fortcraft.skyworld.listeners.MobDisplayListener;

public final class SkyworldCore extends JavaPlugin {

    private static SkyworldCore instance;
    private ManagerHandler managerHandler;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.managerHandler = new ManagerHandler();
        this.managerHandler.loadManagers();

        getServer().getPluginManager().registerEvents(
                new MobDisplayListener(managerHandler.getMobDisplayManager()),
                this
        );

        getLogger().info("Skyworld Core habilitado correctamente.");
    }


    @Override
    public void onDisable() {
        if (managerHandler != null) {
            managerHandler.unloadManagers();
        }

        getLogger().info("Skyworld Core deshabilitado.");
    }

    public static SkyworldCore getInstance() {
        return instance;
    }

    public ManagerHandler getManagerHandler() {
        return managerHandler;
    }

}



