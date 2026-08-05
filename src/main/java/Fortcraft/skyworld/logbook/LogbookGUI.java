package Fortcraft.skyworld.logbook;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.data.PlayerData;
import Fortcraft.skyworld.farming.FarmBiome;
import Fortcraft.skyworld.farming.FarmDrop;
import Fortcraft.skyworld.fishing.FishingBiome;
import Fortcraft.skyworld.fishing.FishingDrop;
import Fortcraft.skyworld.foraging.ForagingBiome;
import Fortcraft.skyworld.foraging.ForagingDrop;
import Fortcraft.skyworld.items.ItemRegistry;
import Fortcraft.skyworld.mining.MiningBiome;
import Fortcraft.skyworld.mining.MiningDrop;
import Fortcraft.skyworld.utils.AnimatedHolder;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class LogbookGUI {

    private static final String KEY_BIOME_ID = "skyworld_biome_id";
    private static final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

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

        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse(titleString));

        switch (mode) {
            case FISHING -> renderBiomeSelection(inv, zoneManager.getAllFishingBiomes(), playerData);
            case MINING -> renderBiomeSelection(inv, zoneManager.getAllMiningBiomes(), playerData);
            case FARMING -> renderBiomeSelection(inv, zoneManager.getAllFarmingBiomes(), playerData);
            case FORAGING -> renderBiomeSelection(inv, zoneManager.getAllForagingBiomes(), playerData);
            case COMBAT -> inv.setItem(22, createInfoIcon(Material.IRON_SWORD, "&cPróximamente", "&7Derrota enemigos..."));
            case GLOBAL -> renderGlobalStats(inv, playerData, zoneManager);
        }
        player.openInventory(inv);
        startAnimationTask(player, inv);
    }

    private static void openMiningBiomeView(Player player, MiningBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse("Capa: " + biome.getDisplayName()));

        // Agrupamos todos los drops por el bloque de origen (Material)
        Map<Material, List<MiningDrop>> groupedBySource = biome.getAllDrops().stream()
                .collect(Collectors.groupingBy(MiningDrop::getSource));

        List<Map.Entry<Material, List<MiningDrop>>> sortedEntries = new ArrayList<>(groupedBySource.entrySet());

        // Ordenamos las fuentes en el menú según la rareza de su PRIMER drop (o el criterio que prefieras)
        sortedEntries.sort((e1, e2) -> {
            var t1 = ItemRegistry.getDropTemplates().get(e1.getValue().getFirst().itemId());
            var t2 = ItemRegistry.getDropTemplates().get(e2.getValue().getFirst().itemId());
            Rarity r1 = t1 != null ? Rarity.fromString(t1.rarity()) : Rarity.COMUN;
            Rarity r2 = t2 != null ? Rarity.fromString(t2.rarity()) : Rarity.COMUN;
            return Integer.compare(r1.ordinal(), r2.ordinal());
        });

        int slot = 10;
        for (Map.Entry<Material, List<MiningDrop>> entry : sortedEntries) {
            List<MiningDrop> blockDrops = entry.getValue();
            MiningDrop primary = blockDrops.getFirst(); // Tomamos el primer drop como referencia de la fuente
            double totalWeight = blockDrops.stream().mapToDouble(MiningDrop::getWeight).sum();

            var template = ItemRegistry.getDropTemplates().get(primary.itemId());
            Material displayMat = primary.getSource(); // El icono del menú será el bloque físico (ej: IRON_ORE)
            Rarity rarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

            boolean isSourceDiscovered = data.hasDiscovered(primary.getSourceId());

            ItemStack icon = createDiscoveryIcon(
                    isSourceDiscovered,
                    displayMat,          // El bloque (ej: Material.IRON_ORE)
                    primary.getName(),   // El nombre visual del bloque (ej: "&eMena de Hierro")
                    rarity,
                    getLoreMining(blockDrops, totalWeight) // Muestra los porcentajes de lo que puede dar
            );

            inv.setItem(slot++, icon);
            if ((slot % 9) == 8) slot += 2;
        }
        addBackButton(inv);
        player.openInventory(inv);
        startAnimationTask(player, inv);
    }

    private static void openFarmBiomeView(Player player, FarmBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse("Cultivos: " + biome.getDisplayName()));

        Map<Material, List<FarmDrop>> groupedBySource = biome.getAllDrops().stream()
                .collect(Collectors.groupingBy(FarmDrop::getSourceBlock));

        List<Map.Entry<Material, List<FarmDrop>>> sortedEntries = new ArrayList<>(groupedBySource.entrySet());
        sortedEntries.sort((e1, e2) -> {
            var t1 = ItemRegistry.getDropTemplates().get(e1.getValue().getFirst().itemId());
            var t2 = ItemRegistry.getDropTemplates().get(e2.getValue().getFirst().itemId());
            Rarity r1 = t1 != null ? Rarity.fromString(t1.rarity()) : Rarity.COMUN;
            Rarity r2 = t2 != null ? Rarity.fromString(t2.rarity()) : Rarity.COMUN;
            return Integer.compare(r1.ordinal(), r2.ordinal());
        });

        int slot = 10;
        for (Map.Entry<Material, List<FarmDrop>> entry : sortedEntries) {
            List<FarmDrop> cropDrops = entry.getValue();
            FarmDrop primary = cropDrops.getFirst();
            double totalWeight = cropDrops.stream().mapToDouble(FarmDrop::getWeight).sum();

            var template = ItemRegistry.getDropTemplates().get(primary.itemId());
            Material displayMat = template != null ? template.material() : primary.getSourceBlock();
            Rarity rarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

            // REFACTOR: Usar getSourceId() de la fuente
            ItemStack icon = createDiscoveryIcon(
                    data.hasDiscovered(primary.getSourceId()),
                    displayMat,
                    primary.getName(),
                    rarity,
                    getLoreFarm(cropDrops, totalWeight)
            );
            inv.setItem(slot++, icon);
            if ((slot % 9) == 8) slot += 2;
        }
        addBackButton(inv);
        player.openInventory(inv);
        startAnimationTask(player, inv);
    }

    private static void openForagingBiomeView(Player player, ForagingBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse("Árboles: " + biome.getDisplayName()));

        Map<Material, List<ForagingDrop>> groupedBySource = biome.getAllDrops().stream()
                .collect(Collectors.groupingBy(ForagingDrop::getSourceMaterial));

        List<Map.Entry<Material, List<ForagingDrop>>> sortedEntries = new ArrayList<>(groupedBySource.entrySet());
        sortedEntries.sort((e1, e2) -> {
            var t1 = ItemRegistry.getDropTemplates().get(e1.getValue().getFirst().itemId());
            var t2 = ItemRegistry.getDropTemplates().get(e2.getValue().getFirst().itemId());
            Rarity r1 = t1 != null ? Rarity.fromString(t1.rarity()) : Rarity.COMUN;
            Rarity r2 = t2 != null ? Rarity.fromString(t2.rarity()) : Rarity.COMUN;
            return Integer.compare(r1.ordinal(), r2.ordinal());
        });

        int slot = 10;
        for (Map.Entry<Material, List<ForagingDrop>> entry : sortedEntries) {
            List<ForagingDrop> treeDrops = entry.getValue();
            ForagingDrop primary = treeDrops.getFirst();
            double totalWeight = treeDrops.stream().mapToDouble(ForagingDrop::getWeight).sum();

            var template = ItemRegistry.getDropTemplates().get(primary.itemId());
            Material displayMat = primary.getSourceMaterial();
            Rarity rarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

            // REFACTOR: Usar getSourceId() de la fuente
            ItemStack icon = createDiscoveryIcon(
                    data.hasDiscovered(primary.getSourceId()),
                    displayMat,
                    primary.getName(),
                    rarity,
                    getLoreForaging(treeDrops, totalWeight)
            );
            inv.setItem(slot++, icon);
            if ((slot % 9) == 8) slot += 2;
        }
        addBackButton(inv);
        player.openInventory(inv);
        startAnimationTask(player, inv);
    }

    private static void openFishingBiomeView(Player player, FishingBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse("Bioma: " + biome.getDisplayName()));

        Map<String, List<FishingDrop>> groupedDrops = biome.getDrops().stream()
                .collect(Collectors.groupingBy(FishingDrop::getGroupId));

        List<Map.Entry<String, List<FishingDrop>>> sortedEntries = new ArrayList<>(groupedDrops.entrySet());
        sortedEntries.sort((e1, e2) -> {
            Rarity r1 = e1.getValue().getFirst().getSpeciesRarity();
            Rarity r2 = e2.getValue().getFirst().getSpeciesRarity();
            return Integer.compare(r1.ordinal(), r2.ordinal());
        });

        int slot = 10;
        for (Map.Entry<String, List<FishingDrop>> entry : sortedEntries) {
            List<FishingDrop> variants = entry.getValue();
            FishingDrop primary = variants.getFirst();

            String cleanGroupId = primary.getGroupId().toLowerCase();

            boolean groupDiscovered = data.hasDiscovered(cleanGroupId)
                    || variants.stream().anyMatch(v -> data.hasDiscovered(v.getItemId().toLowerCase()));

            String fishName = primary.getName();

            List<Component> details = new ArrayList<>();
            details.add(Component.empty());
            details.add(parse("&fPesajes registrados:"));

            for (FishingDrop var : variants) {
                boolean varDiscovered = data.hasDiscovered(var.getItemId().toLowerCase());
                String prefix = varDiscovered ? " &2✔ &a" : " &8✘ &7";

                String sizeLabel = switch (var.getSize().toUpperCase()) {
                    case "S" -> "Pequeño";
                    case "M" -> "Mediano";
                    case "L" -> "Grande";
                    case "XL" -> "Gigante";
                    default -> "Único";
                };

                if (!var.getSize().isEmpty()) {
                    details.add(parse(prefix + sizeLabel + " &7(" + var.getSize() + ")"));
                } else {
                    details.add(parse(prefix + sizeLabel));
                }
            }

            ItemStack icon = createDiscoveryIcon(
                    groupDiscovered,
                    primary.getMaterial(),
                    fishName,
                    primary.getSpeciesRarity(),
                    details
            );

            inv.setItem(slot++, icon);
            if ((slot % 9) == 8) slot += 2;
        }
        addBackButton(inv);
        player.openInventory(inv);
        startAnimationTask(player, inv);
    }

    private static void startAnimationTask(Player player, Inventory inv) {
        if (activeTasks.containsKey(player.getUniqueId())) {
            activeTasks.get(player.getUniqueId()).cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getOpenInventory().getTopInventory() != inv) {
                    this.cancel();
                    activeTasks.remove(player.getUniqueId());
                    return;
                }

                for (ItemStack item : inv.getContents()) {
                    if (item == null || !item.hasItemMeta()) continue;

                    ItemMeta meta = item.getItemMeta();
                    var pdc = meta.getPersistentDataContainer();

                    if (pdc.has(Skyworld.getKey("rarity"), PersistentDataType.STRING)) {
                        String rarityName = pdc.get(Skyworld.getKey("rarity"), PersistentDataType.STRING);
                        String originalName = pdc.get(Skyworld.getKey("original_name"), PersistentDataType.STRING);

                        try {
                            Rarity rarity = Rarity.valueOf(rarityName);
                            if (isAnimatedRarity(rarity) && originalName != null) {
                                meta.displayName(ColorUtils.getAnimatedName(originalName, rarity));
                                item.setItemMeta(meta);
                            }
                        } catch (Exception ignored) { }
                    }
                }
            }
        }.runTaskTimer(Skyworld.getInstance(), 1L, 3L);

        activeTasks.put(player.getUniqueId(), task);
    }

    private static boolean isAnimatedRarity(Rarity rarity) {
        String n = rarity.name().toUpperCase();
        return n.equals("LEGENDARIO") || n.equals("EXOTICO") || n.equals("MYTHIC");
    }

    private static ItemStack createDiscoveryIcon(boolean discovered, Material mat, String rawName, Rarity rarity, List<Component> loreLines) {
        ItemStack item = new ItemStack(discovered ? mat : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (discovered) {
                meta.displayName(ColorUtils.getAnimatedName(rawName, rarity));

                meta.getPersistentDataContainer().set(Skyworld.getKey("rarity"), PersistentDataType.STRING, rarity.name());
                meta.getPersistentDataContainer().set(Skyworld.getKey("original_name"), PersistentDataType.STRING, rawName);

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

    private static void renderGlobalStats(Inventory inv, PlayerData data, Fortcraft.skyworld.managers.ZoneManager zm) {
        long fishTotal = zm.getAllFishingDrops().stream().map(d -> d.getGroupId().toLowerCase()).distinct().count();
        long fishDiscovered = zm.getAllFishingDrops().stream().map(d -> d.getGroupId().toLowerCase()).distinct().filter(data::hasDiscovered).count();
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
            var template = ItemRegistry.getDropTemplates().get(d.itemId());
            String dName = template != null ? template.displayName() : d.itemId();
            Rarity dRarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

            details.add(parse(" &8• ")
                    .append(dRarity.format(dName))
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
            var template = ItemRegistry.getDropTemplates().get(d.itemId());
            String dName = template != null ? template.displayName() : d.itemId();
            Rarity dRarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

            details.add(parse(" &8• ")
                    .append(dRarity.format(dName))
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
            var template = ItemRegistry.getDropTemplates().get(d.itemId());
            String dName = template != null ? template.displayName() : d.itemId();
            Rarity dRarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

            details.add(parse(" &8• ")
                    .append(dRarity.format(dName))
                    .append(parse(" &7x" + d.getAmount() + " &f" + String.format("%.1f", chance) + "%")));
        }
        return details;
    }

    public static String getCleanId(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "")
                .replaceAll("§[0-9a-fklmnorx]", "")
                .replace("&", "")
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