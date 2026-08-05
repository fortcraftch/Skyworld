package Fortcraft.skyworld.data;

import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.storage.StorageBag;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.HotbarSlot;
import Fortcraft.skyworld.utils.PlayerMode;
import Fortcraft.skyworld.utils.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

public class PlayerData {
    private final UUID uuid;
    private final StorageBag storageBag;

    private double coins = 0.0;
    private final Set<String> discoveredItems = new HashSet<>();
    private final Map<PlayerMode, Map<Integer, String>> loadouts = new HashMap<>();

    // Mapas para gestionar las Skills
    private final Map<String, Integer> skillLevels = new HashMap<>();
    private final Map<String, Double> skillXp = new HashMap<>();

    // NUEVO: Sistema de Action Bar y Salud Custom
    private double customHealth = 100.0;
    private double customMaxHealth = 100.0;
    private final Map<String, Double> pendingActionbarXp = new LinkedHashMap<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.storageBag = new StorageBag(uuid);
        for (PlayerMode mode : PlayerMode.values()) {
            loadouts.put(mode, new HashMap<>());
        }

        String[] defaultSkills = {"combat", "mining", "fishing", "farming", "foraging"};
        for (String skill : defaultSkills) {
            skillLevels.put(skill, 1);
            skillXp.put(skill, 0.0);
        }

