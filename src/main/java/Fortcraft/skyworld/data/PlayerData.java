package Fortcraft.skyworld.data;

import Fortcraft.skyworld.storage.StorageBag;
import Fortcraft.skyworld.utils.HotbarSlot;
import Fortcraft.skyworld.utils.PlayerMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public class PlayerData {
    private final UUID uuid;
    private final StorageBag storageBag;

    // --- ECONOMÍA ---
    private double coins = 0.0; // Variable para almacenar el dinero

    // --- PROGRESO ---
    private final Set<String> discoveredItems = new HashSet<>();
    private final Map<PlayerMode, Map<Integer, String>> loadouts = new HashMap<>();

    // --- ESTADÍSTICAS (Niveles, Mana, etc) ---
    private final Map<String, Double> stats = new HashMap<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.storageBag = new StorageBag(uuid);
        for (PlayerMode mode : PlayerMode.values()) {
            loadouts.put(mode, new HashMap<>());
        }

        // Inicializar stats básicos si es necesario
        stats.put("combat_level", 1.0);
        stats.put("mining_level", 1.0);
    }

    public UUID getUuid() { return uuid; }
    public StorageBag getStorageBag() { return storageBag; }

    // --- MÉTODOS DE ECONOMÍA ---
    public double getCoins() {
        return coins;
    }

    public void setCoins(double coins) {
        this.coins = coins;
    }

    public void addCoins(double amount) {
        this.coins += amount;
    }

    public boolean removeCoins(double amount) {
        if (this.coins >= amount) {
            this.coins -= amount;
            return true;
        }
        return false;
    }

    // --- MÉTODOS DE ESTADÍSTICAS ---
    public double getStat(String key) {
        return stats.getOrDefault(key, 0.0);
    }

    public void setStat(String key, double value) {
        stats.put(key, value);
    }

    // --- MÉTODOS DE LOADOUT (Tu código original) ---
    public void setLoadoutItem(PlayerMode mode, HotbarSlot slot, String itemId) {
        loadouts.get(mode).put(slot.getSlotIndex(), itemId);
    }

    public String getLoadoutItem(PlayerMode mode, int slotIndex) {
        return loadouts.get(mode).get(slotIndex);
    }

    public Map<Integer, String> getLoadoutForMode(PlayerMode mode) {
        return loadouts.get(mode);
    }

    // --- MÉTODOS DE DESCUBRIMIENTO (Tu código original + optimización) ---
    public void discover(String itemId) {
        discoveredItems.add(itemId);
    }

    public void discover(UUID uuid, String itemId) {
        if (!discoveredItems.contains(itemId)) {
            discoveredItems.add(itemId);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                sendDiscoveryNotification(player, itemId);
            }
        }
    }

    private void sendDiscoveryNotification(Player player, String itemName) {
        Component mainTitle = LegacyComponentSerializer.legacySection().deserialize("§6§l¡NUEVA ENTRADA!");
        Component subTitle = LegacyComponentSerializer.legacySection().deserialize("§fHas descubierto: " + itemName);

        Title title = Title.title(
                mainTitle, subTitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
        );

        player.showTitle(title);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.4f, 1.2f);
    }

    public boolean hasDiscovered(String itemId) { return discoveredItems.contains(itemId); }
    public Set<String> getDiscoveredItems() { return discoveredItems; }
}