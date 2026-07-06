package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.listeners.MenuListener;
import Fortcraft.skyworld.menu.MenuItem;
import Fortcraft.skyworld.menu.SkyblockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MenuManager implements Manager {

    private final Map<String, SkyblockMenu> menus = new HashMap<>();
    private File menuFile;
    private FileConfiguration menuConfig;

    @Override
    public void load() {
        menuFile = new File(Skyworld.getInstance().getDataFolder(), "menus.yml");
        if (!menuFile.exists()) {
            Skyworld.getInstance().saveResource("menus.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(menuFile);

        loadMenus();

        Bukkit.getPluginManager().registerEvents(new MenuListener(), Skyworld.getInstance());
    }

    @Override
    public void unload() {
        menus.clear();
    }

    private void loadMenus() {
        menus.clear();
        ConfigurationSection section = menuConfig.getConfigurationSection("menus");
        if (section == null) return;

        for (String menuId : section.getKeys(false)) {
            String title = section.getString(menuId + ".title");
            int size = section.getInt(menuId + ".size");

            SkyblockMenu menu = new SkyblockMenu(menuId, title, size);

            ConfigurationSection itemsSection = section.getConfigurationSection(menuId + ".items");
            if (itemsSection != null) {
                for (String slotStr : itemsSection.getKeys(false)) {
                    int slot = Integer.parseInt(slotStr);
                    String path = menuId + ".items." + slotStr;

                    Material mat = Material.valueOf(itemsSection.getString(slotStr + ".material"));
                    String name = itemsSection.getString(slotStr + ".name");
                    var lore = itemsSection.getStringList(slotStr + ".lore");
                    String action = itemsSection.getString(slotStr + ".action", "NONE");
                    String target = itemsSection.getString(slotStr + ".target-item", "");
                    int amount = itemsSection.getInt(slotStr + ".amount", 1);
                    double menuPriceModifier = itemsSection.getDouble(slotStr + ".price", 0.0);

                    double finalPrice = 0.0;

                    if (!target.isEmpty()) {
                        var template = ItemRegistry.getDropTemplates().get(target.toLowerCase());
                        if (template != null) {
                            Map<String, Double> customStats = template.customStats();
                            double basePrice = action.equalsIgnoreCase("SELL") ? customStats.get("sell_price").intValue() : customStats.get("buy_price").intValue();

                            finalPrice = (basePrice * amount) + menuPriceModifier;

                            if (finalPrice < 0) finalPrice = 0.0; // Evitamos precios negativos accidentales
                        } else {
                            // Fallback si el ítem no está en ItemRegistry (ej: un ítem vanilla genérico)
                            finalPrice = menuPriceModifier;
                        }
                    } else {
                        finalPrice = menuPriceModifier;
                    }
                    menu.addItem(new MenuItem(slot, mat, name, lore, action, target, amount, finalPrice));
                }
            }
            menus.put(menuId, menu);
        }
        Skyworld.getInstance().getLogger().info("Cargados " + menus.size() + " menús dinámicos.");
    }

    public SkyblockMenu getMenu(String id) {
        return menus.get(id);
    }
}