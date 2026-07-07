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
import java.util.Objects;

import static Fortcraft.skyworld.logbook.LogbookGUI.getCleanId;

public class FishingDrop {

    private final String itemId;
    private final String groupId;
    private final String groupName;
    private final double weight;
    private final int slot;
    private final Rarity speciesRarity;

    public FishingDrop(String itemId, String groupId, String groupName,
                       double weight, int slot, Rarity speciesRarity) {
        this.itemId = itemId;
        this.groupId = groupId;
        this.groupName = groupName;
        this.weight = weight;
        this.slot = slot;
        this.speciesRarity = speciesRarity;
    }

    public void giveToStorage(Player player) {
        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());

        // Generamos el item original desde la plantilla
        ItemStack item = ItemRegistry.build(itemId);
        if (item == null) return;

        // Obtención dinámica del tamaño para el nombre visual
        String currentSize = getSize();
        if (currentSize != null && !currentSize.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                meta.displayName(meta.displayName().append(Component.text(" (" + currentSize + ")")));
                item.setItemMeta(meta);
            }
        }

        Rarity itemRarity = getSpeciesRarity();

        playerData.getStorageBag().addItemWithoutDiscovery(item, this.itemId, itemRarity);

        playerData.discover(player.getUniqueId(), this.groupId.toLowerCase(), this.groupName);
        playerData.discover(getCleanId(this.itemId).toLowerCase());
    }

    // --- Getters Dinámicos (Solucionan el orden de carga del servidor) ---

    public int getSizeNum() {
        var template = ItemRegistry.getDropTemplates().get(itemId);
        if (template != null && template.customStats() != null) {
            Number sizeObj = template.customStats().get("size");
            if (sizeObj != null) {
                return sizeObj.intValue();
            }
        }
        return 0;
    }

    public String getSize() {
        return switch (getSizeNum()) {
            case 1 -> "S";
            case 2 -> "M";
            case 3 -> "L";
            case 4 -> "XL";
            default -> "";
        };
    }

    public int getRarity() {
        return (int) (getSizeNum() + speciesRarity.getRarityNumber());
    }

    public Material getMaterial() {
        var template = ItemRegistry.getDropTemplates().get(itemId);
        return template != null ? template.material() : Material.COD;
    }

    public String getItemId() { return itemId; }
    public String getGroupId() { return groupId; }
    public String getName() { return groupName; }
    public double getWeight() { return weight; }
    public int getSlot() { return slot; }
    public Rarity getSpeciesRarity() { return speciesRarity; }

    public static FishingDrop fromConfig(String groupId, String groupName, Rarity speciesRarity, ConfigurationSection variantSection) {
        String itemId = variantSection.getString("item_id", "cod");
        double weight = variantSection.getDouble("weight", 1.0);
        int slot = variantSection.getInt("slot", -1);

        return new FishingDrop(itemId, groupId, groupName, weight, slot, speciesRarity);
    }
}