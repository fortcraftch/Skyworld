package Fortcraft.skyworld.foraging;

import Fortcraft.skyworld.logbook.LogbookGUI;
import Fortcraft.skyworld.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ForagingBiome {

    private final String id;
    private final Material icon;
    private final String displayName;

    // Mapa de listas para soportar múltiples drops por tronco/bloque de madera
    private final Map<Material, List<ForagingDrop>> drops = new HashMap<>();
    private final Set<String> uniqueSourceIds = new HashSet<>();

    public ForagingBiome(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = config.getString("display_name", id);
        this.icon = Material.valueOf(config.getString("icon", "IRON_AXE"));

        ConfigurationSection logsSec = config.getConfigurationSection("logs");
        if (logsSec != null) {
            for (String blockKey : logsSec.getKeys(false)) {
                Material sourceMat = Material.valueOf(blockKey.toUpperCase());
                ConfigurationSection blockSec = logsSec.getConfigurationSection(blockKey);

                String sourceName = blockSec.getString("name", blockKey);
                int defRegen = blockSec.getInt("regen_time", 5);

                List<ForagingDrop> blockDrops = new ArrayList<>();
                ConfigurationSection dropsListSec = blockSec.getConfigurationSection("drops");

                // Mapeo adaptado al nuevo sistema global sin duplicación de atributos visuales
                if (dropsListSec != null) {
                    for (String dropId : dropsListSec.getKeys(false)) {
                        ConfigurationSection singleDropSec = dropsListSec.getConfigurationSection(dropId);

                        // El dropId o un item_id explícito define el vínculo con drops.yml
                        String itemId = singleDropSec.getString("item_id", dropId);
                        double weight = singleDropSec.getDouble("weight", 10.0);
                        int amount = singleDropSec.getInt("amount", 1);

                        ForagingDrop drop = new ForagingDrop(
                                sourceMat,
                                sourceName,
                                itemId,
                                weight,
                                amount,
                                defRegen
                        );
                        blockDrops.add(drop);
                    }
                }

                if (!blockDrops.isEmpty()) {
                    drops.put(sourceMat, blockDrops);
                    uniqueSourceIds.add(LogbookGUI.getCleanId(sourceName));
                }
            }
        }
    }

    /**
     * Selecciona un drop aleatorio basado en los pesos configurados para ese tronco.
     */
    public ForagingDrop getWeightedDrop(Material source) {
        List<ForagingDrop> possibleDrops = drops.get(source);
        if (possibleDrops == null || possibleDrops.isEmpty()) return null;

        double totalWeight = possibleDrops.stream().mapToDouble(ForagingDrop::getWeight).sum();
        double randomValue = ThreadLocalRandom.current().nextDouble() * totalWeight;

        double currentWeight = 0;
        for (ForagingDrop drop : possibleDrops) {
            currentWeight += drop.getWeight();
            if (currentWeight >= randomValue) {
                return drop;
            }
        }
        return possibleDrops.get(0);
    }

    public int getTotalUniqueSources() { return uniqueSourceIds.size(); }
    public Set<String> getUniqueSourceIds() { return uniqueSourceIds; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public Collection<ForagingDrop> getAllDrops() {
        return drops.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public ItemStack getGuiIcon(int discoveredCount) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(ColorUtils.format(getDisplayName())
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

            List<Component> lore = new ArrayList<>();

            lore.add(ColorUtils.format("<gray>Descubiertos: <yellow>" + discoveredCount + "/" + getTotalUniqueSources())
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

            lore.add(Component.empty());

            lore.add(ColorUtils.format("<yellow>Click para ver colección")
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}