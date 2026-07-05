package Fortcraft.skyworld.commands;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.NavigationManager;
import Fortcraft.skyworld.navigation.PathDestination;
import Fortcraft.skyworld.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PathToolCommand implements CommandExecutor {

    private final NavigationManager navigationManager;

    public PathToolCommand(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("skyworld.admin.paths")) {
            sender.sendMessage(ColorUtils.format("&cNo tienes permisos para usar este comando."));
            return true;
        }

        // Subcomandos de administración de datos
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("save")) {
                navigationManager.savePaths();
                sender.sendMessage(ColorUtils.format("&a&l[Grafos] &fCaminos guardados con éxito en paths.yml."));
                return true;
            }
            else if (args[0].equalsIgnoreCase("reload")) {
                navigationManager.loadPaths();
                sender.sendMessage(ColorUtils.format("&e&l[Grafos] &fCaminos recargados desde el archivo paths.yml."));
                return true;
            }
            if (args[0].equalsIgnoreCase("setdest") && args.length >= 3 && sender instanceof Player player) {
                String id = args[1];
                // Unir el resto de argumentos como el nombre del destino
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    nameBuilder.append(args[i]).append(" ");
                }
                String displayName = nameBuilder.toString().trim().replace("&", "§");

                PathDestination destination = new PathDestination(id, player.getLocation(), displayName);
                navigationManager.registerDestination(destination);
                navigationManager.savePaths(); // Guardado inmediato
                player.sendMessage("§a[Grafos] Destino '" + id + "' registrado correctamente en tu ubicación.");
                return true;
            }

            if (args[0].equalsIgnoreCase("goto") && args.length >= 2 && sender instanceof Player player) {
                navigationManager.startGuiding(player, args[1]);
                return true;
            }

            if (args[0].equalsIgnoreCase("stop") && sender instanceof Player player) {
                navigationManager.stopGuiding(player);
                player.sendMessage("§e[Guía] Ruta cancelada.");
                return true;
            }
        }

        // Si no hay argumentos o no coinciden, entregamos el ítem (requiere ser jugador)
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cUsa /pathtool <save|reload> desde la consola.");
            return true;
        }

        player.getInventory().addItem(createPathTool());
        player.sendMessage(ColorUtils.format("&a&l[Grafos] &f¡Herramienta de caminos recibida!"));
        return true;
    }

    private ItemStack createPathTool() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.format("&b&lHerramienta de Caminos"));
            meta.lore(List.of(
                    Component.empty(),
                    ColorUtils.format("&eClick Derecho en bloque:"),
                    ColorUtils.format(" &7-> Crea o conecta un nodo (Soporta intersecciones)."),
                    Component.empty(),
                    ColorUtils.format("&eClick Izquierdo en bloque:"),
                    ColorUtils.format(" &7-> Elimina un nodo (Soporta intersecciones)."),
                    Component.empty(),
                    ColorUtils.format("&eComandos de utilidad:"),
                    ColorUtils.format(" &7-> /pathtool save  &8(Guardar cambios)"),
                    ColorUtils.format(" &7-> /pathtool reload  &8(Recargar del archivo)"),
                    Component.empty(),
                    ColorUtils.format("&8&oMapeador Skyworld")
            ));
            meta.getPersistentDataContainer().set(Skyworld.getKey("path_tool"), PersistentDataType.BYTE, (byte) 1);
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }
}