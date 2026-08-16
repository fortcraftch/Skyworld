package Fortcraft.skyworld.items;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.stats.CustomStat;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ItemRegistry {

    private static final Map<String, CustomItemData> itemTemplates = new HashMap<>();
    private static final Map<String, CustomDropData> dropTemplates = new HashMap<>();

    public static final NamespacedKey KEY_ID = new NamespacedKey("skyworld", "item_id");
    public static final NamespacedKey KEY_CATEGORY = new NamespacedKey("skyworld", "category");

    public static void load() {
        itemTemplates.clear();
        dropTemplates.clear();

        File itemsFile = new File(Skyworld.getInstance().getDataFolder(), "items.yml");
        if (!itemsFile.exists()) createDefaultItemsConfig(itemsFile);
        loadItems(itemsFile);

        File dropsFile = new File(Skyworld.getInstance().getDataFolder(), "drops.yml");
        if (!dropsFile.exists()) createDefaultDropsConfig(dropsFile);
        loadDrops(dropsFile);

        Skyworld.getInstance().getLogger().info("¡Cargados " + itemTemplates.size() + " ítems y " + dropTemplates.size() + " drops!");
    }

    private static void loadItems(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) continue;

            Map<String, Double> stats = loadStats(section);
            String category = section.getString("category", "ANY").toUpperCase();

            itemTemplates.put(id.toLowerCase(), new CustomItemData(
                    id.toLowerCase(),
                    Material.valueOf(section.getString("material", "BARRIER").toUpperCase()),
                    section.getString("name", id),
                    section.getStringList("lore"),
                    category,
                    section.getString("rarity", "COMUN").toUpperCase(),
                    stats,
                    true
            ));
        }
    }

    private static void loadDrops(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) continue;

            Map<String, Double> stats = loadStats(section);

            dropTemplates.put(id.toLowerCase(), new CustomDropData(
                    id.toLowerCase(),
                    Material.valueOf(section.getString("material", "BARRIER").toUpperCase()),
                    section.getString("name", id),
                    section.getStringList("lore"),
                    section.getString("rarity", "COMUN").toUpperCase(),
                    section.getString("category", "GLOBAL").toUpperCase(),
                    stats,
                    false
            ));
        }
    }

    private static Map<String, Double> loadStats(ConfigurationSection section) {
        Map<String, Double> stats = new HashMap<>();
        ConfigurationSection statsSec = section.getConfigurationSection("custom_stats");
        if (statsSec != null) {
            for (String key : statsSec.getKeys(false)) {
                stats.put(key.toLowerCase(), statsSec.getDouble(key));
            }
        }
        return stats;
    }

    public static ItemStack build(String id) {
        String cleanId = id.toLowerCase();
        if (itemTemplates.containsKey(cleanId)) {
            return buildItem(itemTemplates.get(cleanId));
        }
        if (dropTemplates.containsKey(cleanId)) {
            return buildDrop(dropTemplates.get(cleanId));
        }
        return null;
    }

    private static ItemStack buildItem(CustomItemData data) {
        ItemStack item = new ItemStack(data.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, data.id());
        meta.getPersistentDataContainer().set(Skyworld.ITEM_CATEGORY_KEY, PersistentDataType.STRING, data.category());

        if (data.stats() != null) {
            data.stats().forEach((statKey, value) -> {
                NamespacedKey key = new NamespacedKey("skyworld", statKey);
                meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
            });
        }

        Rarity rarity = Rarity.fromString(data.rarity());
        meta.displayName(rarity.format(data.displayName()).decoration(TextDecoration.ITALIC, false));
        meta.lore(buildLoreWithStats(data.lore(), data.stats()));

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildDrop(CustomDropData data) {
        ItemStack item = new ItemStack(data.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, data.id());

        if (data.category() != null) {
            meta.getPersistentDataContainer().set(Skyworld.ITEM_CATEGORY_KEY, PersistentDataType.STRING, data.category().toUpperCase());
        }

        if (data.stats() != null) {
            data.stats().forEach((statKey, value) -> {
                NamespacedKey key = new NamespacedKey("skyworld", statKey);
                meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
            });
        }

        Rarity rarity = Rarity.fromString(data.rarity());
        meta.displayName(rarity.format(data.displayName()).decoration(TextDecoration.ITALIC, false));
        meta.lore(buildLoreWithStats(data.lore(), data.stats()));

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    private static List<Component> buildLoreWithStats(List<String> rawLore, Map<String, Double> stats) {
        List<Component> finalLore = new ArrayList<>();

        for (String line : rawLore) {
            finalLore.add(ColorUtils.format(line).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }

        if (stats != null && !stats.isEmpty()) {
            finalLore.add(Component.text(" ")); // Espacio separador estético

            for (Map.Entry<String, Double> entry : stats.entrySet()) {
                String rawKey = entry.getKey();
                double value = entry.getValue();

                // 1. Buscamos si existe un CustomStat correspondiente
                CustomStat customStat = CustomStat.fromKey(rawKey);
                String displayName;

                if (customStat != null) {
                    displayName = customStat.getDisplayName(); // Usa el nombre bonito del enum (ej: "☘ Fortuna de Minería")
                } else {
                    // Fallback para stats genéricas o externas (ej: attack_damage -> attack damage)
                    displayName = rawKey.replace("_", " ").toLowerCase();
                }

                // 2. Detección automática si debe llevar porcentaje (%)
                boolean isPercent = rawKey.contains("chance")
                        || (rawKey.contains("damage") && rawKey.contains("crit"))
                        || rawKey.contains("wisdom")
                        || rawKey.contains("luck")
                        || rawKey.contains("fortune")
                        || rawKey.contains("percent");

                // Formato del valor numérico
                String valueString = (value > 0 ? "+" : "") + (value % 1 == 0 ? String.format("%.0f", value) : value);
                if (isPercent) {
                    valueString += "%";
                }

                // 3. Diseño limpio combinando el displayName del enum
                finalLore.add(Component.text("  ▸ ")
                        .color(NamedTextColor.DARK_GRAY)
                        .append(Component.text(displayName + ": ").color(NamedTextColor.GRAY))
                        .append(Component.text(valueString).color(NamedTextColor.GREEN))
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        return finalLore;
    }

    private static void createDefaultItemsConfig(File file) {
        try {
            if (!Skyworld.getInstance().getDataFolder().exists()) Skyworld.getInstance().getDataFolder().mkdirs();
            file.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("novice_sword.material", "WOODEN_SWORD");
            config.set("novice_sword.name", "&fEspada de Novato");
            config.set("novice_sword.category", "WEAPON");
            config.set("novice_sword.rarity", "COMUN");
            config.set("novice_sword.custom_stats.attack_damage", 5.0);
            config.set("novice_sword.custom_stats.crit_chance", 5.0);
            config.set("novice_sword.lore", Arrays.asList("<gray>Una espada básica.", "<yellow>Calidad: Común"));

            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultDropsConfig(File file) {
        try {
            if (!Skyworld.getInstance().getDataFolder().exists()) Skyworld.getInstance().getDataFolder().mkdirs();
            file.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("ancient_amber.material", "HONEYCOMB");
            config.set("ancient_amber.name", "Ámbar Ancestral");
            config.set("ancient_amber.rarity", "RARO");
            config.set("ancient_amber.category", "MINING");
            config.set("ancient_amber.lore", Arrays.asList("<gray>Una resina fósil altamente cotizada."));
            config.set("ancient_amber.custom_stats.exp_given", 25.0);

            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, CustomItemData> getItemTemplates() { return itemTemplates; }
    public static Map<String, CustomDropData> getDropTemplates() { return dropTemplates; }
}