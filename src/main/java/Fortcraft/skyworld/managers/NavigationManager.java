package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.commands.PathToolCommand;
import Fortcraft.skyworld.listeners.PathToolListener;
import Fortcraft.skyworld.navigation.PathDestination;
import Fortcraft.skyworld.navigation.PathNavigationTask;
import Fortcraft.skyworld.navigation.PathNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NavigationManager implements Manager {

    // Grafo global: ID del nodo -> Objeto Nodo
    private final Map<UUID, PathNode> graph = new HashMap<>();
    private final Map<UUID, PathDestination> activeGuides = new HashMap<>();
    private final Map<String, PathDestination> destinations = new HashMap<>();

    // Archivos para la persistencia de datos
    private File file;
    private FileConfiguration config;

    public void addNode(PathNode node) {
        graph.put(node.getId(), node);
    }

    public Map<UUID, PathNode> getGraph() {
        return graph;
    }

    /**
     * Registra un nuevo punto de destino en el mapa en memoria
     */
    public void registerDestination(PathDestination destination) {
        if (destination != null && destination.getId() != null) {
            destinations.put(destination.getId().toLowerCase(), destination);
        }
    }

    /**
     * Une dos nodos de forma bidireccional
     */
    public void connectNodes(UUID idA, UUID idB) {
        PathNode nodeA = graph.get(idA);
        PathNode nodeB = graph.get(idB);
        if (nodeA != null && nodeB != null) {
            nodeA.addConnection(idB);
            nodeB.addConnection(idA);
        }
    }

    /**
     * Encuentra el nodo más cercano a una ubicación cualquiera
     */
    public PathNode getNearestNode(Location loc) {
        if (graph.isEmpty() || loc.getWorld() == null) return null;
        PathNode nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (PathNode node : graph.values()) {
            if (!node.getLocation().getWorld().equals(loc.getWorld())) continue;
            double dist = node.getLocation().distanceSquared(loc);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = node;
            }
        }
        return nearest;
    }

    /**
     * Calcula la ruta más corta entre un punto de inicio y un final dinámicos
     */
    public List<Location> calculateRoute(Location start, Location end) {
        List<Location> fullPath = new ArrayList<>();
        if (graph.isEmpty()) return fullPath;

        PathNode startNode = getNearestNode(start);
        PathNode endNode = getNearestNode(end);

        if (startNode == null || endNode == null) return fullPath;

        // Si los nodos base están listos, ejecutamos Dijkstra
        List<PathNode> nodePath = findShortestPath(startNode.getId(), endNode.getId());

        if (nodePath.isEmpty()) return fullPath;

        // Construimos el camino final: Origen real -> Nodos del grafo -> Destino real
        fullPath.add(start);
        for (PathNode n : nodePath) {
            fullPath.add(n.getLocation());
        }
        fullPath.add(end);

        return fullPath;
    }

    private List<PathNode> findShortestPath(UUID startId, UUID endId) {
        Map<UUID, Double> distances = new HashMap<>();
        Map<UUID, UUID> previousNodes = new HashMap<>();
        PriorityQueue<UUID> queue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (UUID nodeId : graph.keySet()) {
            distances.put(nodeId, Double.MAX_VALUE);
        }
        distances.put(startId, 0.0);
        queue.add(startId);

        while (!queue.isEmpty()) {
            UUID currentId = queue.poll();
            if (currentId.equals(endId)) break;

            PathNode currentNode = graph.get(currentId);
            if (currentNode == null) continue;

            for (UUID neighborId : currentNode.getConnections()) {
                PathNode neighbor = graph.get(neighborId);
                if (neighbor == null) continue;

                double newDist = distances.get(currentId) + currentNode.getLocation().distance(neighbor.getLocation());
                if (newDist < distances.get(neighborId)) {
                    distances.put(neighborId, newDist);
                    previousNodes.put(neighborId, currentId);
                    queue.add(neighborId);
                }
            }
        }

        // Reconstruir el camino desde el final hacia atrás
        LinkedList<PathNode> path = new LinkedList<>();
        UUID current = endId;
        if (!previousNodes.containsKey(current) && !current.equals(startId)) {
            return Collections.emptyList(); // No hay camino disponible
        }

        while (current != null) {
            path.addFirst(graph.get(current));
            current = previousNodes.get(current);
        }
        return path;
    }

    // --- PERSISTENCIA DE DATOS (YAML) ---

    private void setupFile() {
        Skyworld plugin = Skyworld.getInstance();
        this.file = new File(plugin.getDataFolder(), "paths.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear el archivo paths.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void savePaths() {
        // Limpieza preventiva de las secciones completas
        config.set("nodes", null);
        config.set("destinations", null);

        // 1. Guardar Nodos del Grafo
        for (PathNode node : graph.values()) {
            String path = "nodes." + node.getId().toString();
            Location loc = node.getLocation();

            config.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());

            List<String> connectionStrings = new ArrayList<>();
            for (UUID targetId : node.getConnections()) {
                connectionStrings.add(targetId.toString());
            }
            config.set(path + ".connections", connectionStrings);
        }

        // 2. Guardar Puntos de Destino
        for (PathDestination dest : destinations.values()) {
            String path = "destinations." + dest.getId().toLowerCase();
            Location loc = dest.getLocation();

            config.set(path + ".name", dest.getDisplayName());
            config.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
        }

        try {
            config.save(file);
            Skyworld.getInstance().getLogger().info("[NavigationManager] Guardados " + graph.size() + " nodos y " + destinations.size() + " destinos en paths.yml.");
        } catch (IOException e) {
            Skyworld.getInstance().getLogger().severe("Error al salvar paths.yml: " + e.getMessage());
        }
    }

    public void loadPaths() {
        graph.clear();
        destinations.clear();

        // 1. Cargar Nodos del Grafo
        if (config.contains("nodes")) {
            var nodesSection = config.getConfigurationSection("nodes");
            if (nodesSection != null) {
                for (String uuidStr : nodesSection.getKeys(false)) {
                    UUID id = UUID.fromString(uuidStr);
                    String path = "nodes." + uuidStr;

                    String worldName = config.getString(path + ".world", "world");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;

                    double x = config.getDouble(path + ".x");
                    double y = config.getDouble(path + ".y");
                    double z = config.getDouble(path + ".z");
                    Location loc = new Location(world, x, y, z);

                    Set<UUID> connections = new HashSet<>();
                    List<String> connectionStrings = config.getStringList(path + ".connections");
                    for (String targetStr : connectionStrings) {
                        connections.add(UUID.fromString(targetStr));
                    }

                    PathNode node = new PathNode(id, loc, connections);
                    graph.put(id, node);
                }
            }
        }

        // 2. Cargar Puntos de Destino
        if (config.contains("destinations")) {
            var destSection = config.getConfigurationSection("destinations");
            if (destSection != null) {
                for (String id : destSection.getKeys(false)) {
                    String path = "destinations." + id;
                    String name = config.getString(path + ".name");
                    String worldName = config.getString(path + ".world", "world");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;

                    double x = config.getDouble(path + ".x");
                    double y = config.getDouble(path + ".y");
                    double z = config.getDouble(path + ".z");
                    Location loc = new Location(world, x, y, z);

                    PathDestination dest = new PathDestination(id, loc, name);
                    destinations.put(id.toLowerCase(), dest);
                }
            }
        }

        Skyworld.getInstance().getLogger().info("[NavigationManager] Cargados " + graph.size() + " nodos y " + destinations.size() + " destinos desde paths.yml.");
    }

    // Métodos de verificación rápidos requeridos por la Task unificada
    public boolean hasActiveGuide(UUID playerUUID) {
        return activeGuides.containsKey(playerUUID);
    }

    public PathDestination getActiveDestination(UUID playerUUID) {
        return activeGuides.get(playerUUID);
    }

    public void startGuiding(Player player, String destinationId) {
        PathDestination dest = destinations.get(destinationId.toLowerCase());
        if (dest == null) {
            player.sendMessage("§cEl destino especificado no existe.");
            return;
        }
        activeGuides.put(player.getUniqueId(), dest);
    }

    public void stopGuiding(Player player) {
        activeGuides.remove(player.getUniqueId());
    }

    public void removeNode(UUID nodeId) {
        if (!graph.containsKey(nodeId)) return;

        graph.remove(nodeId);

        for (PathNode node : graph.values()) {
            node.removeConnection(nodeId);
        }
        savePaths();
    }

    // --- MÉTODOS DEL MANAGER INTERFACE ---

    @Override
    public void load() {
        setupFile();
        loadPaths();

        Bukkit.getPluginManager().registerEvents(new PathToolListener(this), Skyworld.getInstance());

        PluginCommand command = Skyworld.getInstance().getCommand("pathtool");
        if (command != null) {
            command.setExecutor(new PathToolCommand(this));
        }

        // --- LANZAMOS LA TAREA UNIFICADA ---
        new PathNavigationTask(this).runTaskTimer(Skyworld.getInstance(), 20L, 10L);

        Skyworld.getInstance().getLogger().info("[NavigationManager] Sistema unificado iniciado a 10 Ticks.");
    }

    @Override
    public void unload() {
        savePaths();
        activeGuides.clear();
        graph.clear();
        destinations.clear();
    }
}