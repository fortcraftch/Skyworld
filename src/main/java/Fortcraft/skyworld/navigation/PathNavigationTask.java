package Fortcraft.skyworld.navigation;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.NavigationManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public class PathNavigationTask extends BukkitRunnable {

    private final NavigationManager navigationManager;

    // --- CONFIGURACIÓN DE PARTÍCULAS ---
    // Administrador: Nodos Verdes / Enlaces Amarillos
    private final Particle.DustOptions nodeOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 128), 1.5f);
    private final Particle.DustOptions connectionOptions = new Particle.DustOptions(Color.fromRGB(255, 200, 0), 0.8f);
    // Jugador: Guía Azul Celeste Brillante
    private final Particle.DustOptions guideOptions = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.5f);

    public PathNavigationTask(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
    }

    @Override
    public void run() {
        var graph = navigationManager.getGraph();
        boolean hasNodes = !graph.isEmpty();

        // Recorremos todos los jugadores online en un único bucle eficiente
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            // 1. GESTIÓN DE GUÍA ACTIVA PARA EL JUGADOR
            if (navigationManager.hasActiveGuide(uuid)) {
                processPlayerGuide(player, uuid);
            }

            // 2. GESTIÓN DE VISUALIZACIÓN DEL MAPA PARA EL ADMINISTRADOR
            if (hasNodes && player.hasPermission("skyworld.admin.paths")) {
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (!handItem.getType().isAir() && handItem.hasItemMeta()) {
                    var pdc = handItem.getItemMeta().getPersistentDataContainer();
                    if (pdc.has(Skyworld.getKey("path_tool"), PersistentDataType.BYTE)) {
                        renderGraphForPlayer(player);
                    }
                }
            }
        }
    }

    /**
     * Procesa la lógica de guía y renderizado de camino para un jugador específico
     */
    private void processPlayerGuide(Player player, UUID uuid) {
        PathDestination destination = navigationManager.getActiveDestination(uuid);
        if (destination == null) return;

        Location playerLoc = player.getLocation();
        Location targetLoc = destination.getLocation();

        // Condición de llegada: Radio de 2 bloques (4.0 en distancia al cuadrado)
        if (playerLoc.getWorld().equals(targetLoc.getWorld()) && playerLoc.distanceSquared(targetLoc) < 4.0) {
            player.sendMessage("§a§l[Guía] §f¡Has llegado a tu destino: " + destination.getDisplayName() + "!");
            player.spawnParticle(Particle.HAPPY_VILLAGER, targetLoc.clone().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);

            // INYECCIÓN DE MISIÓN: Llegada al destino físico
            Skyworld.getInstance().getManagerHandler().getQuestManager()
                    .handleProgress(player, Fortcraft.skyworld.quests.QuestType.VISIT_LOCATION, destination.getId(), 1);

            navigationManager.stopGuiding(player);
            return;
        }

        // Calculamos la ruta óptima desde la posición actual
        List<Location> route = navigationManager.calculateRoute(playerLoc, targetLoc);
        if (route.size() < 2) return;

        // Renderizamos los primeros tramos intermedios de la ruta del jugador
        for (int i = 0; i < Math.min(route.size() - 1, 5); i++) {
            Location start = route.get(i);
            Location end = route.get(i + 1);

            double distance = start.distance(end);
            double spacing = 0.6;

            for (double d = 0; d < distance; d += spacing) {
                double ratio = d / distance;
                double x = start.getX() + (end.getX() - start.getX()) * ratio;
                double y = start.getY() + (end.getY() - start.getY()) * ratio;
                double z = start.getZ() + (end.getZ() - start.getZ()) * ratio;

                // Añadimos +0.3 en el eje Y para que flote elegantemente a la altura de la vista
                player.spawnParticle(Particle.DUST, new Location(start.getWorld(), x, y + 0.3, z), 1, 0, 0, 0, 0, guideOptions);
            }
        }
    }

    /**
     * Renderiza todo el mapa de grafos (Nodos y Conexiones) para un Administrador
     */
    private void renderGraphForPlayer(Player player) {
        var graph = navigationManager.getGraph();
        var world = player.getWorld();

        for (PathNode node : graph.values()) {
            Location locA = node.getLocation();
            if (!locA.getWorld().equals(world)) continue;

            // Dibujar el nodo
            player.spawnParticle(Particle.DUST, locA, 3, 0.1, 0.1, 0.1, 0, nodeOptions);

            // Dibujar sus aristas conexas
            for (UUID neighborId : node.getConnections()) {
                PathNode neighbor = graph.get(neighborId);
                if (neighbor == null) continue;

                // Evitar dibujar la línea dos veces (A->B y B->A)
                if (node.getId().compareTo(neighborId) > 0) continue;

                Location locB = neighbor.getLocation();
                drawConnectionLine(player, locA, locB);
            }
        }
    }

    private void drawConnectionLine(Player player, Location start, Location end) {
        double distance = start.distance(end);
        if (distance == 0) return;

        double spacing = 0.5;

        for (double d = 0; d < distance; d += spacing) {
            double ratio = d / distance;
            double x = start.getX() + (end.getX() - start.getX()) * ratio;
            double y = start.getY() + (end.getY() - start.getY()) * ratio;
            double z = start.getZ() + (end.getZ() - start.getZ()) * ratio;

            Location particleLoc = new Location(start.getWorld(), x, y, z);
            player.spawnParticle(Particle.DUST, particleLoc, 1, 0.2, 0, 0.2, 0, connectionOptions);
        }
    }
}