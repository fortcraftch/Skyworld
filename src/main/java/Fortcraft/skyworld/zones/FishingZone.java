package Fortcraft.skyworld.zones;

import Fortcraft.skyworld.fishing.FishingDrop;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.List;

public class ZoneFishing extends Zone {

    private final List<FishingDrop> drops;

    public ZoneFishing(
            String id,
            World world,
            BoundingBox box,
            List<FishingDrop> drops
    ) {
        super(id, world, box);
        this.drops = drops;
    }

    public List<FishingDrop> getDrops() {
        return drops;
    }

    @Override
    public void tick() {
        // pesca no necesita tick por ahora
    }
}
