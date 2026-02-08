package Fortcraft.skyworld.mining;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.fishing.FishingDrop;
import Fortcraft.skyworld.foraging.ForagingDrop;
import Fortcraft.skyworld.managers.StorageManager;
import Fortcraft.skyworld.storage.StorageBag;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.file.FileStore;

public class MiningDrop {
    private final Material source;
    private final Material drop;
    private final String name;
    private final String dropName;
    private final int amount;
    private final double weight;
    private final Material transformTo;
    private final int regenTime;
    private final int slot;
    private final Rarity rarity; // 1. Nuevo campo de rareza

    private ItemStack cachedItem;

    public MiningDrop(Material source, Material drop, String name, String dropName, int amount, double weight, Material transformTo, int regenTime, int slot, Rarity rarity) {
        this.source = source;
        this.drop = drop;
        this.name = name;
        this.dropName = dropName;
        this.amount = amount;
        this.weight = weight;
        this.transformTo = transformTo;
        this.regenTime = regenTime;
        this.slot = slot;
        this.rarity = rarity;

        prebuild(); // Precargamos al instanciar
    }

    private void prebuild() {
        this.cachedItem = new ItemStack(drop, amount);
        ItemMeta meta = cachedItem.getItemMeta();
        if (meta != null) {
            meta.displayName(rarity.format(dropName));

            meta.getPersistentDataContainer().set(Skyworld.ITEM_CATEGORY_KEY,
                    org.bukkit.persistence.PersistentDataType.STRING, "MINING");

            cachedItem.setItemMeta(meta);
        }
    }

    public void giveToStorage(Player player) {
        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();

        var playerData = dataManager.getPlayerData(player.getUniqueId());

        playerData.getStorageBag().addItem(cachedItem.clone(), name, rarity);
    }

    public static MiningDrop fromConfig(Material sourceMat, String sourceName, ConfigurationSection dropSection, Material parentTransform, int parentRegen) {
        return new MiningDrop(
                sourceMat,
                Material.valueOf(dropSection.getString("drop").toUpperCase()),
                sourceName,
                dropSection.getString("name", sourceName),
                dropSection.getInt("amount", 1),
                dropSection.getDouble("weight", 10.0),
                Material.valueOf(parentTransform.name().toUpperCase()),
                parentRegen,
                dropSection.getInt("slot", -1),
                Rarity.fromString(dropSection.getString("class", "comun"))
        );
    }

    public Material getSource() { return source; }
    public Material getTransformTo() { return transformTo; }
    public int getRegenTime() { return regenTime; }
    public String getName() { return name;}
    public String getDropName() { return dropName;}
    public Rarity getRarity() { return rarity; } // Getter útil para el Logbook
    public Material getDrop() { return drop; }
    public int getAmount() { return amount; }
    public double getWeight() { return weight; }
    public int getSlot() { return slot; }
}
