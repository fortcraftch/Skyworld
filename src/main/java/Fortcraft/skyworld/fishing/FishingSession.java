package Fortcraft.skyworld.fishing;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.zones.FishingZone;
import org.bukkit.entity.Player;

import static Fortcraft.skyworld.logbook.LogbookGUI.getCleanId;

public class FishingSession {

    private final Player player;
    private final FishingZone zone;
    private boolean active = true;
    private FishingMinigame minigame;
    private final int rarity;

    public FishingSession(Player player, FishingZone zone, int rarity) {
        if (zone == null) {
            throw new IllegalArgumentException("FishingSession created with null zone");
        }
        this.player = player;
        this.zone = zone;
        this.rarity = rarity; // <- Este rarity ahora es el sizerarity inyectado desde FishingBiome
    }

    public void startMinigame(int rarity) {
        minigame = new FishingMinigame(this, rarity);
        minigame.start();
    }

    public void onClick() {
        if (minigame != null && minigame.isAccepted() == false) {
            minigame.accept();
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

                String variantId = getCleanId(drop.getItemId());
                boolean isNewSize = !playerData.hasDiscovered(variantId);

                // Esto aplicará el (S) al ItemStack y lo guardará
                drop.giveToStorage(player);

                var template = ItemRegistry.getDropTemplates().get(drop.getItemId());
                if (template != null && template.customStats() != null) {
                    double expGiven = template.customStats().getOrDefault("exp_given", 0.0);
                    if (expGiven > 0) {
                        player.giveExp((int) expGiven);
                    }
                }

                String speciesColor = drop.getSpeciesRarity().getColorCode();

                // Construcción limpia con MiniMessage
                String msg = "<gray>[<green>+</green>] " + speciesColor + drop.getName();

                // Añadimos visualmente el (S) al chat solo si el pez tiene un tamaño definido
                if (drop.getSize() != null && !drop.getSize().isEmpty()) {
                    msg += " <gray>(" + speciesColor + drop.getSize() + "<gray>)";
                }

                if (isNewSize) {
                    msg += " <green>(Nuevo Pesaje)</green>";
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 2f);
                }

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