        startActionBarTask();
    }

    // --- GETTERS Y SETTERS DE SALUD CUSTOM ---
    public double getCustomHealth() { return customHealth; }
    public void setCustomHealth(double customHealth) { this.customHealth = Math.min(customHealth, customMaxHealth); }
    public double getCustomMaxHealth() { return customMaxHealth; }
    public void setCustomMaxHealth(double customMaxHealth) { this.customMaxHealth = customMaxHealth; }

    // --- GETTERS Y SETTERS DE SKILLS ---
    public int getSkillLevel(String skill) { return skillLevels.getOrDefault(skill.toLowerCase(), 1); }
    public void setSkillLevel(String skill, int level) { skillLevels.put(skill.toLowerCase(), level); }

    public double getSkillXp(String skill) { return skillXp.getOrDefault(skill.toLowerCase(), 0.0); }
    public void setSkillXp(String skill, double xp) { skillXp.put(skill.toLowerCase(), xp); }
    public void addSkillXp(String skill, double amount) {
        String key = skill.toLowerCase();
        skillXp.put(key, getSkillXp(key) + amount);
    }

    // --- NUEVO: ACUMULADOR DE XP PARA ACTION BAR ---
    public void queueActionbarXp(String skill, double amount) {
        String key = skill.toLowerCase();
        // Si ya hay XP pendiente de esta skill en este segundo, la suma. Si no, crea la entrada al final de la cola.
        pendingActionbarXp.put(key, pendingActionbarXp.getOrDefault(key, 0.0) + amount);
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(uuid);
                // Si el jugador se desconecta, cancelamos su reloj personal para no consumir memoria
                if (player == null || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                if (!pendingActionbarXp.isEmpty()) {
                    // Extraemos el primer elemento que entró (FIFO)
                    Iterator<Map.Entry<String, Double>> it = pendingActionbarXp.entrySet().iterator();
                    Map.Entry<String, Double> entry = it.next();
                    String skill = entry.getKey();
                    double amount = entry.getValue();
                    it.remove(); // Lo eliminamos de la cola (reiniciando su ciclo)

                    int currentLevel = getSkillLevel(skill);
                    double currentXp = getSkillXp(skill);

                    var skillManager = Fortcraft.skyworld.Skyworld.getInstance().getManagerHandler().getSkillManager();
                    int maxLevel = skillManager.getMaxLevel(skill);

                    String msg;
                    if (currentLevel >= maxLevel) {
                        msg = String.format("&6&l%s &7| &fNivel &a%d &c(MÁXIMO) &7| &e+%.1f XP", skill.toUpperCase(), currentLevel, amount);
                    } else {
                        double reqXp = skillManager.getRequiredXpForLevel(skill, currentLevel + 1);
                        msg = String.format("&6&l%s &7| &fNivel &a%d &7| &e+%.1f XP &7(&b%.1f&7/&b%.1f&7)",
                                skill.toUpperCase(), currentLevel, amount, currentXp, reqXp);
                    }

                    player.spigot().sendMessage(
                            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(org.bukkit.ChatColor.translateAlternateColorCodes('&', msg))
                    );

                } else {
                    // Si la cola de XP está vacía, mostramos la vida por defecto
                    String msg = String.format("&c❤ %.1f &7/ &c%.1f", customHealth, customMaxHealth);
                    player.spigot().sendMessage(
                            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(org.bukkit.ChatColor.translateAlternateColorCodes('&', msg))
                    );
                }
            }
        }.runTaskTimer(Fortcraft.skyworld.Skyworld.getInstance(), 20L, 20L); // 20 ticks = 1 segundo
    }

    public UUID getUuid() { return uuid; }
    public StorageBag getStorageBag() { return storageBag; }

    public double getCoins() { return coins; }
    public void setCoins(double coins) { this.coins = coins; }
    public void addCoins(double amount) { this.coins += amount; }
    public boolean removeCoins(double amount) {
        if (this.coins >= amount) {
            this.coins -= amount;
            return true;
        }
        return false;
    }

    public void setLoadoutItem(PlayerMode mode, HotbarSlot slot, String itemId) {
        loadouts.get(mode).put(slot.getSlotIndex(), itemId);
    }
    public String getLoadoutItem(PlayerMode mode, int slotIndex) {
        return loadouts.get(mode).get(slotIndex);
    }
    public Map<Integer, String> getLoadoutForMode(PlayerMode mode) { return loadouts.get(mode); }

    public void discover(String id) { discoveredItems.add(id); }

    public void discover(UUID uuid, String id, String friendlyName) {
        String cleanId = id.toLowerCase();
        if (!discoveredItems.contains(cleanId)) {
            discoveredItems.add(cleanId);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                sendDiscoveryNotification(player, friendlyName);
            }
        }
    }

    private void sendDiscoveryNotification(Player player, String friendlyName) {
        Component mainTitle = LegacyComponentSerializer.legacySection().deserialize("§6§l¡NUEVA ENTRADA!");
        Component subTitle = LegacyComponentSerializer.legacySection().deserialize("§fHas descubierto: " + friendlyName);

        Title title = Title.title(
                mainTitle, subTitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
        );

        player.showTitle(title);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.4f, 1.2f);
    }

    public boolean hasDiscovered(String id) { return discoveredItems.contains(id.toLowerCase()); }
    public Set<String> getDiscoveredItems() { return discoveredItems; }

    public void loadQuestsProgress(org.bukkit.configuration.file.FileConfiguration playerConfig) {
        var questManager = Fortcraft.skyworld.Skyworld.getInstance().getManagerHandler().getQuestManager();
        if (questManager != null) questManager.loadPlayerProgress(this.uuid, playerConfig);
    }

    public void saveQuestsProgress(org.bukkit.configuration.file.FileConfiguration playerConfig) {
        var questManager = Fortcraft.skyworld.Skyworld.getInstance().getManagerHandler().getQuestManager();
        if (questManager != null) questManager.savePlayerProgress(this.uuid, playerConfig);
    }

    private final Map<String, Integer> pendingChatDrops = new HashMap<>();
    private org.bukkit.scheduler.BukkitTask chatDropTask = null;

    public void queueChatDrop(String itemId, int amount) {
        pendingChatDrops.put(itemId, pendingChatDrops.getOrDefault(itemId, 0) + amount);

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;

        // Si ya hay una tarea corriendo, la cancelamos para posponer el envío 3 segundos más
        if (chatDropTask != null) {
            chatDropTask.cancel();
        }

        // Programamos la tarea para que se ejecute a los 3 segundos (60 ticks) de inactividad
        chatDropTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                flushChatDrops(player);
            }
        }.runTaskLater(Fortcraft.skyworld.Skyworld.getInstance(), 40L);
    }

    private void flushChatDrops(Player player) {
        if (pendingChatDrops.isEmpty()) return;

        player.sendMessage(Fortcraft.skyworld.utils.ColorUtils.format("&8&m-----------------------------------"));
        player.sendMessage(Fortcraft.skyworld.utils.ColorUtils.format("&6&l¡Has recolectado los siguientes recursos!"));

        for (Map.Entry<String, Integer> entry : pendingChatDrops.entrySet()) {
            String itemId = entry.getKey();
            int totalAmount = entry.getValue();

            var template = ItemRegistry.getDropTemplates().get(itemId);
            Rarity rarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;
            var formattedName = ColorUtils.getAnimatedName(template != null ? template.displayName() : itemId, rarity);

            player.sendMessage(Fortcraft.skyworld.utils.ColorUtils.format("&7- &3" + totalAmount + "x ").append(formattedName));
        }

        player.sendMessage(Fortcraft.skyworld.utils.ColorUtils.format("&8&m-----------------------------------"));

        // Limpiamos el mapa y la referencia de la tarea
        pendingChatDrops.clear();
        chatDropTask = null;
    }
}