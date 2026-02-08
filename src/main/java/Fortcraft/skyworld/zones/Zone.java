package Fortcraft.skyworld.zones;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.concurrent.ThreadLocalRandom;

public abstract class Zone {
    protected final String id;
    protected final World world;
    protected final BoundingBox box;
    protected Location spawnPoint; // Nuevo campo

    public Zone(String id, World world, BoundingBox box) {
        this.id = id;
        this.world = world;
        this.box = box;
    }

    public void setSpawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    public BoundingBox getBox() {
        return box;
    }

    public boolean contains(Location loc) {
        return world.equals(loc.getWorld())
                && box.contains(
                loc.getX(),
                loc.getY(),
                loc.getZ()
        );
    }

    public Location getRandomLocation() {
        double x = ThreadLocalRandom.current().nextDouble(box.getMinX(), box.getMaxX());
        double y = ThreadLocalRandom.current().nextDouble(box.getMinY(), box.getMaxY());
        double z = ThreadLocalRandom.current().nextDouble(box.getMinZ(), box.getMaxZ());
        return new Location(world, x, y, z);
    }

    /** Hook para lógica periódica */
    public abstract void tick();
}
