package Fortcraft.skyworld.fishing;

import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FishingBiome {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<FishingDrop> drops = new ArrayList<>();
    private final Set<String> uniqueSourceIds = new HashSet<>();

    private int maxRarity = 1;

    public FishingBiome(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = config.getString("display_name", id);
        this.icon = Material.valueOf(config.getString("icon", "WATER_BUCKET"));

        loadDrops(config.getConfigurationSection("species"));
        calculateStats();
    }

    private void loadDrops(ConfigurationSection section) {
        if (section == null) return;
        uniqueSourceIds.clear();

        for (String groupKey : section.getKeys(false)) { // ej: "bacalao"
            ConfigurationSection speciesSec = section.getConfigurationSection(groupKey);
            String groupName = speciesSec.getString("name", groupKey);

            uniqueSourceIds.add(groupKey);

            ConfigurationSection dropsSec = speciesSec.getConfigurationSection("drops");
            if (dropsSec != null && !dropsSec.getKeys(false).isEmpty()) {

                // 1. Escoger la rareza de la clase basándonos en el PRIMER drop registrado
                String firstDropKey = dropsSec.getKeys(false).iterator().next();
                String firstItemId = dropsSec.getConfigurationSection(firstDropKey).getString("item_id");
                var firstTemplate = Fortcraft.skyworld.items.ItemRegistry.getDropTemplates().get(firstItemId);
                Rarity speciesRarity = firstTemplate != null ? Rarity.fromString(firstTemplate.rarity()) : Rarity.COMUN;

                // 2. Iterar sobre todos los tamaños (pesajes)
                for (String dropKey : dropsSec.getKeys(false)) {
                    ConfigurationSection variantSec = dropsSec.getConfigurationSection(dropKey);
                    drops.add(FishingDrop.fromConfig(groupKey, groupName, speciesRarity, variantSec));
                }
            }
        }
    }

    private void calculateStats() {
        for (FishingDrop drop : drops) {
            if (drop.getRarity() > maxRarity) maxRarity = drop.getRarity();
        }
    }

    public FishingDrop rollDrop(int rarityGoal) {
        List<FishingDrop> exactPool = new ArrayList<>();
        double totalWeight = 0;

        for (FishingDrop drop : drops) {
            if (drop.getRarity() == rarityGoal) {
                exactPool.add(drop);
                totalWeight += drop.getWeight();
            }
        }

        if (!exactPool.isEmpty()) {
            double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
            double current = 0;
            for (FishingDrop drop : exactPool) {
                current += drop.getWeight();
                if (roll <= current) return drop;
            }
        }

        return drops.stream()
                .filter(d -> d.getRarity() <= rarityGoal)
                .max(Comparator.comparingInt(FishingDrop::getRarity))
                .orElse(drops.isEmpty() ? null : drops.getFirst());
    }

    public int rollRarity() {
        if (drops.isEmpty()) return 1;

        Map<Integer, Double> rarityWeights = new HashMap<>();
        for (FishingDrop drop : drops) {
            rarityWeights.put(drop.getRarity(), rarityWeights.getOrDefault(drop.getRarity(), 0.0) + drop.getWeight());
        }

        double totalWeight = rarityWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double acc = 0;

        List<Integer> sortedRarities = new ArrayList<>(rarityWeights.keySet());
        Collections.sort(sortedRarities);

        for (int r : sortedRarities) {
            acc += rarityWeights.get(r);
            if (roll <= acc) return r;
        }
        return 1;
    }

    public int getTotalUniqueSources() { return uniqueSourceIds.size(); }
    public List<FishingDrop> getDrops() { return drops; }
    public String getId() { return id; }
    public Set<String> getUniqueSourceIds() { return uniqueSourceIds; }

    public String getDisplayName() {
        return displayName;
    }

    public ItemStack getGuiIcon(int discoveredCount) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(ColorUtils.format(getDisplayName())
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtils.format("&7Descubiertos: &e" + discoveredCount + "/" + getTotalUniqueSources())
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(ColorUtils.format("&eClick para ver colección")
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}