package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.data.PlayerData;
import Fortcraft.skyworld.utils.HotbarSlot;
import Fortcraft.skyworld.utils.PlayerMode;
import Fortcraft.skyworld.listeners.GUIListener;
import Fortcraft.skyworld.storage.StorageBag;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager implements Manager {

    private final Map<UUID, PlayerData> loadedData = new HashMap<>();
    private File dataFolder;

    @Override
    public void load() {
        dataFolder = new File(Skyworld.getInstance().getDataFolder(), "userdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();

        Bukkit.getPluginManager().registerEvents(new GUIListener(), Skyworld.getInstance());
    }

    @Override
    public void unload() {
        saveAll();
        loadedData.clear();
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (loadedData.containsKey(uuid)) return loadedData.get(uuid);

        PlayerData data = loadFromFile(uuid);
        loadedData.put(uuid, data);
        return data;
    }

    private PlayerData loadFromFile(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        PlayerData data = new PlayerData(uuid);

        if (!file.exists()) {
            applyDefaultKits(data);
            return data;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // --- ECONOMÍA & STATS ---
        data.setCoins(config.getDouble("economy.coins", 0.0));
        data.setStat("combat_level", config.getDouble("stats.combat_level", 1.0));
        data.setStat("mining_level", config.getDouble("stats.mining_level", 1.0));

        // --- DESCUBRIMIENTOS ---
        List<String> discovered = config.getStringList("discovered");
        discovered.forEach(data::discover);

        // --- LOADOUTS ---
        ConfigurationSection loadoutSec = config.getConfigurationSection("loadouts");
        if (loadoutSec != null) {
            for (String modeKey : loadoutSec.getKeys(false)) {
                try {
                    PlayerMode mode = PlayerMode.valueOf(modeKey);
                    ConfigurationSection slots = loadoutSec.getConfigurationSection(modeKey);
                    if (slots != null) {
                        for (String slotIndexStr : slots.getKeys(false)) {
                            int slotIndex = Integer.parseInt(slotIndexStr);
                            String itemId = slots.getString(slotIndexStr);
                            data.setLoadoutItem(mode, HotbarSlot.fromIndex(slotIndex), itemId);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // --- ALMACÉN (STORAGE) ---
        ConfigurationSection storageSec = config.getConfigurationSection("storage");
        if (storageSec != null) {
            for (String modeKey : storageSec.getKeys(false)) {
                try {
                    PlayerMode mode = PlayerMode.valueOf(modeKey);
                    ConfigurationSection itemsInMode = storageSec.getConfigurationSection(modeKey);

                    if (itemsInMode != null) {
                        for (String itemId : itemsInMode.getKeys(false)) {
                            String matName = itemsInMode.getString(itemId + ".material");
                            String displayName = itemsInMode.getString(itemId + ".display-name", itemId);
                            int amount = itemsInMode.getInt(itemId + ".amount");
                            String rarityName = itemsInMode.getString(itemId + ".rarity", "COMUN");

                            if (matName != null) {
                                data.getStorageBag().loadItem(
                                        itemId,
                                        displayName,
                                        Material.valueOf(matName),
                                        amount,
                                        mode,
                                        Rarity.fromString(rarityName)
                                );
                            }
                        }
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // --- MISIONES ---
        data.loadQuestsProgress(config);

        return data;
    }

    private void applyDefaultKits(PlayerData data) {
        if (data.getLoadoutForMode(PlayerMode.GLOBAL).isEmpty()) {
            data.setLoadoutItem(PlayerMode.GLOBAL, HotbarSlot.PRIMARY, "novice_sword");
            data.setLoadoutItem(PlayerMode.GLOBAL, HotbarSlot.CONSUMABLE_1, "apple");
        }
        if (data.getLoadoutForMode(PlayerMode.MINING).isEmpty()) {
            data.setLoadoutItem(PlayerMode.MINING, HotbarSlot.SUPPORT, "novice_pickaxe");
        }
    }

    public void save(PlayerData data) {
        File file = new File(dataFolder, data.getUuid() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("economy.coins", data.getCoins());
        config.set("stats.combat_level", data.getStat("combat_level"));
        config.set("stats.mining_level", data.getStat("mining_level"));
        config.set("discovered", new ArrayList<>(data.getDiscoveredItems()));

        for (PlayerMode mode : PlayerMode.values()) {
            Map<Integer, String> loadout = data.getLoadoutForMode(mode);
            if (loadout == null || loadout.isEmpty()) continue;

            loadout.forEach((slotIndex, itemId) -> {
                config.set("loadouts." + mode.name() + "." + slotIndex, itemId);
            });
        }

        // Se guarda ahora directo con el itemId
        StorageBag bag = data.getStorageBag();
        bag.getCategorizedContents().forEach((mode, items) -> {
            if (items.isEmpty()) return;

            items.forEach((itemId, itemData) -> {
                String path = "storage." + mode.name() + "." + itemId;
                config.set(path + ".material", itemData.getMaterial().name());
                config.set(path + ".display-name", itemData.getDisplayName());
                config.set(path + ".amount", itemData.getAmount());
                config.set(path + ".rarity", itemData.getRarity().name());
            });
        });

        data.saveQuestsProgress(config);

        try {
            config.save(file);
        } catch (IOException e) {
            Skyworld.getInstance().getLogger().severe("No se pudo guardar la data de: " + data.getUuid());
        }
    }

    public void saveAll() {
        if (loadedData.isEmpty()) return;
        Skyworld.getInstance().getLogger().info("Guardando datos de " + loadedData.size() + " jugadores...");
        loadedData.values().forEach(this::save);
    }

    public void removePlayerData(UUID uuid) {
        if (loadedData.containsKey(uuid)) {
            save(loadedData.get(uuid));
            loadedData.remove(uuid);
        }
    }
}