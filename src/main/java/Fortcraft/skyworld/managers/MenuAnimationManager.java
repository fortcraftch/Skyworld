package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.AnimatedHolder;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class MenuAnimationManager implements Manager, Listener {

    private BukkitTask globalTask;

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, Skyworld.getInstance());

        // UN SOLO BUCLE PARA TODO EL SERVIDOR
        globalTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Inventory topInv = player.getOpenInventory().getTopInventory();

                    // Si el jugador no tiene un inventario animado abierto, lo saltamos
                    if (topInv == null || !(topInv.getHolder() instanceof AnimatedHolder)) continue;

                    // Procesamos los ítems del inventario
                    boolean updated = false;
                    ItemStack[] contents = topInv.getContents();

                    for (ItemStack item : contents) {
                        if (item == null || !item.hasItemMeta()) continue;

                        ItemMeta meta = item.getItemMeta();
                        var pdc = meta.getPersistentDataContainer();

                        // Verificación estricta de las llaves compartidas
                        if (pdc.has(Skyworld.getKey("rarity"), PersistentDataType.STRING)) {
                            String rarityName = pdc.get(Skyworld.getKey("rarity"), PersistentDataType.STRING);
                            String originalName = pdc.get(Skyworld.getKey("original_name"), PersistentDataType.STRING);

                            try {
                                Rarity rarity = Rarity.valueOf(rarityName);
                                if (isAnimatedRarity(rarity) && originalName != null) {
                                    meta.displayName(ColorUtils.getAnimatedName(originalName, rarity));
                                    item.setItemMeta(meta);
                                    updated = true;
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    // Forzar la actualización visual en el cliente del jugador si hubo cambios
                    if (updated) {
                        player.updateInventory();
                    }
                }
            }
        }.runTaskTimer(Skyworld.getInstance(), 1L, 3L);
    }

    @Override
    public void unload() {
        if (globalTask != null) {
            globalTask.cancel();
        }
    }

    private boolean isAnimatedRarity(Rarity rarity) {
        if (rarity == null) return false;
        String n = rarity.name().toUpperCase();
        return n.equals("LEGENDARIO") || n.equals("EXOTICO");
    }
}