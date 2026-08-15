package Fortcraft.skyworld.excavation;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ExcavationDrop {
    private final Material source;
    private final String sourceId;
    private final String sourceName;
    private final String itemId;
    private final double weight;
    private final int amount;
    private final double expGiven;

    public ExcavationDrop(Material source, String sourceId, String sourceName, String itemId, double weight, int amount, double expGiven) {
        this.source = source;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.itemId = itemId;
        this.weight = weight;
        this.amount = amount;
        this.expGiven = expGiven;
    }

    /**
     * Construye y devuelve el ItemStack correspondiente a este drop con su cantidad configurada.
     */
    public ItemStack getItemStack() {
        ItemStack item = ItemRegistry.build(this.itemId);
        if (item != null) {
            item.setAmount(this.amount);
        }
        return item;
    }

    public void giveToStorage(Player player, int totalAmount) {
        ItemStack item = getItemStack();
        if (item == null) return;

        item.setAmount(totalAmount);

        Rarity itemRarity = Rarity.COMUN;
        var dropTemplate = ItemRegistry.getDropTemplates().get(itemId);
        if (dropTemplate != null) {
            itemRarity = Rarity.fromString(dropTemplate.rarity());
        }

        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());

        // Registrar en la bitácora
        playerData.discover(player.getUniqueId(), this.sourceId, this.sourceName);

        // Añadir a la infinibag
        playerData.getStorageBag().addItemWithoutDiscovery(item, itemId, itemRarity);

        // Mostrar en el chat
        playerData.queueChatDrop(itemId, totalAmount);
    }

    public String itemId() { return itemId; }
    public Material getSource() { return source; }
    public String getSourceId() { return sourceId; }
    public String getName() { return sourceName; }
    public double getWeight() { return weight; }
    public int getAmount() { return amount; }
    public double getExpGiven() { return expGiven; }
}