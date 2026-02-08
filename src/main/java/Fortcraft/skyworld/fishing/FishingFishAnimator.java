package Fortcraft.skyworld.fishing;

import Fortcraft.skyworld.zones.FishingZone;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class FishingFishAnimator extends BukkitRunnable {

    private static final double STEP = Math.PI * 2 / 8; // 45°
    private double angle = 0;

    private final List<FishingZone> zones;

    public FishingFishAnimator(List<FishingZone> zones) {
        this.zones = zones;
    }

    @Override
    public void run() {
        angle += STEP;

        for (FishingZone zone : zones) {
            zone.updateFishPositions(angle);
        }
    }
}

