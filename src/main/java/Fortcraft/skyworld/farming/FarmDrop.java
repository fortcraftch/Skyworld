package Fortcraft.skyworld.farming;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.Rarity;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class FarmDrop {
    private final String name;           // ID del Source (ej: "Trigo Místico")
    private final String dropName;       // Nombre visual del item
    private final Material sourceBlock;
    private final Material dropItem;
    private final int amount;            // Cantidad de items
    private final double weight;         // Peso para probabilidad
    private final int regenTime;
    private final int exp;
    private final int slot;
    private final Rarity rarity; // 1. Nuevo campo de rareza

    private ItemStack cachedItem;

    public FarmDrop(String name, String dropName, Material sourceBlock, Material dropItem, int amount, double weight, int regenTime, int exp, int slot, Rarity rarity) {
        this.name = name;
        this.dropName = dropName;
        this.sourceBlock = sourceBlock;
        this.dropItem = dropItem;
        this.amount = amount;
        this.weight = weight;
        this.regenTime = regenTime;
        this.exp = exp;
        this.slot = slot;
        this.rarity = rarity;

        prebuild();
    }

    private void prebuild() {
        this.cachedItem = new ItemStack(dropItem, amount);
        ItemMeta meta = cachedItem.getItemMeta();

        if (meta != null) {
            meta.displayName(rarity.format(dropName));

            meta.getPersistentDataContainer().set(
                    Skyworld.ITEM_CATEGORY_KEY,
                    PersistentDataType.STRING,
                    "FARMING"
            );

            cachedItem.setItemMeta(meta);
        }
    }

    public void giveToStorage(Player player) {
        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());

        playerData.getStorageBag().addItem(cachedItem.clone(), name, rarity);
    }

    public static FarmDrop fromConfig(Material sourceMat, String sourceName, ConfigurationSection dropSection, int parentRegen) {
        if (dropSection == null) return null;

        return new FarmDrop(
                sourceName,
                dropSection.getString("name", sourceName),
                sourceMat,
                Material.valueOf(dropSection.getString("drop").toUpperCase()),
                dropSection.getInt("amount", 1),
                dropSection.getDouble("weight", 10.0),
                parentRegen,
                dropSection.getInt("exp", 1),
                dropSection.getInt("slot", -1),
                Rarity.fromString(dropSection.getString("class", "comun"))
        );
    }

    // Getters
    public String getName() { return name; }
    public String getDropName() { return dropName;}
    public Rarity getRarity() { return rarity; } // Getter útil para el Logbook
    public Material getSourceBlock() { return sourceBlock; }
    public Material getDropItem() { return dropItem; }
    public int getAmount() { return amount; }
    public double getWeight() { return weight; }
    public int getRegenTime() { return regenTime; }
    public int getExp() { return exp; }
    public int getSlot() { return slot; }
}