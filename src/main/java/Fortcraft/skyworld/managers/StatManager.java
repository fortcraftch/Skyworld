package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.data.PlayerData;
import Fortcraft.skyworld.listeners.StatUpdateListener;
import Fortcraft.skyworld.stats.CustomStat;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

public class StatManager implements Manager {

    private static DataManager dataManager;

    public StatManager(DataManager dataManager) {
        StatManager.dataManager = dataManager;
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(
                new StatUpdateListener(),
                Skyworld.getInstance()
        );
    }

    @Override
    public void unload() {

    }

    /**
     * Lee el stat directamente de la memoria (PlayerData).
     * Ultra rápido, 0 lag.
     */
    public static double getStat(Player player, String statName) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null) return 0.0;

        return data.getStat(statName);
    }

    /**
     * Recalcula los stats leyendo los PDC y los guarda en el PlayerData.
     */
    public static void updateStats(Player player) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        Map<String, Double> newStats = new HashMap<>();

        // 1. Leer stat del ítem en la mano principal
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.hasItemMeta()) {
            ItemMeta meta = mainHand.getItemMeta();

            for (CustomStat stat : CustomStat.values()) {
                NamespacedKey key = new NamespacedKey("skyworld", stat.getKey());
                if (meta.getPersistentDataContainer().has(key, PersistentDataType.DOUBLE)) {
                    double value = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.DOUBLE, 0.0);
                    newStats.put(stat.getKey(), newStats.getOrDefault(stat.getKey(), 0.0) + value);
                }
            }
        }

        // --- ESPACIO PARA FUTURAS EXPANSIÓNES ---
        // 2. Sumar stats de armadura
        // 3. Sumar stats pasivos (talismanes, nivel de skill)

        // EJEMPLO: Si el nivel de Farming da 2 de Fortune por nivel:
        // int farmingLevel = data.getSkillLevel("farming");
        // newStats.put("farming_fortune", newStats.getOrDefault("farming_fortune", 0.0) + (farmingLevel * 2));

        // Guardamos el nuevo cálculo en el PlayerData
        data.updateCachedStats(newStats);
    }

    /**
     * Calcula la cantidad final de drops usando la estadística de Fortune.
     */
    public static int calculateFortuneDrops(int baseAmount, double fortune) {
        if (fortune <= 0) return baseAmount;

        int extraDrops = (int) (fortune / 100);
        double leftoverChance = fortune % 100;

        if (Math.random() * 100 < leftoverChance) {
            extraDrops++;
        }

        return baseAmount + extraDrops;
    }
}