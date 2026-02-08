package Fortcraft.skyworld.fishing;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.zones.FishingZone;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

import static Fortcraft.skyworld.logbook.LogbookGUI.getCleanId;

public class FishingSession {

    private final Player player;
    private final FishingZone zone; // ← nunca debe ser null
    private boolean active = true;
    private FishingMinigame minigame;
    private final int rarity;

    public FishingSession(Player player, FishingZone zone, int rarity) {
        if (zone == null) {
            throw new IllegalArgumentException("FishingSession created with null zone");
        }
        this.player = player;
        this.zone = zone;
        this.rarity = rarity;
    }

    public void startMinigame(int rarity) {
        minigame = new FishingMinigame(this, rarity);
        minigame.start();
    }

    public void onClick() {
        if (minigame != null && minigame.isAccepted() == false) {
            minigame.accept(); // acepta el minijuego
        }
    }

    public void success() {
        finish(true);
    }

    public void fail() {
        finish(false);
    }

    private void finish(boolean success) {
        active = false;

        if (success) {
            FishingDrop drop = zone.rollDrop(rarity);
            if (drop != null) {
                var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
                var playerData = dataManager.getPlayerData(player.getUniqueId());

                String variantId = getCleanId(drop.getId());
                boolean isNewSize = !playerData.hasDiscovered(variantId);

                drop.giveToStorage(player);

                // Usamos etiquetas de MiniMessage para el mensaje de chat
                String speciesColor = drop.getSpeciesRarity().getColorCode();
                String variantColor = drop.getVariantRarity().getColorCode();

                // Construcción limpia con MiniMessage
                String msg = "<gray>[<green>+</green>] " + speciesColor  + drop.getName();

                if (!drop.getSizeName().isEmpty()) {
                    msg += " " + variantColor + drop.getSizeName();
                    if (isNewSize) {
                        msg += " <green>(Nuevo Pesaje)</green>";
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 2f);
                    }
                }

                // Enviamos el mensaje procesado por ColorUtils
                player.sendMessage(ColorUtils.format(msg));
                zone.processCatch();
            }
        } else {
            player.sendMessage(ColorUtils.format("<red>El pez escapó..."));
        }

        Skyworld.getInstance().getManagerHandler().getFishingManager().handleReel(player);
    }

    public boolean isActive() {
        return active;
    }

    public Player getPlayer() {
        return player;
    }

    public FishingZone getZone() {
        return zone;
    }
}

