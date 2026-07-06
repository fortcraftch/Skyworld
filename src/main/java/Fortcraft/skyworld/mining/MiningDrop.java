package Fortcraft.skyworld.mining;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MiningDrop {
    private final Material source;
    private final String sourceName;
    private final String itemId;
    private final double weight;
    private final int amount;
    private final Material transformTo;
    private final int regenTime;

    public MiningDrop(Material source, String sourceName, String itemId, double weight, int amount, Material transformTo, int regenTime) {
        this.source = source;
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

        // Forzar cantidad física en el ítem construido
        item.setAmount(this.amount);

        Rarity itemRarity = Rarity.COMUN;
        var dropTemplate = ItemRegistry.getDropTemplates().get(itemId);
        if (dropTemplate != null) {
            itemRarity = Rarity.fromString(dropTemplate.rarity());
        }

        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());

        // CORRECCIÓN: Si tu método .addItem() acepta un bucle o una cantidad explícita, úsala.
        // Si no, agregamos un bucle clásico de seguridad para que la Bag registre el total de unidades extraídas
        for (int i = 0; i < this.amount; i++) {
            playerData.getStorageBag().addItem(item, sourceName, itemRarity);
        }
    }

    public String itemId() { return itemId; }
    public Material getSource() { return source; }
    public String getName() { return sourceName; }
    public double getWeight() { return weight; }
    public int getAmount() { return amount; }
    public Material getTransformTo() { return transformTo; }
    public int getRegenTime() { return regenTime; }
}