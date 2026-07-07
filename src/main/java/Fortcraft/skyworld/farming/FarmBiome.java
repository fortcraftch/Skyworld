package Fortcraft.skyworld.farming;

import Fortcraft.skyworld.logbook.LogbookGUI;
import Fortcraft.skyworld.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class FarmBiome {
    private final String id;
    private final String displayName;
    private final Material icon;

    // Cambiado a Map de Listas para soportar múltiples drops por cultivo
    private final Map<Material, List<FarmDrop>> drops = new HashMap<>();
    private final Set<String> uniqueSourceIds = new HashSet<>(); // Ahora guardará IDs

    public FarmBiome(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = config.getString("display_name", id);
        this.icon = Material.valueOf(config.getString("icon", "GOLDEN_HOE"));

        ConfigurationSection cropsSec = config.getConfigurationSection("crops");
        if (cropsSec != null) {
            for (String blockKey : cropsSec.getKeys(false)) {
                Material sourceMat = Material.valueOf(blockKey.toUpperCase());
                ConfigurationSection cropSec = cropsSec.getConfigurationSection(blockKey);

                String sourceId = blockKey.toLowerCase(); // ID Único de la fuente
                String sourceName = cropSec.getString("name", blockKey);
                int defRegen = cropSec.getInt("regen_time", 5);

                List<FarmDrop> cropDrops = new ArrayList<>();
                ConfigurationSection dropsListSec = cropSec.getConfigurationSection("drops");

                // Cambia la lectura interna del bucle del constructor en FarmBiome por esto:
                if (dropsListSec != null) {
                    for (String dropId : dropsListSec.getKeys(false)) {
                        ConfigurationSection singleDropSec = dropsListSec.getConfigurationSection(dropId);

                        // El dropId de la sección o una key explícita será el itemId unificado
                        String itemId = singleDropSec.getString("item_id", dropId);
                        double weight = singleDropSec.getDouble("weight", 10.0);
                        int amount = singleDropSec.getInt("amount", 1);

                        FarmDrop drop = new FarmDrop(
                                sourceMat,
                                sourceId,
                                sourceName,
                                itemId,
                                weight,
                                amount,
                                defRegen
                        );
                        cropDrops.add(drop);
                    }
                }

                if (!cropDrops.isEmpty()) {
                    drops.put(sourceMat, cropDrops);
                    uniqueSourceIds.add(sourceId); // Guardamos la ID estricta
                }
            }
        }
    }

    /**
     * Selecciona un drop aleatorio basado en los pesos del cultivo.
     */
    public FarmDrop getWeightedDrop(Material source) {
        List<FarmDrop> possibleDrops = drops.get(source);
        if (possibleDrops == null || possibleDrops.isEmpty()) return null;

        double totalWeight = possibleDrops.stream().mapToDouble(FarmDrop::getWeight).sum();
        double randomValue = ThreadLocalRandom.current().nextDouble() * totalWeight;

        double currentWeight = 0;
        for (FarmDrop drop : possibleDrops) {
            currentWeight += drop.getWeight();
            if (currentWeight >= randomValue) {
                return drop;
            }
        }
        return possibleDrops.getFirst();
    }

    public int getTotalUniqueSources() { return uniqueSourceIds.size(); }
    public Set<String> getUniqueSourceIds() { return uniqueSourceIds; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public Collection<FarmDrop> getAllDrops() {
        return drops.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public ItemStack getGuiIcon(int discoveredCount) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(ColorUtils.format(getDisplayName())
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();

            lore.add(ColorUtils.format("<gray>Descubiertos: <yellow>" + discoveredCount + "/" + getTotalUniqueSources())
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());

            lore.add(ColorUtils.format("<yellow>Click para ver colección")
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}