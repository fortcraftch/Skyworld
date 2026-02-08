package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.utils.PlayerMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardManager implements Manager {

    private int taskID = -1;

    @Override
    public void load() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Skyworld.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 0L, 20L); // Cada segundo
    }

    @Override
    public void unload() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
        }
    }

    public void updateScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        if (bukkitManager == null) return;

        Scoreboard board = bukkitManager.getNewScoreboard();
        Objective obj = board.registerNewObjective("skyworld", Criteria.DUMMY, "§6§lFORTCRAFT");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        var managerHandler = Skyworld.getInstance().getManagerHandler();
        PlayerMode mode = managerHandler.getHotbarManager().getMode(player);

        double coins = 0.0;
        try {
            coins = managerHandler.getEconomyManager().getBalance(player);
        } catch (NullPointerException ignored) {}

        int i = 15;
        addScore(obj, "§7" + java.time.LocalDate.now(), i--);
        addScore(obj, "§1 ", i--); // Espacio invisible 1

        addScore(obj, "§fModo:", i--);
        addScore(obj, mode.getLegacyColor() + mode.getDisplayName(), i--); // Línea separada para nombres largos

        addScore(obj, "§2 ", i--); // Espacio invisible 2

        addScore(obj, "§fMonedas:", i--);
        addScore(obj, "§e" + String.format("%,.1f", coins) + " ⛁", i--); // Formato con comas (1,000.0)

        addScore(obj, "§3 ", i--); // Espacio invisible 3

        // Ejemplo de zona
        addScore(obj, "§fZona:", i--);
        addScore(obj, "§aGlobal", i--);

        addScore(obj, "§4 ", i--);
        addScore(obj, "§eplay.fortcraft.net", i--);

        player.setScoreboard(board);
    }

    private void addScore(Objective obj, String text, int score) {
        // Bukkit limita el tamaño de las líneas en versiones antiguas,
        // pero en 1.20+ no hay problema con textos largos.
        Score s = obj.getScore(text);
        s.setScore(score);
    }
}