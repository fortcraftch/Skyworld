package Fortcraft.skyworld.items;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.Rarity;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
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
    public static final NamespacedKey KEY_FORTUNE = new NamespacedKey("skyworld", "mining_fortune");

    public static void load() {
        itemTemplates.clear();
        dropTemplates.clear();

        File itemsFile = new File(Skyworld.getInstance().getDataFolder(), "items.yml");
        if (!itemsFile.exists()) createDefaultItemsConfig(itemsFile);
        loadItems(itemsFile);

        File dropsFile = new File(Skyworld.getInstance().getDataFolder(), "drops.yml");
        if (!dropsFile.exists()) createDefaultDropsConfig(dropsFile);
        loadDrops(dropsFile);
    }

    private static void loadItems(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) continue;

            Map<Attribute, Double> stats = loadBukkitStats(section, file.getName());
            Map<String, Double> customStats = loadCustomStats(section);

            String category = section.getString("category", "ANY").toUpperCase();
            itemTemplates.put(id, new CustomItemData(
                    id,
                    Material.valueOf(section.getString("material", "BARRIER").toUpperCase()),
                    section.getString("name", id),
                    section.getStringList("lore"),
                    category,
                    section.getString("rarity", "COMUN").toUpperCase(),
                    stats,
                    customStats,
                    true
            ));
        }
    }

    private static void loadDrops(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) continue;

            Map<String, Double> customStats = loadCustomStats(section);

            dropTemplates.put(id, new CustomDropData(
                    id,
                    Material.valueOf(section.getString("material", "BARRIER").toUpperCase()),
                    section.getString("name", id),
                    section.getStringList("lore"),
                    section.getString("rarity", "COMUN").toUpperCase(),
                    section.getString("category", "GLOBAL").toUpperCase(), // Captura la categoría del drops.yml
                    customStats,
                    false
            ));
        }
    }

    private static Map<Attribute, Double> loadBukkitStats(ConfigurationSection section, String fileName) {
        Map<Attribute, Double> stats = new HashMap<>();
        ConfigurationSection statsSec = section.getConfigurationSection("stats");
        if (statsSec != null) {
            for (String key : statsSec.getKeys(false)) {
                try {
                    stats.put(Attribute.valueOf(key.toUpperCase()), statsSec.getDouble(key));
                } catch (IllegalArgumentException e) {
                    Skyworld.getInstance().getLogger().warning("Atributo Bukkit inválido en " + fileName + ": " + key);
                }
            }
        }
        return stats;
    }

    private static Map<String, Double> loadCustomStats(ConfigurationSection section) {
        Map<String, Double> customStats = new HashMap<>();
        ConfigurationSection customSec = section.getConfigurationSection("custom_stats");
        if (customSec != null) {
            for (String key : customSec.getKeys(false)) {
                customStats.put(key.toLowerCase(), customSec.getDouble(key));
            }
        }
        return customStats;
    }

    public static ItemStack build(String id) {
        if (itemTemplates.containsKey(id)) {
            return buildItem(itemTemplates.get(id));
        }
        if (dropTemplates.containsKey(id)) {
            return buildDrop(dropTemplates.get(id));
        }
        return null;
    }

    private static ItemStack buildItem(CustomItemData data) {
        ItemStack item = new ItemStack(data.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, data.id());

        // CAMBIO AQUÍ: Usamos la llave global de Skyworld
        meta.getPersistentDataContainer().set(Skyworld.ITEM_CATEGORY_KEY, PersistentDataType.STRING, data.category());

        if (data.customStats().containsKey("mining_fortune")) {
            meta.getPersistentDataContainer().set(KEY_FORTUNE, PersistentDataType.DOUBLE, data.customStats().get("mining_fortune"));
        }

        meta.setDisplayName(data.displayName().replace("&", "§"));

        List<net.kyori.adventure.text.Component> finalLore = new ArrayList<>();
        data.lore().forEach(line -> finalLore.add(ColorUtils.format(line).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
        meta.lore(finalLore);

        data.stats().forEach((attr, value) -> {
            meta.addAttributeModifier(attr, new AttributeModifier(UUID.randomUUID(), "skyworld_stat", value, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        });

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildDrop(CustomDropData data) {
        ItemStack item = new ItemStack(data.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, data.id());

        // CAMBIO AQUÍ: Usamos la llave global de Skyworld
        if (data.category() != null) {
            meta.getPersistentDataContainer().set(Skyworld.ITEM_CATEGORY_KEY, PersistentDataType.STRING, data.category().toUpperCase());
        }

        Rarity rarity = Rarity.fromString(data.rarity());
        meta.displayName(rarity.format(data.displayName()));

        List<net.kyori.adventure.text.Component> finalLore = new ArrayList<>();
        data.lore().forEach(line -> finalLore.add(ColorUtils.format(line).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
        meta.lore(finalLore);

        item.setItemMeta(meta);
        return item;
    }

    private static void createDefaultItemsConfig(File file) {
        try {
            if (!Skyworld.getInstance().getDataFolder().exists()) Skyworld.getInstance().getDataFolder().mkdirs();
            file.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("novice_sword.material", "WOODEN_SWORD");
            config.set("novice_sword.name", "Espada de Novato");
            config.set("novice_sword.category", "WEAPON");
            config.set("novice_sword.rarity", "COMUN");
            config.set("novice_sword.stats.GENERIC_ATTACK_DAMAGE", 5.0);
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
            config.set("ancient_amber.category", "MINING"); // Añadido por defecto para consistencia
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