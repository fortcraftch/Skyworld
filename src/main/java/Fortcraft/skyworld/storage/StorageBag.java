package Fortcraft.skyworld.storage;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.PlayerMode;
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

    public void addItem(ItemStack item, String itemId, Rarity rarity) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String cleanId = itemId.toLowerCase();
        String coloredName = meta.hasDisplayName()
                ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(meta.displayName())
                : item.getType().name().replace("_", " ");

        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(owner);

        playerData.discover(owner, cleanId, coloredName);

        loadItem(cleanId, coloredName, item.getType(), item.getAmount(), detectCategory(meta), rarity);
    }

    public void addItemWithoutDiscovery(ItemStack item, String itemId, Rarity rarity) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String cleanId = itemId.toLowerCase();
        String coloredName = meta.hasDisplayName()
                ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(meta.displayName())
                : item.getType().name().replace("_", " ");

        loadItem(cleanId, coloredName, item.getType(), item.getAmount(), detectCategory(meta), rarity);
    }


    public void loadItem(String itemId, String displayName, Material material, int amount, PlayerMode category, Rarity rarity) {
        Map<String, StorageItemData> shelf = categorizedContents.get(category);
        if (shelf.containsKey(itemId)) {
            shelf.get(itemId).addAmount(amount);
        } else {
            shelf.put(itemId, new StorageItemData(itemId, material, displayName, amount, category, rarity));
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
            try { return PlayerMode.valueOf(tag.toUpperCase()); } catch (Exception ignored) {}
        }
        return PlayerMode.GLOBAL;
    }

    public boolean removeItem(String itemId, int amount) {
        String cleanId = itemId.toLowerCase();
        if (!hasItem(cleanId, amount)) return false;

        int remainingToRemove = amount;
        for (Map<String, StorageItemData> shelf : categorizedContents.values()) {
            if (remainingToRemove <= 0) break;

            if (shelf.containsKey(cleanId)) {
                StorageItemData data = shelf.get(cleanId);
                int available = data.getAmount();

                if (available > remainingToRemove) {
                    data.setAmount(available - remainingToRemove);
                    remainingToRemove = 0;
                } else {
                    remainingToRemove -= available;
                    shelf.remove(cleanId);
                }
            }
        }
        return true;
    }

    public boolean hasItem(String itemId, int amount) {
        String cleanId = itemId.toLowerCase();
        int total = 0;
        for (Map<String, StorageItemData> shelf : categorizedContents.values()) {
            if (shelf.containsKey(cleanId)) {
                total += shelf.get(cleanId).getAmount();
            }
        }
        return total >= amount;
    }

    public Map<PlayerMode, Map<String, StorageItemData>> getCategorizedContents() {
        return categorizedContents;
    }

    public static class StorageItemData {
        private final String itemId;
        private final Material material;
        private final String displayName;
        private final PlayerMode category;
        private final Rarity rarity;
        private int amount;

        public StorageItemData(String itemId, Material material, String displayName, int amount, PlayerMode category, Rarity rarity) {
            this.itemId = itemId.toLowerCase();
            this.material = material;
            this.displayName = displayName;
            this.amount = amount;
            this.category = category;
            this.rarity = (rarity != null) ? rarity : Rarity.COMUN;
        }

        public void setAmount(int amount) { this.amount = amount; }
        public void addAmount(int extra) { this.amount += extra; }
        public String getItemId() { return itemId; }
        public Material getMaterial() { return material; }
        public String getDisplayName() { return displayName; }
        public int getAmount() { return amount; }
        public Rarity getRarity() { return rarity; }
        public PlayerMode getCategory() { return category; }
    }
}