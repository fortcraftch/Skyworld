package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.managers.*;
import Fortcraft.skyworld.zones.FarmZone;
import Fortcraft.skyworld.zones.ForagingZone;
import Fortcraft.skyworld.zones.MiningZone;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZoneInteractionListener implements Listener {

    private final MiningManager miningManager;
    private final FarmManager farmManager;
    private final ForagingManager foragingManager;
    private final QuestManager questManager;

    private final Map<UUID, Long> interactCooldown = new HashMap<>();
    private static final long COOLDOWN_TIME = 250;

    public ZoneInteractionListener(ManagerHandler manager) {
        this.miningManager = manager.getMiningManager();
        this.farmManager = manager.getFarmManager();
        this.foragingManager = manager.getForagingManager();
        this.questManager = manager.getQuestManager();
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        interactCooldown.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGlobalBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();

        if (p.isOp() && p.getGameMode() == GameMode.CREATIVE) return;

        long now = System.currentTimeMillis();
        UUID uuid = p.getUniqueId();
        if (interactCooldown.containsKey(uuid) && (now - interactCooldown.get(uuid) < COOLDOWN_TIME)) {
            e.setCancelled(true);
            return;
        }

        Block block = e.getBlock();
        e.setCancelled(true);
        e.setDropItems(false);

        String blockTypeName = block.getType().name();

        MiningZone miningZone = miningManager.getZoneAt(block);
        if (miningZone != null) {
            boolean success = miningManager.handleMine(p, block, miningZone);
            if (!success) {
                interactCooldown.put(uuid, now);
            } else {
                // INYECCIÓN DE MISIÓN: Minar
                questManager.handleProgress(p, Fortcraft.skyworld.quests.QuestType.MINE_BLOCK, blockTypeName, 1);
            }
            return;
        }

        FarmZone farmZone = farmManager.getZoneAt(block.getLocation());
        if (farmZone != null) {
            boolean success = farmManager.handleHarvest(p, block, farmZone);
            if (!success) {
                interactCooldown.put(uuid, now);
            } else {
                // INYECCIÓN DE MISIÓN: Cosechar/Farming
                questManager.handleProgress(p, Fortcraft.skyworld.quests.QuestType.MINE_BLOCK, blockTypeName, 1);
            }
            return;
        }

        ForagingZone foragingZone = foragingManager.getZoneAt(block.getLocation());
        if (foragingZone != null) {
            boolean success = foragingManager.handleBreak(p, block, foragingZone);
            if (!success) {
                interactCooldown.put(uuid, now);
            } else {
                // INYECCIÓN DE MISIÓN: Talar/Foraging
                questManager.handleProgress(p, Fortcraft.skyworld.quests.QuestType.MINE_BLOCK, blockTypeName, 1);
            }
            return;
        }

        interactCooldown.put(uuid, now);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();

        if (e.getAction() == Action.PHYSICAL) {
            if (type == Material.FARMLAND) {
                e.setCancelled(true);
            }
        }

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = e.getItem();
            if (isLog(type) && item != null && item.getType().name().endsWith("_AXE")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(EntityInteractEvent e) {
        if (e.getBlock().getType() == Material.FARMLAND) {
            e.setCancelled(true);
        }
    }

    private boolean isLog(Material mat) {
        return mat.name().endsWith("_LOG") || mat.name().endsWith("_WOOD") || mat.name().endsWith("_STEM");
    }
}