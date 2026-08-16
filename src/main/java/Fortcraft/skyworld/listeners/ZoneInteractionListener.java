package Fortcraft.skyworld.listeners;

import Fortcraft.skyworld.managers.*;
import Fortcraft.skyworld.utils.CooldownManager;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ZoneInteractionListener implements Listener {

    private final MiningManager miningManager;
    private final FarmManager farmManager;
    private final ForagingManager foragingManager;
    private final QuestManager questManager;

    public ZoneInteractionListener(ManagerHandler manager) {
        this.miningManager = manager.getMiningManager();
        this.farmManager = manager.getFarmManager();
        this.foragingManager = manager.getForagingManager();
        this.questManager = manager.getQuestManager();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Usamos el gestor centralizado
        CooldownManager.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGlobalBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();

        if (p.isOp() && p.getGameMode() == GameMode.CREATIVE) return;

        Block block = e.getBlock();
        String blockTypeName = block.getType().name();
        UUID uuid = p.getUniqueId();

        // 1. CHEQUEO GLOBAL DE COOLDOWN
        if (CooldownManager.isOnCooldown(uuid)) {
            e.setCancelled(true);
            return;
        }

        // 2. ZONA DE MINERÍA
        MiningZone miningZone = miningManager.getZoneAt(block);
        if (miningZone != null) {
            e.setCancelled(true);
            // Si intenta romper bloque de minería sin esperar, penalizamos con cooldown
            CooldownManager.setCooldown(uuid);
            return;
        }

        e.setCancelled(true);
        e.setDropItems(false);

        // 3. ZONA DE FARMING
        FarmZone farmZone = farmManager.getZoneAt(block.getLocation());
        if (farmZone != null) {
            boolean success = farmManager.handleHarvest(p, block, farmZone);
            if (!success) {
                CooldownManager.setCooldown(uuid);
            } else {
                questManager.handleProgress(p, Fortcraft.skyworld.quests.QuestType.MINE_BLOCK, blockTypeName, 1);
            }
            return;
        }

        // 4. ZONA DE FORAGING
        ForagingZone foragingZone = foragingManager.getZoneAt(block.getLocation());
        if (foragingZone != null) {
            boolean success = foragingManager.handleBreak(p, block, foragingZone);
            if (!success) {
                CooldownManager.setCooldown(uuid);
            } else {
                questManager.handleProgress(p, Fortcraft.skyworld.quests.QuestType.MINE_BLOCK, blockTypeName, 1);
            }
            return;
        }

        // Fallback: Si no encaja en ninguna zona, aplicamos cooldown para evitar spam
        CooldownManager.setCooldown(uuid);
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