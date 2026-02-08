package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.mining.MiningDrop;
import Fortcraft.skyworld.mining.MiningRegenState;
import Fortcraft.skyworld.zones.MiningZone;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiningManager implements Manager {

    private final List<MiningZone> zones = new ArrayList<>();
    private final Map<Block, MiningRegenState> regeneratingBlocks = new ConcurrentHashMap<>();

    @Override
    public void load() {
        startGlobalRegenTask();
    }

    @Override
    public void unload() {
        // Restaurar bloques pendientes al cerrar el server (Opcional, pero recomendado)
        for (Map.Entry<Block, MiningRegenState> entry : regeneratingBlocks.entrySet()) {
            Block b = entry.getKey();
            MiningRegenState state = entry.getValue();
            if (state.hasHistory()) {
                // Restauramos al estado original más profundo
                while(state.hasHistory()) {
                    MiningDrop drop = state.popHistory();
                    b.setType(drop.getSource(), false);
                }
            }
        }
        regeneratingBlocks.clear();
        zones.clear();
    }

    public void registerZone(MiningZone zone) {
        zones.add(zone);
    }

    public MiningZone getZoneAt(Block block) {
        for (MiningZone zone : zones) {
            if (zone.contains(block)) return zone;
        }
        return null;
    }

    public boolean handleMine(Player p, Block block, MiningZone zone) {
        MiningDrop drop = zone.getBiome().getWeightedDrop(block.getType());

        if (drop == null) return false;

        drop.giveToStorage(p);

        MiningRegenState state = regeneratingBlocks.computeIfAbsent(block, b -> new MiningRegenState());
        state.pushHistory(drop); // Guardamos qué drop salió para logs o futuros usos

        long respawnTime = System.currentTimeMillis() + (drop.getRegenTime() * 1000L);
        state.setNextRegenTime(respawnTime);

        block.setType(drop.getTransformTo(), false);

        return true;
    }

    private void startGlobalRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (regeneratingBlocks.isEmpty()) return;

                long now = System.currentTimeMillis();
                Iterator<Map.Entry<Block, MiningRegenState>> it = regeneratingBlocks.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry<Block, MiningRegenState> entry = it.next();
                    MiningRegenState state = entry.getValue();

                    if (now >= state.getNextRegenTime()) {
                        Block block = entry.getKey();

                        MiningDrop dropToRestore = state.popHistory();

                        if (dropToRestore != null) {
                            block.setType(dropToRestore.getSource(), false);
                        }
                        if (state.hasHistory()) {
                            MiningDrop nextStep = state.peekHistory();
                            state.setNextRegenTime(now + (nextStep.getRegenTime() * 1000L));
                        } else {
                            it.remove();
                        }
                    }
                }
            }
        }.runTaskTimer(Skyworld.getInstance(), 20L, 2L);
    }
}