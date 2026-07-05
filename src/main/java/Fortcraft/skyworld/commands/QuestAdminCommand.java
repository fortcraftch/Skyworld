package Fortcraft.skyworld.commands;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.QuestManager;
import Fortcraft.skyworld.quests.Quest;
import Fortcraft.skyworld.quests.PlayerQuestProgress;
import Fortcraft.skyworld.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QuestAdminCommand implements CommandExecutor {

    private final QuestManager questManager;

    public QuestAdminCommand(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Validación de permisos utilizando tu esquema de administración
        if (!sender.hasPermission("skyworld.admin.quests")) {
            sender.sendMessage(ColorUtils.format("&cNo tienes permisos para usar los comandos de misiones."));
            return true;
        }

        // Validación de argumentos iniciales
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.format("&c¡Uso correcto! /questadmin <iniciar|completar|reset> <id_mision> [jugador]"));
            return true;
        }

        String accion = args[0].toLowerCase();
        String questId = args[1].toLowerCase();

        // Verificar si la misión existe en la configuración global
        Quest quest = questManager.getQuest(questId);
        if (quest == null) {
            sender.sendMessage(ColorUtils.format("&cLa misión '" + questId + "' no existe en el archivo quests.yml."));
            return true;
        }

        // Determinar el jugador objetivo (si se ejecuta por consola o se especifica un tercero)
        Player targetPlayer;
        if (args.length >= 3) {
            targetPlayer = org.bukkit.Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                sender.sendMessage(ColorUtils.format("&cEl jugador '" + args[2] + "' no está conectado."));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ColorUtils.format("&cDebes especificar un jugador si ejecutas este comando desde la consola."));
                return true;
            }
            targetPlayer = (Player) sender;
        }

        // --- SUBCOMANDOS ---
        switch (accion) {
            case "iniciar" -> {
                // Vincula la misión al jugador y arranca automáticamente los efectos y HUD
                questManager.setTrackingQuest(targetPlayer, questId);
                sender.sendMessage(ColorUtils.format("&a&l[Misiones] &fMisión '&e" + quest.getTitle() + "&f' vinculada y activa para &b" + targetPlayer.getName() + "&f."));
            }
            case "completar" -> {
                var progressMap = questManager.getPlayerQuests(targetPlayer.getUniqueId());
                PlayerQuestProgress progress = progressMap.get(questId);

                if (progress != null) {
                    progress.setCompleted(true);
                    // Si estaba siguiendo visualmente esta misión, limpiamos los objetivos activos
                    if (questId.equalsIgnoreCase(questManager.getTrackedQuestId(targetPlayer.getUniqueId()))) {
                        Skyworld.getInstance().getManagerHandler().getNavigationManager().stopGuiding(targetPlayer);
                    }
                    sender.sendMessage(ColorUtils.format("&a&l[Misiones] &fMisión '&e" + quest.getTitle() + "&f' forzada como completada para &b" + targetPlayer.getName() + "&f."));
                    targetPlayer.sendMessage(ColorUtils.format("&a&l[Misiones] &f¡La misión '&e" + quest.getTitle() + "&f' ha sido completada externamente!"));
                } else {
                    sender.sendMessage(ColorUtils.format("&cEl jugador no tiene progreso registrado en esta misión. Iníciala primero."));
                }
            }
            case "reset" -> {
                var progressMap = questManager.getPlayerQuests(targetPlayer.getUniqueId());
                if (progressMap.containsKey(questId)) {
                    progressMap.remove(questId);
                    if (questId.equalsIgnoreCase(questManager.getTrackedQuestId(targetPlayer.getUniqueId()))) {
                        questManager.setTrackingQuest(targetPlayer, null);
                        Skyworld.getInstance().getManagerHandler().getNavigationManager().stopGuiding(targetPlayer);
                    }
                    sender.sendMessage(ColorUtils.format("&e&l[Misiones] &fProgreso borrado por completo para &b" + targetPlayer.getName() + "&f."));
                } else {
                    sender.sendMessage(ColorUtils.format("&cEl jugador no tenía progreso guardado en esta misión."));
                }
            }
            default -> sender.sendMessage(ColorUtils.format("&cAcción desconocida. Modos válidos: iniciar, completar, reset"));
        }

        return true;
    }
}