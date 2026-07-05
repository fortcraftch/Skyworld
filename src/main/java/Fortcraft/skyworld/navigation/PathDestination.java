package Fortcraft.skyworld.navigation;

import org.bukkit.Location;

public class PathDestination {
    private final String id;
    private final Location location;
    private final String displayName;

    public PathDestination(String id, Location location, String displayName) {
        this.id = id;
        this.location = location;
        this.displayName = displayName;
    }

    public String getId() { return id; }
    public Location getLocation() { return location; }
    public String getDisplayName() { return displayName; }
}