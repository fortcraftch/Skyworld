package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.fishing.FishingSession;
import Fortcraft.skyworld.managers.FishingManager;

import Fortcraft.skyworld.zones.FishingZone;
import Fortcraft.skyworld.zones.Zone;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public class FishingListener implements Listener {

    private final FishingManager manager;

    public FishingListener(FishingManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {

        Player player = event.getPlayer();
        FishingSession session = manager.getSession(player);

        switch (event.getState()) {

            case FISHING -> {
                // El jugador lanza la caña
                if (session == null) manager.registerPendingHook(player);
            }

            case BITE -> {
                event.setCancelled(true); // cancelamos solo para iniciar el minijuego
                manager.startFishing(player);
            }

            case REEL_IN, FAILED_ATTEMPT -> {

                if (session == null || !session.isActive()) {
                    // Solo si no hay minijuego activo
                    manager.handleReel(player);
                } else {
                    event.setCancelled(true); // No permitir recoger la caña mientras el minijuego está activo
                    session.onClick();
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        int cx = e.getChunk().getX();
        int cz = e.getChunk().getZ();

        // Buscamos en nuestras zonas (que están en memoria desde el onEnable)
        for (Zone zone : manager.getZones()) {
            if (!(zone instanceof FishingZone fz)) continue;

            // Calculamos el chunk del centro de la zona
            int zoneChunkX = (int) fz.getBox().getCenterX() >> 4;
            int zoneChunkZ = (int) fz.getBox().getCenterZ() >> 4;

            // Si el chunk que se acaba de cargar es donde debe ir el holograma...
            if (cx == zoneChunkX && cz == zoneChunkZ) {
                fz.createDisplay();
            }
        }
    }
}

