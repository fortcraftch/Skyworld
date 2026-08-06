package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.data.PlayerData;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.Rarity;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;

public class SkillManager implements Manager {

    private File file;
    private FileConfiguration config;

    @Override
    public void load() {
        this.file = new File(Skyworld.getInstance().getDataFolder(), "skills.yml");
        if (!file.exists()) {
            Skyworld.getInstance().saveResource("skills.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void unload() {
        // Limpieza si es necesario
    }

    // Nuevo método auxiliar para que el PlayerData sepa cuál es el tope
    public int getMaxLevel(String skill) {
        return config.getInt("skills." + skill.toLowerCase() + ".max_level", 30);
    }

    public double getRequiredXpForLevel(String skill, int targetLevel) {
        String path = "skills." + skill.toLowerCase() + ".progression";
        double baseXp = config.getDouble(path + ".base_xp", 100.0);
        double multiplier = config.getDouble(path + ".multiplier", 1.5);

        return baseXp * Math.pow((targetLevel - 1), multiplier);
    }

    public void giveXp(Player player, String skill, double amount) {
        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        String skillName = skill.toLowerCase();
        int maxLevel = getMaxLevel(skillName);
        int currentLevel = data.getSkillLevel(skillName);

        // Sin importar si es max level o no, encolamos el texto visual de la Action Bar
        data.queueActionbarXp(skillName, amount);

        if (currentLevel >= maxLevel) return; // Top level, no damos XP real

        data.addSkillXp(skillName, amount);
        double xpRequerida = getRequiredXpForLevel(skillName, currentLevel + 1);

        if (data.getSkillXp(skillName) >= xpRequerida) {
            levelUp(player, data, skillName, currentLevel + 1, maxLevel);
        }
    }

    private void levelUp(Player player, PlayerData data, String skill, int newLevel, int maxLevel) {
        double xpSobrante = data.getSkillXp(skill) - getRequiredXpForLevel(skill, newLevel);
        data.setSkillXp(skill, Math.max(0, xpSobrante));
        data.setSkillLevel(skill, newLevel);

        player.sendTitle(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6&l¡NUEVO NIVEL!"),
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&fNivel de &e" + skill.toUpperCase() + " &fha subido a &a" + newLevel),
                10, 60, 10
        );
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        grantLevelUpRewards(player, skill, newLevel);

        if (data.getSkillXp(skill) >= getRequiredXpForLevel(skill, newLevel + 1) && newLevel < maxLevel) {
            levelUp(player, data, skill, newLevel + 1, maxLevel);
        }
    }

    private void grantLevelUpRewards(Player player, String skill, int level) {
        String levelPath = "skills." + skill.toLowerCase() + ".levels." + level;
        ConfigurationSection section = config.getConfigurationSection(levelPath);

        if (section == null) return;

        player.sendMessage(ColorUtils.format("&6¡Recompensas por subir de nivel!"));

        // 1. Dinero
        double money = section.getDouble("money", 0.0);
        if (money > 0) {
            var eco = Skyworld.getInstance().getManagerHandler().getEconomyManager();
            eco.addCoins(player, money);
            player.sendMessage(ColorUtils.format("&7[&a+&7] &a$" + money + " Monedas"));
        }

        // 2. Comandos
        List<String> commands = section.getStringList("commands");
        for (String cmd : commands) {
            String parsedCmd = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
        }

        // 3. Ítems
        List<String> items = section.getStringList("items");
        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());

        for (String itemStr : items) {
            String[] split = itemStr.split(":");
            String itemId = split[0];
            int amount = split.length > 1 ? Integer.parseInt(split[1]) : 1;

            ItemStack itemStack = ItemRegistry.build(itemId);
            if (itemStack != null) {
                itemStack.setAmount(amount);
                var template = ItemRegistry.getDropTemplates().get(itemId);
                Rarity rarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

                playerData.getStorageBag().addItem(itemStack, itemId, rarity);

                var formattedName = ColorUtils.getAnimatedName(template != null ? template.displayName() : itemId, rarity);
                player.sendMessage(ColorUtils.format("&7[&a+&7] &3" + amount + "x ").append(formattedName));
            }
        }
    }
}