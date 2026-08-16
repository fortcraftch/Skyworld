package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.data.PlayerData;
import Fortcraft.skyworld.managers.ManagerHandler;
import Fortcraft.skyworld.managers.MiningManager;
import Fortcraft.skyworld.managers.QuestManager;
import Fortcraft.skyworld.managers.StatManager;
import Fortcraft.skyworld.mining.MiningDrop;
import Fortcraft.skyworld.quests.QuestType;
import Fortcraft.skyworld.utils.CooldownManager;
import Fortcraft.skyworld.zones.MiningZone;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MiningListener implements Listener {

    private final MiningManager miningManager;
    private final QuestManager questManager;
    private final Map<UUID, BukkitTask> activeMiningTasks = new ConcurrentHashMap<>();

    public MiningListener(ManagerHandler managerHandler) {
        this.miningManager = managerHandler.getMiningManager();
        this.questManager = managerHandler.getQuestManager();
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        UUID uuid = player.getUniqueId();

        if (CooldownManager.isOnCooldown(uuid)) {
            return;
        }

        MiningZone zone = miningManager.getZoneAt(block);
        if (zone == null) return; // Solo actuamos en bloques de zona de minería

        // 1. Obtener los atributos del bloque desde su bioma/drop
        MiningDrop refDrop = zone.getBiome().getFirstDropFor(block.getType());
        if (refDrop == null) {
            return;
        }

        double requiredPower = refDrop.getRequiredPower();
        double blockHardness = refDrop.getHardness();

        // 2. Obtener las estadísticas del jugador directamente desde StatManager (¡Ultra rápido y en caché!)
        double playerPower = StatManager.getStat(player, "breaking_power");
        double playerSpeed = StatManager.getStat(player, "mining_speed");

        // 3. REQUISITO DE PODER: Comprobar si el jugador cumple con el poder necesario
        if (playerPower < requiredPower) {
            PlayerData data = Skyworld.getInstance().getManagerHandler().getDataManager().getPlayerData(uuid);
            if (data != null) {
                data.sendNotice("&c¡Necesitas un pico con Poder de Minería &e" + requiredPower + "&c!", 1);
            }

            CooldownManager.setCooldown(uuid);
            return;
        }

        cancelMining(player, block); // Limpiar tareas previas por si acaso

        // 4. PROGRESO DE MINADO
        double progressPerTick = playerSpeed / blockHardness;

        BukkitTask task = new BukkitRunnable() {
            float progress = 0.0f;

            @Override
            public void run() {
                progress += (float) progressPerTick;
                player.sendBlockDamage(block.getLocation(), progress);

                if (progress >= 1.0f) {
                    boolean success = miningManager.handleMine(player, block, zone);

                    if (success) {
                        questManager.handleProgress(player, QuestType.MINE_BLOCK, block.getType().name(), 1);
                    }

                    player.sendBlockDamage(block.getLocation(), 0.0f);
                    activeMiningTasks.remove(uuid);
                    this.cancel();
                }
            }
        }.runTaskTimer(Skyworld.getInstance(), 0L, 1L);

        activeMiningTasks.put(uuid, task);
    }

    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        cancelMining(event.getPlayer(), event.getBlock());
    }

    private void cancelMining(Player player, Block block) {
        BukkitTask task = activeMiningTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            player.sendBlockDamage(block.getLocation(), 0.0f);
        }
    }
}