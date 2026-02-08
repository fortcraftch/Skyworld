package Fortcraft.skyworld.fishing;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.TextDecoration;

import static Fortcraft.skyworld.logbook.LogbookGUI.getCleanId;

public class FishingDrop {

    private final String id;
    private final String groupId; // El ID que se usa en la bitácora (ej: "salmon")
    private final Material material;
    private final String name;     // Nombre base (ej: "Salmón")
    private final String sizeName; // Variante (ej: "Grande")
    private final double weight;
    private final int rarity;
    private final int slot;
    private final Rarity speciesRarity; // La del grupo (ej: Salmón -> Raro)
    private final Rarity variantRarity; // La de la variante (ej: ★★★ -> Legendario)

    private ItemStack cachedItem;

    public FishingDrop(String id, String groupId, Material material, String name, String sizeName,
                       double weight, int rarity, int slot, Rarity speciesRarity, Rarity variantRarity) {
        this.id = id;
        this.groupId = groupId;
        this.material = material;
        this.name = name;
        this.sizeName = sizeName;
        this.weight = weight;
        this.rarity = rarity;
        this.slot = slot;
        this.speciesRarity = speciesRarity;
        this.variantRarity = variantRarity;

        prebuild();
    }

    public void prebuild() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String speciesColor = speciesRarity.getColorCode();
            String variantColor = variantRarity.getColorCode();

            String displayName =  speciesColor + name;
            if (!sizeName.isEmpty()) {
                displayName += " " + variantColor + sizeName;
            }

            meta.displayName(ColorUtils.format(displayName));

            meta.getPersistentDataContainer().set(
                    Skyworld.ITEM_CATEGORY_KEY,
                    PersistentDataType.STRING,
                    "FISHING"
            );

            item.setItemMeta(meta);
        }
        this.cachedItem = item;
    }

    public void giveToStorage(Player player) {
        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());

        playerData.getStorageBag().addItem(cachedItem.clone(), groupId, speciesRarity);
        playerData.discover(getCleanId(this.id));
    }

    // Getters
    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getGroupId() { return groupId; }
    public String getName() { return name; }
    public String getSizeName() { return sizeName; }
    public Rarity getSpeciesRarity() { return speciesRarity; }
    public Rarity getVariantRarity() { return variantRarity; }
    public double getWeight() { return weight; }
    public int getRarity() { return rarity; }
    public int getSlot() { return slot; }

    public static FishingDrop fromConfig(String id, String groupId, String baseName, String sizeName,
                                         ConfigurationSection variantSection, Rarity speciesRarity) {

        Material mat = Material.valueOf(variantSection.getString("material", "COD").toUpperCase());
        double weight = variantSection.getDouble("weight", 1.0);
        int rarity = variantSection.getInt("rarity", 1);
        int slot = variantSection.getInt("slot", -1);

        Rarity variantRarity = Rarity.fromString(variantSection.getString("class", "comun"));

        return new FishingDrop(id, groupId, mat, baseName, sizeName, weight, rarity, slot, speciesRarity, variantRarity);
}
}