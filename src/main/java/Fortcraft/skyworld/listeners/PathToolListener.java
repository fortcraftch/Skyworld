package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.NavigationManager;
import Fortcraft.skyworld.navigation.PathNode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PathToolListener implements Listener {

    private final NavigationManager navigationManager;
    private final Map<UUID, UUID> lastSelectedNode = new HashMap<>();

    public PathToolListener(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
    }

    @EventHandler
    public void onToolInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("skyworld.admin.paths")) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(Skyworld.getKey("path_tool"), PersistentDataType.BYTE)) return;

        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            var nodeLoc = block.getLocation().add(0.5, 1.0, 0.5);

            // --- DETECCIÓN INTELIGENTE DE INTERSECCIONES ---
            PathNode activeNode = null;
            double threshold = 0.5 * 0.5; // Margen de tolerancia (medio bloque cuadrado)
            PathNode nearest = navigationManager.getNearestNode(nodeLoc);

            if (nearest != null && nearest.getLocation().getWorld().equals(nodeLoc.getWorld())
                    && nearest.getLocation().distanceSquared(nodeLoc) < threshold) {
                activeNode = nearest;
                player.sendMessage("§e[Grafos] Intersección detectada. Conectando con camino existente...");
            } else {
                // Si no hay ninguno cerca, creamos uno nuevo normalmente
                activeNode = new PathNode(nodeLoc);
                navigationManager.addNode(activeNode);
                player.sendMessage("§a[Grafos] Nodo creado en: " + block.getX() + ", " + block.getY() + ", " + block.getZ());
            }

            // Conectar con el nodo anterior (siempre que no sea el mismo nodo)
            if (!player.isSneaking() && lastSelectedNode.containsKey(player.getUniqueId())) {
                UUID previousId = lastSelectedNode.get(player.getUniqueId());
                if (!previousId.equals(activeNode.getId())) {
                    navigationManager.connectNodes(previousId, activeNode.getId());
                    player.sendMessage("§b[Grafos] Enlace creado exitosamente.");
                }
            }

            // Actualizar el último nodo seleccionado del admin
            lastSelectedNode.put(player.getUniqueId(), activeNode.getId());
        }

        else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            var nodeLoc = block.getLocation().add(0.5, 1.0, 0.5);
            double threshold = 0.5 * 0.5;
            PathNode nearest = navigationManager.getNearestNode(nodeLoc);

            // Si hay un nodo exactamente donde hicimos click izquierdo, lo eliminamos
            if (nearest != null && nearest.getLocation().getWorld().equals(nodeLoc.getWorld())
                    && nearest.getLocation().distanceSquared(nodeLoc) < threshold) {

                UUID nodeId = nearest.getId();
                navigationManager.removeNode(nodeId);

                // Si estábamos seleccionando este nodo, limpiamos la selección
                if (nodeId.equals(lastSelectedNode.get(player.getUniqueId()))) {
                    lastSelectedNode.remove(player.getUniqueId());
                }

                player.sendMessage("§c[Grafos] Nodo eliminado junto con sus conexiones.");
            } else {
                // Si hicimos click en un bloque pero no hay nodo, reseteamos selección
                resetSelection(player);
            }
        }

        else if (event.getAction() == Action.LEFT_CLICK_AIR) {
            resetSelection(player);
        }
    }

    private void resetSelection(Player player) {
        if (lastSelectedNode.containsKey(player.getUniqueId())) {
            lastSelectedNode.remove(player.getUniqueId());
            player.sendMessage("§e[Grafos] Selección reseteada. El próximo nodo iniciará un nuevo tramo.");
        }
    }
}