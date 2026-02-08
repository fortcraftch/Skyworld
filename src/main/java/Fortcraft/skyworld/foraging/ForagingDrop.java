package Fortcraft.skyworld.foraging;

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

public class ForagingDrop {

    private final String name;
    private final String dropName;
    private final Material sourceMaterial;
    private final Material dropItem;
    private final int amount;
    private final double weight;
    private final int regenTime;
    private final int exp;
    private final int slot;
    private final Rarity rarity; // 1. Nuevo campo de rareza

    private ItemStack cachedItem;

    public ForagingDrop(String name, String dropName, Material sourceMaterial, Material dropItem, int amount, double weight, int regenTime, int exp, int slot, Rarity rarity) {
        this.name = name;
        this.dropName = dropName;
        this.sourceMaterial = sourceMaterial;
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
        // Ahora usamos el campo amount
        this.cachedItem = new ItemStack(dropItem, amount);
        ItemMeta meta = cachedItem.getItemMeta();

        if (meta != null) {
            meta.displayName(rarity.format(dropName));

            meta.getPersistentDataContainer().set(
                    Skyworld.ITEM_CATEGORY_KEY,
                    PersistentDataType.STRING,
                    "FORAGING"
            );

            cachedItem.setItemMeta(meta);
        }
    }

    public void giveToStorage(Player player) {
        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());

        playerData.getStorageBag().addItem(cachedItem.clone(), name, rarity);
    }

    public static ForagingDrop fromConfig(Material sourceMat, String sourceName, ConfigurationSection dropSection, int parentRegen) {
        if (dropSection == null) return null;
        return new ForagingDrop(
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
    public Material getSourceMaterial() { return sourceMaterial; }
    public int getRegenTime() { return regenTime; }
    public Rarity getRarity() { return rarity; } // Getter útil para el Logbook
    public String getName() { return name; }
    public String getDropName() { return dropName;}
    public Material getDropItem() { return dropItem; }
    public double getWeight() { return weight; } // Getter necesario para el cálculo
    public int getAmount() { return amount; }
    public int getExp() { return exp; }
    public int getSlot() { return slot; }
}