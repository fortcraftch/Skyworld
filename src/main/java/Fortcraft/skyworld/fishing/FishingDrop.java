package Fortcraft.skyworld.fishing;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.Rarity;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

import static Fortcraft.skyworld.logbook.LogbookGUI.getCleanId;

public class FishingDrop {

    private final String itemId;
    private final String groupId;
    private final String groupName;
    private final double weight;
    private final int minigameRarity;
    private final int slot;
    private final String size;
    private final Rarity speciesRarity;

    public FishingDrop(String itemId, String groupId, String groupName,
                       double weight, int sizeNum, int slot, Rarity speciesRarity, String size) {
        this.itemId = itemId;
        this.groupId = groupId;
        this.groupName = groupName;
        this.weight = weight;
        this.minigameRarity = sizeNum;
        this.slot = slot;
        this.size = size;
        this.speciesRarity = speciesRarity;
    }

    public void giveToStorage(Player player) {
        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());

        // Generamos el item original desde la plantilla
        ItemStack item = ItemRegistry.build(itemId);
        if (item == null) return;

        // INYECCIÓN DE TAMAÑO: Si tiene un tamaño asignado, modificamos el nombre del Drop permanentemente
        if (size != null && !size.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                // Añadimos " (S)" manteniendo el formato y el color original del ItemMeta
                meta.displayName(meta.displayName().append(Component.text(" (" + size + ")")));
                item.setItemMeta(meta);
            }
        }

        Rarity itemRarity = getSpeciesRarity();

        playerData.getStorageBag().addItem(item, groupId, itemRarity);
        playerData.discover(getCleanId(this.groupId));
        playerData.discover(getCleanId(this.itemId));
    }

    // --- Getters Dinámicos para LogbookGUI ---
    public Material getMaterial() {
        var template = ItemRegistry.getDropTemplates().get(itemId);
        return template != null ? template.material() : Material.COD;
    }

    public String getItemId() { return itemId; }
    public String getGroupId() { return groupId; }
    public String getName() { return groupName; }
    public double getWeight() { return weight; }
    public int getRarity() { return (int) (minigameRarity + speciesRarity.getRarityNumber()); } // Rareza de dificultad del minijuego
    public int getSlot() { return slot; }
    public Rarity getSpeciesRarity() { return speciesRarity; }
    public String getSize() {return size;}

    public static FishingDrop fromConfig(String groupId, String groupName, Rarity speciesRarity, ConfigurationSection variantSection) {
        String itemId = variantSection.getString("item_id", "cod");
        double weight = variantSection.getDouble("weight", 1.0);
        int slot = variantSection.getInt("slot", -1);

        var template = ItemRegistry.getDropTemplates().get(itemId);
        String sizeStr = "";
        int sizeNum = 0;

        if (template != null) {
            Map<String, Double> customStats = template.customStats();
            if (customStats.get("size") != null){
                sizeNum = customStats.get("size").intValue();
            }

            // Extraemos size (numerico a string S,M,L,XL)
            if (customStats.containsKey("size")) {
                sizeStr = switch (sizeNum) {
                    case 1 -> "S";
                    case 2 -> "M";
                    case 3 -> "L";
                    case 4 -> "XL";
                    case 0 -> ""; // Vacío si es un tamaño atípico o único
                    default -> "";
                };
            }
        }

        return new FishingDrop(itemId, groupId, groupName, weight, sizeNum, slot, speciesRarity, sizeStr);
    }
}