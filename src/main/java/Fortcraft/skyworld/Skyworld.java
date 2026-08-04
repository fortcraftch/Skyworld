package Fortcraft.skyworld;

import Fortcraft.skyworld.commands.NPCCommand;
import Fortcraft.skyworld.commands.PartyCommand;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.party.PlayerLeaveListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import Fortcraft.skyworld.managers.ManagerHandler;
import Fortcraft.skyworld.listeners.MobDisplayListener;

public final class Skyworld extends JavaPlugin {

    public static NamespacedKey ITEM_CATEGORY_KEY;
    private static Skyworld instance;
    private ManagerHandler managerHandler;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        ITEM_CATEGORY_KEY = new NamespacedKey(this, "item_category");

        ItemRegistry.load();

        this.managerHandler = new ManagerHandler();
        this.managerHandler.loadManagers();

        getServer().getPluginManager().registerEvents(new MobDisplayListener(managerHandler.getMobDisplayManager()), this);
        getServer().getPluginManager().registerEvents(new PlayerLeaveListener(), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {

            commands.registrar().register(NPCCommand.getInstance().addCommands());
            commands.registrar().register(PartyCommand.getInstance().addCommands());

        });

        getLogger().info("Skyworld Core habilitado correctamente.");
    }

    public static NamespacedKey getKey(String key) {
        return new NamespacedKey(getInstance(), key);
    }

    @Override
    public void onDisable() {
        if (managerHandler != null) {
            managerHandler.unloadManagers();
        }

        getLogger().info("Skyworld Core deshabilitado.");
    }

    public static Skyworld getInstance() {
        return instance;
    }

    public ManagerHandler getManagerHandler() {
        return managerHandler;
    }
}



