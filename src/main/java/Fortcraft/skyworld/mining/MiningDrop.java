package Fortcraft.skyworld.mining;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MiningDrop {
    private final Material source;
    private final String sourceId; // NUEVO
    private final String sourceName;
    private final String itemId;
    private final double weight;
    private final int amount;
    private final Material transformTo;
    private final int regenTime;

    public MiningDrop(Material source, String sourceId, String sourceName, String itemId, double weight, int amount, Material transformTo, int regenTime) {
        this.source = source;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.itemId = itemId;
        this.weight = weight;
        this.amount = amount;
        this.transformTo = transformTo;
        this.regenTime = regenTime;
    }

    public void giveToStorage(Player player) {
        ItemStack item = ItemRegistry.build(itemId);
        if (item == null) return;

        item.setAmount(this.amount);

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

    public String itemId() { return itemId; }
    public Material getSource() { return source; }
    public String getSourceId() { return sourceId; }
    public String getName() { return sourceName; }
    public double getWeight() { return weight; }
    public int getAmount() { return amount; }
    public Material getTransformTo() { return transformTo; }
    public int getRegenTime() { return regenTime; }
}