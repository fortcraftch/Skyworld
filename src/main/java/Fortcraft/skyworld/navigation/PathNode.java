package Fortcraft.skyworld.navigation;

import org.bukkit.Location;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PathNode {
    private final UUID id;
    private final Location location;
    private final Set<UUID> connections;

    public PathNode(Location location) {
        this.id = UUID.randomUUID();
        this.location = location;
        this.connections = new HashSet<>();
    }

    public PathNode(UUID id, Location location, Set<UUID> connections) {
        this.id = id;
        this.location = location;
        this.connections = connections;
    }

    public UUID getId() { return id; }
    public Location getLocation() { return location; }
    public Set<UUID> getConnections() { return connections; }

    public void addConnection(UUID nodeId) {
        connections.add(nodeId);
    }

    public void removeConnection(UUID nodeId) {
        connections.remove(nodeId);
    }
}