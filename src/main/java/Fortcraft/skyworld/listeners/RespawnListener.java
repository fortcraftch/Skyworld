package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.RegionManager;
import Fortcraft.skyworld.managers.ZoneManager;
import Fortcraft.skyworld.zones.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class RespawnListener implements Listener {

    private final ZoneManager manager;

    public RespawnListener(ZoneManager manager) { // <- debe ser public
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        if (e.getFinalDamage() >= player.getHealth()) {
            e.setCancelled(true);

            // 1. Restaurar estadísticas
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.setFallDistance(0); // Importante para que no muera al caer de nuevo

            // 2. Determinar destino
            Location targetSpawn = getTargetLocation(player);

            player.teleport(targetSpawn);

            player.sendMessage("§cYou died and lost some loot");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.5f);
        }
    }

    private Location getTargetLocation(Player player) {
        Location playerLoc = player.getLocation();

        // Buscar en las zonas cargadas en el Manager
        for (Zone zone : manager.getZones()) {
            if (zone.contains(playerLoc) && zone.getSpawnPoint() != null) {
                return zone.getSpawnPoint();
            }
        }

        // Si no hay zona, cargar el spawn global desde config.yml
        FileConfiguration config = Skyworld.getInstance().getConfig();
        if (config.contains("global-spawn")) {
            return new Location(
                    Bukkit.getWorld(config.getString("global-spawn.world", "world")),
                    config.getDouble("global-spawn.x"),
                    config.getDouble("global-spawn.y"),
                    config.getDouble("global-spawn.z"),
                    (float) config.getDouble("global-spawn.yaw"),
                    (float) config.getDouble("global-spawn.pitch")
            );
        }

        // Fallback final: El spawn del mundo principal
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }
}

