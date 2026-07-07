package Fortcraft.skyworld.foraging;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ForagingDrop {
    private final Material sourceMaterial;
    private final String sourceName;
    private final String sourceId;
    private final String itemId;
    private final double weight;
    private final int amount;
    private final int regenTime;

    public ForagingDrop(Material sourceMaterial, String sourceId, String sourceName, String itemId, double weight, int amount, int regenTime) {
        this.sourceMaterial = sourceMaterial;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.itemId = itemId;
        this.weight = weight;
        this.amount = amount;
        this.regenTime = regenTime;
    }

    public void giveToStorage(Player player) {
        ItemStack item = ItemRegistry.build(itemId);
        if (item == null) return;

        item.setAmount(amount);

        Rarity itemRarity = Rarity.COMUN;
        var dropTemplate = ItemRegistry.getDropTemplates().get(itemId);
        if (dropTemplate != null) {
            itemRarity = Rarity.fromString(dropTemplate.rarity());
        }

        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());

        playerData.discover(player.getUniqueId(), this.sourceId, this.sourceName);

        playerData.getStorageBag().addItemWithoutDiscovery(item, itemId, itemRarity);
    }

    public Material getSourceMaterial() { return sourceMaterial; }
    public String getName() { return sourceName; }
    public String getSourceId() { return sourceId; }
    public String itemId() { return itemId; }
    public double getWeight() { return weight; }
    public int getAmount() { return amount; }
    public int getRegenTime() { return regenTime; }
}