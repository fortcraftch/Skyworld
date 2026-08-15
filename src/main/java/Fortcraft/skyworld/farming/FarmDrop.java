package Fortcraft.skyworld.farming;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FarmDrop {
    private final Material sourceBlock;
    private final String sourceName;
    private final String sourceId; // NUEVO
    private final String itemId;
    private final double weight;
    private final int amount;
    private final int regenTime;

    public FarmDrop(Material sourceBlock, String sourceId, String sourceName, String itemId, double weight, int amount, int regenTime) {
        this.sourceBlock = sourceBlock;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.itemId = itemId;
        this.weight = weight;
        this.amount = amount;
        this.regenTime = regenTime;
    }

    public void giveToStorage(Player player, int totalAmount) {
        ItemStack item = ItemRegistry.build(itemId);
        if (item == null) return;

        item.setAmount(totalAmount);

        Rarity itemRarity = Rarity.COMUN;
        var dropTemplate = ItemRegistry.getDropTemplates().get(itemId);
        if (dropTemplate != null) {
            itemRarity = Rarity.fromString(dropTemplate.rarity());
        }

        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());

        playerData.discover(player.getUniqueId(), this.sourceId, this.sourceName);
        playerData.getStorageBag().addItemWithoutDiscovery(item, itemId, itemRarity);

        playerData.queueChatDrop(itemId, totalAmount);
    }

    public Material getSourceBlock() { return sourceBlock; }
    public String getName() { return sourceName; }
    public String getSourceId() { return sourceId; }
    public String itemId() { return itemId; }
    public double getWeight() { return weight; }
    public int getAmount() { return amount; }
    public int getRegenTime() { return regenTime; }
}