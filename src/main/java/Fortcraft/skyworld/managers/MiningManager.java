package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
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
        for (Map.Entry<Block, MiningRegenState> entry : regeneratingBlocks.entrySet()) {
            Block b = entry.getKey();
            MiningRegenState state = entry.getValue();
            if (state.hasHistory()) {
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
        // 1. Obtenemos las estadísticas del caché del jugador
        double luck = StatManager.getStat(p, "mining_luck");
        double fortune = StatManager.getStat(p, "mining_fortune");
        double wisdom = StatManager.getStat(p, "wisdom");

        // 2. Obtenemos el drop aplicando la Suerte de Minería
        MiningDrop drop = zone.getBiome().getWeightedDrop(block.getType(), luck);
        if (drop == null) return false;

        var template = ItemRegistry.getDropTemplates().get(drop.itemId());

        // 3. Calculamos la cantidad final de drops aplicando la Fortuna de Minería
        int baseAmount = drop.getAmount();
        int finalAmount = StatManager.calculateFortuneDrops(baseAmount, fortune);
        drop.giveToStorage(p, finalAmount);

        // 4. Calculamos la experiencia aplicando la Sabiduría (Wisdom)
        if (template != null && template.stats() != null) {
            double baseExp = template.stats().getOrDefault("exp_given", 0.0);
            if (baseExp > 0) {
                double multiplier = 1.0 + (wisdom / 100.0);
                double finalExp = baseExp * multiplier;

                Skyworld.getInstance().getManagerHandler().getSkillManager().giveXp(p, "mining", finalExp);
            }
        }

        MiningRegenState state = regeneratingBlocks.computeIfAbsent(block, b -> new MiningRegenState());
        state.pushHistory(drop);

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