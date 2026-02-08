package Fortcraft.skyworld.logbook;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.data.PlayerData;
import Fortcraft.skyworld.farming.FarmBiome;
import Fortcraft.skyworld.farming.FarmDrop;
import Fortcraft.skyworld.fishing.FishingBiome;
import Fortcraft.skyworld.fishing.FishingDrop;
import Fortcraft.skyworld.foraging.ForagingBiome;
import Fortcraft.skyworld.foraging.ForagingDrop;
import Fortcraft.skyworld.mining.MiningBiome;
import Fortcraft.skyworld.mining.MiningDrop;
import Fortcraft.skyworld.utils.ColorUtils;
import Fortcraft.skyworld.utils.PlayerMode;
import Fortcraft.skyworld.utils.Rarity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class LogbookGUI {

    private static final String KEY_BIOME_ID = "skyworld_biome_id";

    private static Component parse(String text) {
        return ColorUtils.format(text);
    }

    public static void open(Player player, PlayerMode mode, String biomeIdContext) {
        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());
        var zoneManager = Skyworld.getInstance().getManagerHandler().getZoneManager();

        String titleString = "&8Bitácora: " + mode.getDisplayName();

        if (biomeIdContext != null) {
            if (mode == PlayerMode.FISHING) {
                FishingBiome biome = zoneManager.getFishingBiome(biomeIdContext);
                if (biome != null) { openFishingBiomeView(player, biome, playerData); return; }
            } else if (mode == PlayerMode.MINING) {
                MiningBiome biome = zoneManager.getMiningBiome(biomeIdContext);
                if (biome != null) { openMiningBiomeView(player, biome, playerData); return; }
            } else if (mode == PlayerMode.FARMING) {
                FarmBiome biome = zoneManager.getFarmingBiome(biomeIdContext);
                if (biome != null) { openFarmBiomeView(player, biome, playerData); return; }
            } else if (mode == PlayerMode.FORAGING) {
                ForagingBiome biome = zoneManager.getForagingBiome(biomeIdContext);
                if (biome != null) { openForagingBiomeView(player, biome, playerData); return; }
            }
        }

        Inventory inv = Bukkit.createInventory(null, 54, parse(titleString));

        switch (mode) {
            case FISHING -> renderBiomeSelection(inv, zoneManager.getAllFishingBiomes(), playerData);
            case MINING -> renderBiomeSelection(inv, zoneManager.getAllMiningBiomes(), playerData);
            case FARMING -> renderBiomeSelection(inv, zoneManager.getAllFarmingBiomes(), playerData);
            case FORAGING -> renderBiomeSelection(inv, zoneManager.getAllForagingBiomes(), playerData);
            case COMBAT -> inv.setItem(22, createInfoIcon(Material.IRON_SWORD, "&cPróximamente", "&7Derrota enemigos..."));
            case GLOBAL -> renderGlobalStats(inv, playerData, zoneManager);
        }
        player.openInventory(inv);
    }

    private static void openMiningBiomeView(Player player, MiningBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 54, parse("<gray>Capa: " + biome.getDisplayName()));
        Map<Material, List<MiningDrop>> groupedBySource = biome.getAllDrops().stream().collect(Collectors.groupingBy(MiningDrop::getSource));

        for (Map.Entry<Material, List<MiningDrop>> entry : groupedBySource.entrySet()) {
            List<MiningDrop> blockDrops = entry.getValue();
            MiningDrop primary = blockDrops.getFirst();
            double totalWeight = blockDrops.stream().mapToDouble(MiningDrop::getWeight).sum();

            ItemStack icon = createDiscoveryIcon(
                    data.hasDiscovered(getCleanId(primary.getName())),
                    primary.getSource(),
                    primary.getName(),
                    primary.getRarity(),
                    getLoreMining(blockDrops, totalWeight)
            );
            inv.setItem(primary.getSlot(), icon);
        }
        addBackButton(inv);
        player.openInventory(inv);
    }

    private static void openFarmBiomeView(Player player, FarmBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 54, parse("<gray>Cultivos: " + biome.getDisplayName()));
        Map<Material, List<FarmDrop>> groupedBySource = biome.getAllDrops().stream().collect(Collectors.groupingBy(FarmDrop::getSourceBlock));

        for (Map.Entry<Material, List<FarmDrop>> entry : groupedBySource.entrySet()) {
            List<FarmDrop> cropDrops = entry.getValue();
            FarmDrop primary = cropDrops.getFirst();
            double totalWeight = cropDrops.stream().mapToDouble(FarmDrop::getWeight).sum();

            ItemStack icon = createDiscoveryIcon(
                    data.hasDiscovered(getCleanId(primary.getName())),
                    primary.getDropItem(),
                    primary.getName(),
                    primary.getRarity(),
                    getLoreFarm(cropDrops, totalWeight)
            );
            inv.setItem(primary.getSlot(), icon);
        }
        addBackButton(inv);
        player.openInventory(inv);
    }

    private static void openForagingBiomeView(Player player, ForagingBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 54, parse("<gray>Árboles: " + biome.getDisplayName()));
        Map<Material, List<ForagingDrop>> groupedBySource = biome.getAllDrops().stream().collect(Collectors.groupingBy(ForagingDrop::getSourceMaterial));

        for (Map.Entry<Material, List<ForagingDrop>> entry : groupedBySource.entrySet()) {
            List<ForagingDrop> treeDrops = entry.getValue();
            ForagingDrop primary = treeDrops.getFirst();
            double totalWeight = treeDrops.stream().mapToDouble(ForagingDrop::getWeight).sum();

            ItemStack icon = createDiscoveryIcon(
                    data.hasDiscovered(getCleanId(primary.getName())),
                    primary.getSourceMaterial(),
                    primary.getName(),
                    primary.getRarity(),
                    getLoreForaging(treeDrops, totalWeight)
            );
            inv.setItem(primary.getSlot(), icon);
        }
        addBackButton(inv);
        player.openInventory(inv);
    }

    private static void openFishingBiomeView(Player player, FishingBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 54, parse("<gray>Bioma: " + biome.getDisplayName()));
        Map<String, List<FishingDrop>> groupedDrops = biome.getDrops().stream()
                .collect(Collectors.groupingBy(FishingDrop::getGroupId, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<FishingDrop>> entry : groupedDrops.entrySet()) {
            List<FishingDrop> variants = entry.getValue();
            FishingDrop primary = variants.getFirst();
            boolean groupDiscovered = data.hasDiscovered(primary.getGroupId());

            List<Component> details = new ArrayList<>();
            details.add(parse("&8Especie: &7" + primary.getGroupId()));
            details.add(Component.empty());
            details.add(parse("&fPesajes registrados:"));

            for (FishingDrop var : variants) {
                boolean varDiscovered = data.hasDiscovered(getCleanId(var.getId()));
                String prefix = varDiscovered ? " &2✔ &a" : " &8✘ &7";
                String internalName = var.getId().substring(var.getId().lastIndexOf("_") + 1);
                internalName = internalName.substring(0, 1).toUpperCase() + internalName.substring(1);
                details.add(parse(prefix + internalName));
            }

            ItemStack icon = createDiscoveryIcon(
                    groupDiscovered,
                    primary.getMaterial(),
                    primary.getName(),
                    primary.getSpeciesRarity(),
                    details
            );
            inv.setItem(primary.getSlot(), icon);
        }
        addBackButton(inv);
        player.openInventory(inv);
    }

    private static void renderBiomeSelection(Inventory inv, java.util.Collection<?> biomes, PlayerData data) {
        int slot = 10;
        for (Object biomeObj : biomes) {
            String id = "";
            ItemStack icon = null;

            if (biomeObj instanceof FishingBiome b) {
                id = b.getId();
                icon = b.getGuiIcon((int) b.getUniqueSourceIds().stream().filter(data::hasDiscovered).count());
            } else if (biomeObj instanceof MiningBiome b) {
                id = b.getId();
                icon = b.getGuiIcon((int) b.getUniqueSourceIds().stream().filter(data::hasDiscovered).count());
            } else if (biomeObj instanceof FarmBiome b) {
                id = b.getId();
                icon = b.getGuiIcon((int) b.getUniqueSourceIds().stream().filter(data::hasDiscovered).count());
            } else if (biomeObj instanceof ForagingBiome b) {
                id = b.getId();
                icon = b.getGuiIcon((int) b.getUniqueSourceIds().stream().filter(data::hasDiscovered).count());
            }

            if (icon != null) {
                setItemBiomeData(icon, id);
                inv.setItem(slot++, icon);
            }

            if ((slot % 9) == 8) slot += 2;
            if (slot >= 44) break;
        }
        addBackButton(inv);
    }

    private static ItemStack createDiscoveryIcon(boolean discovered, Material mat, String rawName, Rarity rarity, List<Component> loreLines) {
        ItemStack item = new ItemStack(discovered ? mat : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (discovered) {
                meta.displayName(rarity.format(rawName));

                List<Component> lore = new ArrayList<>(loreLines);
                lore.add(Component.empty());
                lore.add(parse("&8&o✔ Ya descubierto"));
                meta.lore(lore);
            } else {
                meta.displayName(parse("&c???"));
                meta.lore(List.of(parse("&7Sigue explorando para desbloquear...")));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void renderGlobalStats(Inventory inv, PlayerData data, Fortcraft.skyworld.managers.ZoneManager zm) {
        long fishTotal = zm.getAllFishingDrops().stream().map(d -> getCleanId(d.getName())).distinct().count();
        long fishDiscovered = zm.getAllFishingDrops().stream().map(d -> getCleanId(d.getName())).distinct().filter(data::hasDiscovered).count();
        inv.setItem(19, createInfoIcon(Material.FISHING_ROD, "&bPesca", "&7Especies: &f" + fishDiscovered + "/" + fishTotal));

        long mineTotal = zm.getAllMiningBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().count();
        long mineDiscovered = zm.getAllMiningBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().filter(data::hasDiscovered).count();
        inv.setItem(21, createInfoIcon(Material.DIAMOND_PICKAXE, "&6Minería", "&7Fuentes: &f" + mineDiscovered + "/" + mineTotal));

        long farmTotal = zm.getAllFarmingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().count();
        long farmDiscovered = zm.getAllFarmingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().filter(data::hasDiscovered).count();
        inv.setItem(23, createInfoIcon(Material.GOLDEN_HOE, "&aGranja", "&7Cultivos: &f" + farmDiscovered + "/" + farmTotal));

        long foragTotal = zm.getAllForagingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().count();
        long foragDiscovered = zm.getAllForagingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().filter(data::hasDiscovered).count();
        inv.setItem(25, createInfoIcon(Material.IRON_AXE, "&2Foraging", "&7Recursos: &f" + foragDiscovered + "/" + foragTotal));

        markAsModeLink(inv.getItem(19), PlayerMode.FISHING);
        markAsModeLink(inv.getItem(21), PlayerMode.MINING);
        markAsModeLink(inv.getItem(23), PlayerMode.FARMING);
        markAsModeLink(inv.getItem(25), PlayerMode.FORAGING);
    }

    private static void addBackButton(Inventory inv) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.displayName(parse("&cVolver"));
            meta.getPersistentDataContainer().set(Skyworld.getKey("back_button"), PersistentDataType.BYTE, (byte) 1);
            back.setItemMeta(meta);
        }
        inv.setItem(49, back);
    }

    private static ItemStack createInfoIcon(Material mat, String name, String loreLine) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(parse(name));
            meta.lore(List.of(parse(loreLine)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static @NotNull List<Component> getLoreMining(List<MiningDrop> blockDrops, double totalWeight) {
        List<Component> details = new ArrayList<>();
        details.add(parse("&8Minería"));
        details.add(Component.empty());
        details.add(parse("&7Posibles drops:"));
        for (MiningDrop d : blockDrops) {
            double chance = (d.getWeight() / totalWeight) * 100;
            details.add(parse(" &8• ")
                    .append(d.getRarity().format(d.getDropName()))
                    .append(parse(" &7x" + d.getAmount() + " &f" + String.format("%.1f", chance) + "%")));
        }
        return details;
    }

    private static @NotNull List<Component> getLoreFarm(List<FarmDrop> cropDrops, double totalWeight) {
        List<Component> details = new ArrayList<>();
        details.add(parse("&8Agricultura"));
        details.add(Component.empty());
        details.add(parse("&7Posibles drops:"));
        for (FarmDrop d : cropDrops) {
            double chance = (d.getWeight() / totalWeight) * 100;
            details.add(parse(" &8• ")
                    .append(d.getRarity().format(d.getDropName()))
                    .append(parse(" &7x" + d.getAmount() + " &f" + String.format("%.1f", chance) + "%")));
        }
        return details;
    }

    private static @NotNull List<Component> getLoreForaging(List<ForagingDrop> treeDrops, double totalWeight) {
        List<Component> details = new ArrayList<>();
        details.add(parse("&8Recolección"));
        details.add(Component.empty());
        details.add(parse("&7Posibles drops:"));
        for (ForagingDrop d : treeDrops) {
            double chance = (d.getWeight() / totalWeight) * 100;
            details.add(parse(" &8• ")
                    .append(d.getRarity().format(d.getDropName()))
                    .append(parse(" &7x" + d.getAmount() + " &f" + String.format("%.1f", chance) + "%")));
        }
        return details;
    }

    public static String getCleanId(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "") // Quita tags de MiniMessage <blue>
                .replaceAll("§[0-9a-fklmnorx]", "") // Quita colores legacy y hex §x
                .replace("&", "") // Quita símbolos & sueltos
                .trim();
    }

    private static void setItemBiomeData(ItemStack item, String biomeId) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(Skyworld.getKey(KEY_BIOME_ID), PersistentDataType.STRING, biomeId);
        item.setItemMeta(meta);
    }

    private static void markAsModeLink(ItemStack item, PlayerMode mode) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(Skyworld.getKey("change_mode"), PersistentDataType.STRING, mode.name());
        item.setItemMeta(meta);
    }
}