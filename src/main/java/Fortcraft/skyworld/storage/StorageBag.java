package Fortcraft.skyworld.storage;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.PlayerMode;
import Fortcraft.skyworld.logbook.LogbookGUI;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageBag {
    private final UUID owner;
    private final Map<PlayerMode, Map<String, StorageItemData>> categorizedContents = new HashMap<>();

    public StorageBag(UUID owner) {
        this.owner = owner;
        for (PlayerMode mode : PlayerMode.values()) {
            categorizedContents.put(mode, new HashMap<>());
        }
    }

    public void addItem(ItemStack item, String Source, Rarity rarity) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String coloredName = meta.hasDisplayName()
                ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(meta.displayName())
                : item.getType().name().replace("_", " ");

        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(owner);

        playerData.discover(owner, LogbookGUI.getCleanId(Source));

        loadItem(coloredName, item.getType(), item.getAmount(), detectCategory(meta), rarity);
    }

    public void loadItem(String displayName, Material material, int amount, PlayerMode category, Rarity rarity) {
        Map<String, StorageItemData> shelf = categorizedContents.get(category);
        if (shelf.containsKey(displayName)) {
            shelf.get(displayName).addAmount(amount);
        } else {
            shelf.put(displayName, new StorageItemData(material, displayName, amount, category, rarity));
        }
    }

    public Map<String, StorageItemData> getItemsForMode(PlayerMode mode) {
        if (mode == PlayerMode.GLOBAL) {
            Map<String, StorageItemData> allItems = new HashMap<>();
            categorizedContents.values().forEach(allItems::putAll);
            return allItems;
        }
        return categorizedContents.get(mode);
    }

    private PlayerMode detectCategory(ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();
        if (pdc.has(Skyworld.ITEM_CATEGORY_KEY, org.bukkit.persistence.PersistentDataType.STRING)) {
            String tag = pdc.get(Skyworld.ITEM_CATEGORY_KEY, org.bukkit.persistence.PersistentDataType.STRING);
            try {
                return PlayerMode.valueOf(tag);
            } catch (Exception e) {
                return PlayerMode.GLOBAL;
            }
        }
        return PlayerMode.GLOBAL;
    }

    public boolean removeItem(String displayName, int amount) {
        if (!hasItem(displayName, amount)) return false;

        int remainingToRemove = amount;

        // Iteramos sobre las categorías para ir restando
        for (Map<String, StorageItemData> shelf : categorizedContents.values()) {
            if (remainingToRemove <= 0) break;

            if (shelf.containsKey(displayName)) {
                StorageItemData data = shelf.get(displayName);
                int available = data.getAmount();

                if (available > remainingToRemove) {
                    // Hay más de lo que necesitamos, solo restamos
                    data.setAmount(available - remainingToRemove);
                    remainingToRemove = 0;
                } else {
                    // Hay menos o igual, quitamos la entrada del mapa y seguimos
                    remainingToRemove -= available;
                    shelf.remove(displayName);
                }
            }
        }
        return true;
    }

    public boolean hasItem(String displayName, int amount) {
        int total = 0;
        // Buscamos en todas las categorías (Mining, Farming, etc.)
        for (Map<String, StorageItemData> shelf : categorizedContents.values()) {
            if (shelf.containsKey(displayName)) {
                total += shelf.get(displayName).getAmount();
            }
        }
        return total >= amount;
    }

    public Map<PlayerMode, Map<String, StorageItemData>> getCategorizedContents() {
        return categorizedContents;
    }

    public static class StorageItemData {
        private final Material material;
        private final String displayName;
        private final PlayerMode category;
        private final Rarity rarity; // Nuevo campo persistente
        private int amount;

        public StorageItemData(Material material, String displayName, int amount, PlayerMode category, Rarity rarity) {
            this.material = material;
            this.displayName = displayName;
            this.amount = amount;
            this.category = category;
            this.rarity = (rarity != null) ? rarity : Rarity.COMUN;
        }

        public void setAmount(int amount) { this.amount = amount; }
        public void addAmount(int extra) { this.amount += extra; }
        public Material getMaterial() { return material; }
        public String getDisplayName() { return displayName; }
        public int getAmount() { return amount; }
        public Rarity getRarity() { return rarity; }
        public PlayerMode getCategory() { return category; }
    }
}