package Fortcraft.skyworld.menu;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.managers.QuestManager;
import Fortcraft.skyworld.quests.Quest;
import Fortcraft.skyworld.quests.PlayerQuestProgress;
import Fortcraft.skyworld.quests.QuestStage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestMenu {

    public static void open(Player player) {
        QuestManager questManager = Skyworld.getInstance().getManagerHandler().getQuestManager();

        // Creamos un menú con la base de tu clase SkyblockMenu (Tamaño: 27 slots)
        SkyblockMenu menu = new SkyblockMenu("quest_menu", "&9Mis Misiones Activas", 27);

        Map<String, PlayerQuestProgress> playerQuests = questManager.getPlayerQuests(player.getUniqueId());
        String trackedQuestId = questManager.getTrackedQuestId(player.getUniqueId());

        int slot = 10; // Empezamos a rellenar en la fila central

        for (PlayerQuestProgress progress : playerQuests.values()) {
            if (slot > 16) break; // Evitamos desbordar la fila central del inventario

            Quest quest = questManager.getQuest(progress.getQuestId());
            if (quest == null) continue;

            boolean isTracked = quest.getId().equalsIgnoreCase(trackedQuestId);
            Material iconMaterial = progress.isCompleted() ? Material.WRITTEN_BOOK : Material.BOOK;
            String name = (isTracked ? "&a&l▶ " : "&e") + quest.getTitle();

            // Cálculo de fases (X/Y)
            int totalStages = quest.getStages() != null ? quest.getStages().size() : 1;
            int currentStageNum = progress.isCompleted() ? totalStages : (progress.getCurrentStageIndex() + 1);

            List<String> lore = new ArrayList<>();
            lore.add("&7-------------------");

            // 1. Mostrar la fase actual sobre el total (Ej: Fase: 2/3)
            lore.add("&7Fase: &e" + currentStageNum + "&7/&e" + totalStages);
            lore.add("");

            if (progress.isCompleted()) {
                lore.add("&a✔ Completada.");
            } else {
                QuestStage stage = quest.getStages().get(progress.getCurrentStageIndex());
                lore.add("&7Objetivo actual:");
                lore.add("&f" + stage.getDescription());

                if (stage.getRequiredAmount() > 1) {
                    lore.add("&7Progreso: &e" + progress.getCurrentProgressAmount() + "&7/&e" + stage.getRequiredAmount());
                }
            }

            // 2. Sección de Recompensas
            lore.add("");
            lore.add("&7Recompensas:");
            boolean hasRewards = false;

            // EXP
            if (quest.getRewardExp() > 0) {
                String skillText = quest.getSkill() != null && !quest.getSkill().isEmpty()
                        ? " (" + quest.getSkill() + ")"
                        : "";
                lore.add(" &8• &a+" + quest.getRewardExp() + " EXP" + skillText);
                hasRewards = true;
            }

            // Dinero
            if (quest.getRewardMoney() > 0) {
                lore.add(" &8• &e$" + String.format("%.1f", quest.getRewardMoney()));
                hasRewards = true;
            }

            // Drops especiales (Manejo correcto de List)
            if (quest.getRewardDrops() != null) {
                Object dropsObj = quest.getRewardDrops();

                // Si getRewardDrops() devuelve una List (ej: List<String>)
                if (dropsObj instanceof List<?> dropList && !dropList.isEmpty()) {
                    for (Object dropItem : dropList) {
                        if (dropItem != null) {
                            lore.add(" &8• &d" + formatName(String.valueOf(dropItem)));
                            hasRewards = true;
                        }
                    }
                }
                // Si getRewardDrops() devuelve un Map (ej: Map<String, Integer>)
                else if (dropsObj instanceof Map<?, ?> dropMap && !dropMap.isEmpty()) {
                    for (Map.Entry<?, ?> entry : dropMap.entrySet()) {
                        String dropId = String.valueOf(entry.getKey());
                        int amount = entry.getValue() instanceof Number num ? num.intValue() : 1;
                        lore.add(" &8• &f" + amount + "x &d" + formatName(dropId));
                        hasRewards = true;
                    }
                }
            }

            // Items estándar
            if (quest.getRewardItems() != null) {
                Object itemsObj = quest.getRewardItems();
                if (itemsObj instanceof List<?> itemList && !itemList.isEmpty()) {
                    for (Object itemObj : itemList) {
                        if (itemObj == null) continue;

                        if (itemObj instanceof ItemStack itemStack) {
                            if (itemStack.getType() == Material.AIR) continue;
                            String itemName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
                                    ? itemStack.getItemMeta().getDisplayName()
                                    : formatName(itemStack.getType().name());

                            lore.add(" &8• &f" + itemStack.getAmount() + "x &f" + itemName);
                            hasRewards = true;
                        } else {
                            lore.add(" &8• &f" + formatName(String.valueOf(itemObj)));
                            hasRewards = true;
                        }
                    }
                }
            }

            if (!hasRewards) {
                lore.add(" &8• &7Ninguna");
            }

            // 3. Estado de seguimiento o acción
            lore.add("");
            if (!progress.isCompleted()) {
                lore.add(isTracked ? "&b⭐ Siguiendo esta ruta" : "&e▶ Click para rastrear");
            }
            lore.add("&7-------------------");

            // Construimos tu MenuItem adaptado
            MenuItem item = new MenuItem(
                    slot,
                    iconMaterial,
                    name,
                    lore,
                    progress.isCompleted() ? "NONE" : "COMMAND",
                    "pathtool goto " + quest.getId(), // Al hacer click ejecuta el comando de rastreo
                    1,
                    0.0
            );

            menu.addItem(item);
            slot++;
        }

        // Botón de cierre en el slot 22 usando tu sistema
        menu.addItem(new MenuItem(22, Material.BARRIER, "&cCerrar Menú", List.of("&7Salir de la interfaz"), "CLOSE", "", 1, 0.0));

        menu.open(player);
    }

    /**
     * Convierte claves como 'dense_diamond' a 'Dense Diamond'
     */
    private static String formatName(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] parts = text.replace("_", " ").toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}