package Fortcraft.skyworld.fishing;

import Fortcraft.skyworld.Skyworld;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class FishingMinigame {

    private final FishingSession session;
    private final Player player;

    private int progress = 0;
    private final int totalProgress;

    private boolean accepted = false;

    public FishingMinigame(FishingSession session, int rarity) {
        this.session = session;
        this.player = session.getPlayer();
        this.totalProgress = rarity; // por ejemplo: 1 = 2 ticks, 2 = 4 ticks
    }

    public void start() {
        player.sendTitle("§aA fish bit!", "§7Click to reel!", 5, 30, 5);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 1f);

        // Esperar 1.5 segundos para que el jugador recoja la caña
        Bukkit.getScheduler().runTaskLater(Skyworld.getInstance(), () -> {
            if (!accepted) {
                session.fail();
            }
        }, 30L); // 30 ticks = 1.5 segundos
    }

    public void accept() {
        if (accepted) return;
        accepted = true;
        startProgressBar();
    }

    public boolean isAccepted() {
        return accepted;
    }

    private void startProgressBar() {
        Bukkit.getScheduler().runTaskTimer(Skyworld.getInstance(), task -> {

            progress++;

            if (progress > totalProgress) {
                task.cancel();
                session.success();
                return;
            }
            sendBar();

        }, 0L, 10L); // 0.5 segundos entre incrementos
    }

    private String getProgressColor(int step) {
        int tier = (step - 1) / 2;

        return switch (tier) {
            case 0 -> "§f"; // comun
            case 1 -> "§a"; // poco comun
            case 2 -> "§b"; // raro
            case 3 -> "§5"; // epico
            case 4 -> "§6"; // legendario
            case 5 -> "§c"; // exotico
            default -> "§f"; // default
        };
    }

    private void sendBar() {

        StringBuilder bar = new StringBuilder("§7[");

        for (int i = 1; i <= progress; i++) {
            bar.append(getProgressColor(i)).append("-");
        }

        bar.append("§7]");

        player.sendTitle(
                bar.toString(),     // TÍTULO → barra incremental multicolor
                "§7Reeling...",
                0,
                10,
                5
        );

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                0.4f,
                1f + (progress * 0.1f)
        );
    }
}
