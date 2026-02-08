package Fortcraft.skyworld.items;

import Fortcraft.skyworld.Skyworld;
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

    private static final Map<String, CustomItemData> templates = new HashMap<>();

    public static final NamespacedKey KEY_ID = new NamespacedKey("skyworld", "item_id");
    public static final NamespacedKey KEY_CATEGORY = new NamespacedKey("skyworld", "category");
    public static final NamespacedKey KEY_FORTUNE = new NamespacedKey("skyworld", "mining_fortune");

    public static void load() {
        templates.clear();
        File file = new File(Skyworld.getInstance().getDataFolder(), "items.yml");
        if (!file.exists()) createDefaultConfig(file);

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) continue;

            // 1. CARGAR ATRIBUTOS BUKKIT (DAÑO, VELOCIDAD...)
            Map<Attribute, Double> stats = new HashMap<>();
            ConfigurationSection statsSec = section.getConfigurationSection("stats");
            if (statsSec != null) {
                for (String key : statsSec.getKeys(false)) {
                    try {
                        stats.put(Attribute.valueOf(key.toUpperCase()), statsSec.getDouble(key));
                    } catch (IllegalArgumentException e) {
                        Skyworld.getInstance().getLogger().warning("Atributo Bukkit inválido: " + key);
                    }
                }
            }

            // 2. CARGAR STATS CUSTOM (FORTUNA, ETC)
            Map<String, Double> customStats = new HashMap<>();
            ConfigurationSection customSec = section.getConfigurationSection("custom_stats");
            if (customSec != null) {
                for (String key : customSec.getKeys(false)) {
                    customStats.put(key.toLowerCase(), customSec.getDouble(key));
                }
            }

            templates.put(id, new CustomItemData(
                    id,
                    Material.valueOf(section.getString("material", "BARRIER").toUpperCase()),
                    section.getString("name", "§c" + id),
                    section.getStringList("lore"),
                    section.getString("category", "ANY").toUpperCase(),
                    stats,
                    customStats // Ahora pasamos los customStats al constructor
            ));
        }
    }

    public static ItemStack build(String id) {
        CustomItemData data = templates.get(id);
        if (data == null) return null;

        ItemStack item = new ItemStack(data.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // IDs base
        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, id);
        meta.getPersistentDataContainer().set(KEY_CATEGORY, PersistentDataType.STRING, data.category());

        // --- NUEVO: GRABAR STATS CUSTOM EN EL ITEM ---
        if (data.customStats().containsKey("mining_fortune")) {
            meta.getPersistentDataContainer().set(KEY_FORTUNE, PersistentDataType.DOUBLE, data.customStats().get("mining_fortune"));
        }

        // Estética y Atributos Bukkit (como ya lo tenías)
        meta.setDisplayName(data.displayName().replace("&", "§"));
        List<String> finalLore = new ArrayList<>();
        data.lore().forEach(line -> finalLore.add(line.replace("&", "§")));
        meta.setLore(finalLore);

        data.stats().forEach((attr, value) -> {
            meta.addAttributeModifier(attr, new AttributeModifier(UUID.randomUUID(), "skyworld_stat", value, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        });

        item.setItemMeta(meta);
        return item;
    }

    private static void createDefaultConfig(File file) {
        try {
            if (!Skyworld.getInstance().getDataFolder().exists()) Skyworld.getInstance().getDataFolder().mkdirs();
            file.createNewFile();

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("novice_sword.material", "WOODEN_SWORD");
            config.set("novice_sword.name", "&fEspada de Novato");
            config.set("novice_sword.category", "WEAPON");
            config.set("novice_sword.stats.GENERIC_ATTACK_DAMAGE", 5.0);
            config.set("novice_sword.lore", Arrays.asList("&7Una espada básica.", "&eCalidad: Comun"));

            config.set("apple.material", "APPLE");
            config.set("apple.name", "&cManzana Roja");
            config.set("apple.category", "CONSUMABLE");
            config.set("apple.lore", Arrays.asList("&7Restaura hambre."));

            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, CustomItemData> getTemplates() {
        return templates;
    }
}