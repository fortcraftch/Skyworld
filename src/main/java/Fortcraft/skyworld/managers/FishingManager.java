package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.fishing.FishingFishAnimator;
import Fortcraft.skyworld.fishing.FishingSession;
import Fortcraft.skyworld.listeners.FishingListener;
import Fortcraft.skyworld.zones.FishingZone;
import Fortcraft.skyworld.zones.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FishingManager implements Manager {

    private final List<FishingZone> zones = new ArrayList<>();
    private final Map<UUID, FishingSession> sessions = new HashMap<>();
    private final Map<UUID, Integer> pendingHooks = new HashMap<>();
    private FishingZone zone = null;
    private FishingFishAnimator fishingFishAnimator = null;

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(
                new FishingListener(this),
                Skyworld.getInstance()
        );

        startResetTask();
        fishingFishAnimator = new FishingFishAnimator(getZones());
        fishingFishAnimator.runTaskTimer(Skyworld.getInstance(), 240L, 10L);
    }

    public void registerZone(FishingZone zone) {
        zones.add(zone);
        zone.createDisplay();
        Bukkit.getScheduler().runTaskLater(Skyworld.getInstance(), zone::spawnFish, 200L);
    }

    public FishingZone getZone(Location loc) {
        for (Zone zone : Skyworld.getInstance().getManagerHandler().getZoneManager().getZones()) {
            if (zone instanceof FishingZone fishingZone && zone.contains(loc)) {
                return fishingZone;
            }
        }
        return null;
    }

    public List<FishingZone> getZones(){
        return zones;
    }

    public FishingSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void registerPendingHook(Player player) {
        UUID uuid = player.getUniqueId();
        if (pendingHooks.containsKey(uuid) || sessions.containsKey(uuid)) return; // Evita iniciar minijuego si ya está en uno

        int taskId = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;

                if (sessions.containsKey(uuid)) {
                    cancel();
                    return;
                }

                if (player.getFishHook() == null) {
                    handleReel(player);
                    cancel();
                    return;
                }

                if (player.getFishHook().isInWater()){
                    zone = getZone(player.getFishHook().getLocation());
                    if (zone != null) {
                        if (zone.canFish()){
                            cancel();
                            return;
                        }
                        player.sendMessage("§cThere aren't any fish here");
                        handleReel(player);
                        cancel();
                        return;
                    }
                    player.sendMessage("§cNot a valid fishing area");
                    handleReel(player);
                    cancel();
                    return;
                }

                if (ticks > 8) {
                    player.sendMessage("§cNot a valid fishing area");
                    handleReel(player);
                    cancel();
                }
            }
        }.runTaskTimer(Skyworld.getInstance(), 0L, 5L).getTaskId();

        pendingHooks.put(uuid, taskId);
    }

    public void startFishing(Player player) {
        if (zone == null) {
            handleReel(player);
            return;
        }

        UUID uuid = player.getUniqueId();
        if (sessions.containsKey(uuid)) return;

        Integer taskId = pendingHooks.remove(uuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);

        int rarity = zone.rollRarity();
        FishingSession session = new FishingSession(player, zone, rarity);
        sessions.put(uuid, session);
        session.startMinigame(rarity);
    }

    public void handleReel(Player player) {
        UUID uuid = player.getUniqueId();

        sessions.remove(uuid);

        Integer taskId = pendingHooks.remove(uuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);

        if (player.getFishHook() != null) player.getFishHook().remove();

        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 1f);
    }

    public void startResetTask() {
        Bukkit.getScheduler().runTaskTimer(Skyworld.getInstance(), () -> {
            for (Zone zone : zones) {
                if (zone instanceof FishingZone fz) {
                    fz.resetAbundance();
                }
            }
            Bukkit.broadcastMessage("§3[Skyworld] §f¡Los bancos de peces se han movido! Las zonas de pesca se han regenerado.");
        }, 20L * 60 * 10, 20L * 60 * 10); // Cada 10 minutos
    }

    @Override
    public void unload() {
        zones.forEach(zone -> {
            if (zone instanceof FishingZone fz) {
                fz.removeDisplay();
            }
        });
        sessions.clear();
        pendingHooks.clear();
        if (fishingFishAnimator != null) {
            fishingFishAnimator.cancel();
        }
    }
}
