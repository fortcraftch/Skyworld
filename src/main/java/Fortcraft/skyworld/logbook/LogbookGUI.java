package Fortcraft.skyworld.logbook;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.data.PlayerData;
import Fortcraft.skyworld.excavation.ExcavationBiome;
import Fortcraft.skyworld.excavation.ExcavationDrop;
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
import org.bukkit.enchantments.Enchantment;
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
    private static final String KEY_LOGBOOK_CONTEXT = "logbook_context";
    private static final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    private static Component parse(String text) {
        return ColorUtils.format(text);
    }

    public static void open(Player player, PlayerMode mode, String context) {
        var dataManager = Skyworld.getInstance().getManagerHandler().getDataManager();
        var playerData = dataManager.getPlayerData(player.getUniqueId());
        var zoneManager = Skyworld.getInstance().getManagerHandler().getZoneManager();

        String titleString = "&8Bitácora: " + mode.getDisplayName();

        // 1. Menú Global Principal
        if (mode == PlayerMode.GLOBAL) {
            Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse(titleString));
            renderGlobalStats(inv, playerData, zoneManager);
            player.openInventory(inv);
            startAnimationTask(player, inv);
            return;
        }

        // 2. Menú Intermedio (Elegir entre Biomas o Skills)
        if (context == null || context.equals("OPTIONS")) {
            openModeOptions(player, mode, playerData);
            return;
        }

        // 3. Menú de Ruta de Habilidades (Serpiente Vertical Paginada)
        if (context.startsWith("SKILLS")) {
            int page = 1;
            if (context.contains("_")) {
                try {
                    page = Integer.parseInt(context.split("_")[1]);
                } catch (NumberFormatException ignored) {}
            }
            openSkillTree(player, mode, playerData, page);
            return;
        }

        // 4. Menú de Selección de Biomas
        if (context.equals("BIOMES")) {
            Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse("&8Biomas: " + mode.getDisplayName()));
            switch (mode) {
                case FISHING -> renderBiomeSelection(inv, zoneManager.getAllFishingBiomes(), playerData);
                case MINING -> renderBiomeSelection(inv, zoneManager.getAllMiningBiomes(), playerData);
                case FARMING -> renderBiomeSelection(inv, zoneManager.getAllFarmingBiomes(), playerData);
                case FORAGING -> renderBiomeSelection(inv, zoneManager.getAllForagingBiomes(), playerData);
                case EXCAVATION -> renderBiomeSelection(inv, zoneManager.getAllExcavationBiomes(), playerData);
                case COMBAT -> inv.setItem(22, createInfoIcon(Material.IRON_SWORD, "&cPróximamente", "&7Derrota enemigos..."));
            }
            addContextualBackButton(inv, mode, "OPTIONS");
            player.openInventory(inv);
            startAnimationTask(player, inv);
            return;
        }

        // 5. Menús específicos de cada bioma
        if (mode == PlayerMode.FISHING) {
            FishingBiome biome = zoneManager.getFishingBiome(context);
            if (biome != null) { openFishingBiomeView(player, biome, playerData);
            }
        } else if (mode == PlayerMode.MINING) {
            MiningBiome biome = zoneManager.getMiningBiome(context);
            if (biome != null) { openMiningBiomeView(player, biome, playerData);
            }
        } else if (mode == PlayerMode.FARMING) {
            FarmBiome biome = zoneManager.getFarmingBiome(context);
            if (biome != null) { openFarmBiomeView(player, biome, playerData);
            }
        } else if (mode == PlayerMode.FORAGING) {
            ForagingBiome biome = zoneManager.getForagingBiome(context);
            if (biome != null) { openForagingBiomeView(player, biome, playerData);
            }
        } else if (mode == PlayerMode.EXCAVATION) {
            ExcavationBiome biome = zoneManager.getExcavationBiome(context);
            if (biome != null) { openExcavationBiomeView(player, biome, playerData);
            }
        }
    }

    private static void openModeOptions(Player player, PlayerMode mode, PlayerData data) {
        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 27, parse("&8Ruta: " + mode.getDisplayName()));

        int currentLvl = data.getSkillLevel(mode.name());
        int maxLvl = 100;
        var sm = Skyworld.getInstance().getManagerHandler().getSkillManager();
        var zm = Skyworld.getInstance().getManagerHandler().getZoneManager();
        try { maxLvl = sm.getMaxLevel(mode.name()); } catch (Exception ignored) {}

        // Calcular recursos descubiertos totales según el modo
        long totalResources = 0;
        long discoveredResources = 0;

        switch (mode) {
            case FISHING -> {
                totalResources = zm.getAllFishingDrops().stream().map(d -> d.getGroupId().toLowerCase()).distinct().count();
                discoveredResources = zm.getAllFishingDrops().stream().map(d -> d.getGroupId().toLowerCase()).distinct().filter(data::hasDiscovered).count();
            }
            case MINING -> {
                totalResources = zm.getAllMiningBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().count();
                discoveredResources = zm.getAllMiningBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().filter(data::hasDiscovered).count();
            }
            case FARMING -> {
                totalResources = zm.getAllFarmingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().count();
                discoveredResources = zm.getAllFarmingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().filter(data::hasDiscovered).count();
            }
            case FORAGING -> {
                totalResources = zm.getAllForagingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().count();
                discoveredResources = zm.getAllForagingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().filter(data::hasDiscovered).count();
            }
            case EXCAVATION -> {
                totalResources = zm.getAllExcavationBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().count();
                discoveredResources = zm.getAllExcavationBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().filter(data::hasDiscovered).count();
            }
            default -> {}
        }

        // Botón de Biomas con el progreso de descubrimientos
        ItemStack biomesBtn = createInfoIcon(Material.MAP, "&6Recursos y Drops",
                "&7Descubiertos: &f" + discoveredResources + "&8/&f" + totalResources,
                "",
                "&7Mira los recursos disponibles en esta zona.");
        setContextAction(biomesBtn, mode, "BIOMES");
        inv.setItem(11, biomesBtn);

        // Botón de Ruta de Habilidad
        ItemStack skillsBtn = createInfoIcon(Material.EXPERIENCE_BOTTLE, "&bRuta de Habilidad",
                "&7Nivel Actual: &f" + currentLvl + "&8/&f" + maxLvl,
                "",
                "&7Observa tu progreso y próximos desbloqueos.");
        setContextAction(skillsBtn, mode, "SKILLS_1");
        inv.setItem(15, skillsBtn);

        addContextualBackButton(inv, PlayerMode.GLOBAL, null);
        player.openInventory(inv);
    }

    private static void openSkillTree(Player player, PlayerMode mode, PlayerData data, int page) {
        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse("&8Habilidad: " + mode.getDisplayName() + " (Pág " + page + ")"));

        int currentLevel = data.getSkillLevel(mode.name());
        int maxLevel = 100;
        var skillManager = Skyworld.getInstance().getManagerHandler().getSkillManager();
        try { maxLevel = skillManager.getMaxLevel(mode.name()); } catch (Exception ignored) {}

        // Definición de los mapas de recorrido
        int[] snakePathFirst = {
                10, 19, 28, 37, // Baja col 1
                38,
                39,
                40, 31, 22, 13, // Sube col 4
                14,
                15,
                16, 25, 34, 43, // Baja col 7
                44
        };

        int[] snakePathPar = {
                36,
                37, 28, 19, 10, // Baja col 1
                11,
                12,
                13, 22, 31, 40, // Sube col 4
                41,
                42,
                43, 34, 25, 16, // Baja col 7
                17
        };

        int[] snakePathSenar = {
                9,
                10, 19, 28, 37, // Baja col 1
                38,
                39,
                40, 31, 22, 13, // Sube col 4
                14,
                15,
                16, 25, 34, 43, // Baja col 7
                44
        };

        // Selección del snakePath según la página
        int[] activeSnakePath;
        if (page == 1) {
            activeSnakePath = snakePathFirst;
        } else if (page % 2 == 0) {
            activeSnakePath = snakePathPar;
        } else {
            activeSnakePath = snakePathSenar;
        }

        // Cálculo dinámico del índice de inicio basado en la página y la longitud del mapa activo
        // Nota: Como la página 1 usa snakePathFirst y las demás alternan, el offset de índices se calcula sumando las longitudes correspondientes.
        int startIndex = snakePathFirst.length;
        if (page > 1) {
            // Página 2 usa snakePathPar (longitud X), páginas siguientes alternan
            startIndex = snakePathFirst.length;
            for (int p = 2; p < page; p++) {
                startIndex += (p % 2 == 0 ? snakePathPar.length : snakePathSenar.length);
            }
        } else {
            startIndex = 0;
        }

        for (int i = 0; i < activeSnakePath.length; i++) {
            int level = startIndex + i + 1;
            if (level > maxLevel) break;

            int slot = activeSnakePath[i];
            boolean unlocked = currentLevel >= level;
            boolean isNext = level == currentLevel + 1;
            boolean isMilestone = (level % 5 == 0);

            Material mat;
            String name = (unlocked ? "&a" : (isNext ? "&e" : "&c")) + "Nivel " + level;
            List<Component> lore = new ArrayList<>();

            if (isMilestone) {
                mat = unlocked ? Material.EMERALD_BLOCK : (isNext ? Material.GOLD_BLOCK : Material.REDSTONE_BLOCK);
                lore.add(parse("&e&l¡Hito Importante!"));
            } else {
                mat = unlocked ? Material.LIME_STAINED_GLASS_PANE : (isNext ? Material.YELLOW_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
            }

            // --- EXTRACCIÓN DE DATOS REALES (SKILLMANAGER Y PLAYERDATA) ---
            double expRequired = 0.0;
            double currentExp = 0.0;

            try {
                expRequired = skillManager.getRequiredXpForLevel(mode.name(), level);
                currentExp = data.getSkillXp(mode.name());
            } catch (Exception ignored) {}

            List<String> rewards = new ArrayList<>();

            if (isMilestone && rewards.isEmpty()) {
                rewards.add("Desbloqueo de Nueva Zona");
                rewards.add("+5% Suerte Base");
            }

            lore.add(Component.empty());

            if (unlocked) {
                lore.add(parse("&8&o✔ Desbloqueado"));
            } else if (isNext) {
                lore.add(parse("&e&o► En progreso"));
                lore.add(parse("&7Experiencia: &f" + String.format("%.1f", currentExp) + "&7/&f" + String.format("%.1f", expRequired) + " XP"));
            } else {
                lore.add(parse("&8&o✘ Bloqueado"));
                lore.add(parse("&7Coste: &f" + String.format("%.1f", expRequired) + " XP"));
            }

            if (!rewards.isEmpty()) {
                lore.add(Component.empty());
                lore.add(parse("&6Recompensas:"));
                for (String r : rewards) {
                    lore.add(parse(" &8• &7" + r));
                }
            }

            ItemStack icon = new ItemStack(mat);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(parse(name));
                meta.lore(lore);
                if (isNext) {
                    meta.addEnchant(Enchantment.SHARPNESS, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
                icon.setItemMeta(meta);
            }
            inv.setItem(slot, icon);
        }

        // Paginación
        if (page > 1) {
            ItemStack prevBtn = createInfoIcon(Material.ARROW, "&ePágina Anterior", "&7Volver a niveles anteriores.");
            setContextAction(prevBtn, mode, "SKILLS_" + (page - 1));
            inv.setItem(45, prevBtn);
        }

        // Calcular si existe una página siguiente sumando el tamaño del path actual
        int nextPageIndex = startIndex + activeSnakePath.length;
        if (maxLevel > nextPageIndex) {
            ItemStack nextBtn = createInfoIcon(Material.ARROW, "&eSiguiente Página", "&7Ver próximos niveles.");
            setContextAction(nextBtn, mode, "SKILLS_" + (page + 1));
            inv.setItem(53, nextBtn);
        }

        addContextualBackButton(inv, mode, "OPTIONS");
        player.openInventory(inv);
        startAnimationTask(player, inv);
    }

    private static void renderGlobalStats(Inventory inv, PlayerData data, Fortcraft.skyworld.managers.ZoneManager zm) {
        var sm = Skyworld.getInstance().getManagerHandler().getSkillManager();

        // Pescador
        long fishTotal = zm.getAllFishingDrops().stream().map(d -> d.getGroupId().toLowerCase()).distinct().count();
        long fishDiscovered = zm.getAllFishingDrops().stream().map(d -> d.getGroupId().toLowerCase()).distinct().filter(data::hasDiscovered).count();
        int fishLvl = data.getSkillLevel(PlayerMode.FISHING.name());
        int fishMax = 100; try { fishMax = sm.getMaxLevel(PlayerMode.FISHING.name()); } catch(Exception ignored){}

        inv.setItem(19, createInfoIcon(Material.FISHING_ROD, "&bPesca",
                "&7Nivel: &f" + fishLvl + "&8/&f" + fishMax,
                "&7Especies: &f" + fishDiscovered + "&8/&f" + fishTotal));

        // Minero
        long mineTotal = zm.getAllMiningBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().count();
        long mineDiscovered = zm.getAllMiningBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().filter(data::hasDiscovered).count();
        int mineLvl = data.getSkillLevel(PlayerMode.MINING.name());
        int mineMax = 100; try { mineMax = sm.getMaxLevel(PlayerMode.MINING.name()); } catch(Exception ignored){}

        inv.setItem(21, createInfoIcon(Material.DIAMOND_PICKAXE, "&6Minería",
                "&7Nivel: &f" + mineLvl + "&8/&f" + mineMax,
                "&7Fuentes: &f" + mineDiscovered + "&8/&f" + mineTotal));

        // Agricultor
        long farmTotal = zm.getAllFarmingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().count();
        long farmDiscovered = zm.getAllFarmingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().filter(data::hasDiscovered).count();
        int farmLvl = data.getSkillLevel(PlayerMode.FARMING.name());
        int farmMax = 100; try { farmMax = sm.getMaxLevel(PlayerMode.FARMING.name()); } catch(Exception ignored){}

        inv.setItem(23, createInfoIcon(Material.GOLDEN_HOE, "&aGranja",
                "&7Nivel: &f" + farmLvl + "&8/&f" + farmMax,
                "&7Cultivos: &f" + farmDiscovered + "&8/&f" + farmTotal));

        // Leñador
        long foragTotal = zm.getAllForagingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().count();
        long foragDiscovered = zm.getAllForagingBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().filter(data::hasDiscovered).count();
        int foragLvl = data.getSkillLevel(PlayerMode.FORAGING.name());
        int foragMax = 100; try { foragMax = sm.getMaxLevel(PlayerMode.FORAGING.name()); } catch(Exception ignored){}

        inv.setItem(25, createInfoIcon(Material.IRON_AXE, "&2Foraging",
                "&7Nivel: &f" + foragLvl + "&8/&f" + foragMax,
                "&7Recursos: &f" + foragDiscovered + "&8/&f" + foragTotal));

        // Arqueólogo
        long excavTotal = zm.getAllExcavationBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().count();
        long excavDiscovered = zm.getAllExcavationBiomes().stream().flatMap(b -> b.getUniqueSourceIds().stream()).distinct().filter(data::hasDiscovered).count();
        int excavLvl = data.getSkillLevel(PlayerMode.EXCAVATION.name());
        int excavMax = 100; try { excavMax = sm.getMaxLevel(PlayerMode.EXCAVATION.name()); } catch(Exception ignored){}

        inv.setItem(31, createInfoIcon(Material.BRUSH, "&eArqueología",
                "&7Nivel: &f" + excavLvl + "&8/&f" + excavMax,
                "&7Artefactos: &f" + excavDiscovered + "&8/&f" + excavTotal));

        // Asignar los modos correspondientes
        setContextAction(inv.getItem(19), PlayerMode.FISHING, "OPTIONS");
        setContextAction(inv.getItem(21), PlayerMode.MINING, "OPTIONS");
        setContextAction(inv.getItem(23), PlayerMode.FARMING, "OPTIONS");
        setContextAction(inv.getItem(25), PlayerMode.FORAGING, "OPTIONS");
        setContextAction(inv.getItem(31), PlayerMode.EXCAVATION, "OPTIONS");
    }

    // --- MÉTODOS DE RENDERIZADO DE BIOMAS (Excavation, Mining, Farm, Foraging, Fishing) OMITIDOS PARA BREVEDAD ---
    // (Son exactamente idénticos a los de la versión anterior que te pasé)

    private static void openExcavationBiomeView(Player player, ExcavationBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse("Yacimientos: " + biome.getDisplayName()));

        Map<Material, List<ExcavationDrop>> groupedBySource = biome.getAllDrops().stream()
                .collect(Collectors.groupingBy(ExcavationDrop::getSource));

        List<Map.Entry<Material, List<ExcavationDrop>>> sortedEntries = new ArrayList<>(groupedBySource.entrySet());
        sortedEntries.sort((e1, e2) -> compareRarities(e1.getValue().getFirst().itemId(), e2.getValue().getFirst().itemId()));

        int slot = 10;
        for (Map.Entry<Material, List<ExcavationDrop>> entry : sortedEntries) {
            List<ExcavationDrop> excavDrops = entry.getValue();
            ExcavationDrop primary = excavDrops.getFirst();
            double totalWeight = excavDrops.stream().mapToDouble(ExcavationDrop::getWeight).sum();

            var template = ItemRegistry.getDropTemplates().get(primary.itemId());
            Rarity rarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

            inv.setItem(slot++, createDiscoveryIcon(
                    data.hasDiscovered(primary.getSourceId()),
                    primary.getSource(),
                    primary.getName(),
                    rarity,
                    getLoreExcavation(excavDrops, totalWeight)
            ));
            if ((slot % 9) == 8) slot += 2;
        }
        addContextualBackButton(inv, PlayerMode.EXCAVATION, "BIOMES");
        player.openInventory(inv);
        startAnimationTask(player, inv);
    }

    private static void openMiningBiomeView(Player player, MiningBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse("Capa: " + biome.getDisplayName()));

        Map<Material, List<MiningDrop>> groupedBySource = biome.getAllDrops().stream()
                .collect(Collectors.groupingBy(MiningDrop::getSource));

        List<Map.Entry<Material, List<MiningDrop>>> sortedEntries = new ArrayList<>(groupedBySource.entrySet());
        sortedEntries.sort((e1, e2) -> compareRarities(e1.getValue().getFirst().itemId(), e2.getValue().getFirst().itemId()));

        int slot = 10;
        for (Map.Entry<Material, List<MiningDrop>> entry : sortedEntries) {
            List<MiningDrop> blockDrops = entry.getValue();
            MiningDrop primary = blockDrops.getFirst();
            double totalWeight = blockDrops.stream().mapToDouble(MiningDrop::getWeight).sum();

            var template = ItemRegistry.getDropTemplates().get(primary.itemId());
            Rarity rarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

            inv.setItem(slot++, createDiscoveryIcon(
                    data.hasDiscovered(primary.getSourceId()),
                    primary.getSource(),
                    primary.getName(),
                    rarity,
                    getLoreMining(blockDrops, totalWeight)
            ));
            if ((slot % 9) == 8) slot += 2;
        }
        addContextualBackButton(inv, PlayerMode.MINING, "BIOMES");
        player.openInventory(inv);
        startAnimationTask(player, inv);
    }

    private static void openFarmBiomeView(Player player, FarmBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse("Cultivos: " + biome.getDisplayName()));

        Map<Material, List<FarmDrop>> groupedBySource = biome.getAllDrops().stream()
                .collect(Collectors.groupingBy(FarmDrop::getSourceBlock));

        List<Map.Entry<Material, List<FarmDrop>>> sortedEntries = new ArrayList<>(groupedBySource.entrySet());
        sortedEntries.sort((e1, e2) -> compareRarities(e1.getValue().getFirst().itemId(), e2.getValue().getFirst().itemId()));

        int slot = 10;
        for (Map.Entry<Material, List<FarmDrop>> entry : sortedEntries) {
            List<FarmDrop> cropDrops = entry.getValue();
            FarmDrop primary = cropDrops.getFirst();
            double totalWeight = cropDrops.stream().mapToDouble(FarmDrop::getWeight).sum();

            var template = ItemRegistry.getDropTemplates().get(primary.itemId());
            Material displayMat = template != null ? template.material() : primary.getSourceBlock();
            Rarity rarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

            inv.setItem(slot++, createDiscoveryIcon(
                    data.hasDiscovered(primary.getSourceId()),
                    displayMat,
                    primary.getName(),
                    rarity,
                    getLoreFarm(cropDrops, totalWeight)
            ));
            if ((slot % 9) == 8) slot += 2;
        }
        addContextualBackButton(inv, PlayerMode.FARMING, "BIOMES");
        player.openInventory(inv);
        startAnimationTask(player, inv);
    }

    private static void openForagingBiomeView(Player player, ForagingBiome biome, PlayerData data) {
        Inventory inv = Bukkit.createInventory(new AnimatedHolder(), 54, parse("Árboles: " + biome.getDisplayName()));

        Map<Material, List<ForagingDrop>> groupedBySource = biome.getAllDrops().stream()
                .collect(Collectors.groupingBy(ForagingDrop::getSourceMaterial));

        List<Map.Entry<Material, List<ForagingDrop>>> sortedEntries = new ArrayList<>(groupedBySource.entrySet());
        sortedEntries.sort((e1, e2) -> compareRarities(e1.getValue().getFirst().itemId(), e2.getValue().getFirst().itemId()));

        int slot = 10;
        for (Map.Entry<Material, List<ForagingDrop>> entry : sortedEntries) {
            List<ForagingDrop> treeDrops = entry.getValue();
            ForagingDrop primary = treeDrops.getFirst();
            double totalWeight = treeDrops.stream().mapToDouble(ForagingDrop::getWeight).sum();

            var template = ItemRegistry.getDropTemplates().get(primary.itemId());
            Rarity rarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

            inv.setItem(slot++, createDiscoveryIcon(
                    data.hasDiscovered(primary.getSourceId()),
                    primary.getSourceMaterial(),
                    primary.getName(),
                    rarity,
                    getLoreForaging(treeDrops, totalWeight)
            ));
            if ((slot % 9) == 8) slot += 2;
        }
        addContextualBackButton(inv, PlayerMode.FORAGING, "BIOMES");
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

            inv.setItem(slot++, createDiscoveryIcon(
                    groupDiscovered,
                    primary.getMaterial(),
                    primary.getName(),
                    primary.getSpeciesRarity(),
                    details
            ));
            if ((slot % 9) == 8) slot += 2;
        }
        addContextualBackButton(inv, PlayerMode.FISHING, "BIOMES");
        player.openInventory(inv);
        startAnimationTask(player, inv);
    }

    // --- MÉTODOS DE UTILIDAD RESTANTES ---

    private static void startAnimationTask(Player player, Inventory inv) {
        if (activeTasks.containsKey(player.getUniqueId())) activeTasks.get(player.getUniqueId()).cancel();

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
                id = b.getId(); icon = b.getGuiIcon((int) b.getUniqueSourceIds().stream().filter(data::hasDiscovered).count());
            } else if (biomeObj instanceof MiningBiome b) {
                id = b.getId(); icon = b.getGuiIcon((int) b.getUniqueSourceIds().stream().filter(data::hasDiscovered).count());
            } else if (biomeObj instanceof FarmBiome b) {
                id = b.getId(); icon = b.getGuiIcon((int) b.getUniqueSourceIds().stream().filter(data::hasDiscovered).count());
            } else if (biomeObj instanceof ForagingBiome b) {
                id = b.getId(); icon = b.getGuiIcon((int) b.getUniqueSourceIds().stream().filter(data::hasDiscovered).count());
            } else if (biomeObj instanceof ExcavationBiome b) {
                id = b.getId(); icon = b.getGuiIcon((int) b.getUniqueSourceIds().stream().filter(data::hasDiscovered).count());
            }

            if (icon != null) {
                setItemBiomeData(icon, id);
                inv.setItem(slot++, icon);
            }
            if ((slot % 9) == 8) slot += 2;
            if (slot >= 44) break;
        }
    }

    private static void addContextualBackButton(Inventory inv, PlayerMode targetMode, String targetContext) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.displayName(parse("&cVolver"));
            meta.getPersistentDataContainer().set(Skyworld.getKey("back_button"), PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(Skyworld.getKey("change_mode"), PersistentDataType.STRING, targetMode.name());
            if (targetContext != null) {
                meta.getPersistentDataContainer().set(Skyworld.getKey("logbook_context"), PersistentDataType.STRING, targetContext);
            }
            back.setItemMeta(meta);
        }
        inv.setItem(inv.getSize() - 5, back);
    }

    private static void setContextAction(ItemStack item, PlayerMode mode, String context) {
        if (item == null || item.getItemMeta() == null) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Skyworld.getKey("change_mode"), PersistentDataType.STRING, mode.name());
        if (context != null) {
            meta.getPersistentDataContainer().set(Skyworld.getKey("logbook_context"), PersistentDataType.STRING, context);
        }
        item.setItemMeta(meta);
    }

    /**
     * ¡Actualizado para usar VarArgs ("String... loreLines")!
     * Esto permite enviar múltiples líneas fácilmente y crea espacios vacíos si envías "".
     */
    private static ItemStack createInfoIcon(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(parse(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                if (line.isEmpty()) lore.add(Component.empty());
                else lore.add(parse(line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static int compareRarities(String itemId1, String itemId2) {
        var t1 = ItemRegistry.getDropTemplates().get(itemId1);
        var t2 = ItemRegistry.getDropTemplates().get(itemId2);
        Rarity r1 = t1 != null ? Rarity.fromString(t1.rarity()) : Rarity.COMUN;
        Rarity r2 = t2 != null ? Rarity.fromString(t2.rarity()) : Rarity.COMUN;
        return Integer.compare(r1.ordinal(), r2.ordinal());
    }

    private static @NotNull List<Component> getLoreExcavation(List<ExcavationDrop> blockDrops, double totalWeight) {
        List<Component> details = new ArrayList<>();
        details.add(parse("&8Arqueología"));
        details.add(Component.empty());
        details.add(parse("&7Posibles artefactos:"));
        for (ExcavationDrop d : blockDrops) addDropDetail(details, d.itemId(), d.getAmount(), d.getWeight(), totalWeight);
        return details;
    }

    private static @NotNull List<Component> getLoreMining(List<MiningDrop> blockDrops, double totalWeight) {
        List<Component> details = new ArrayList<>();
        details.add(parse("&8Minería"));
        details.add(Component.empty());
        details.add(parse("&7Posibles drops:"));
        for (MiningDrop d : blockDrops) addDropDetail(details, d.itemId(), d.getAmount(), d.getWeight(), totalWeight);
        return details;
    }

    private static @NotNull List<Component> getLoreFarm(List<FarmDrop> cropDrops, double totalWeight) {
        List<Component> details = new ArrayList<>();
        details.add(parse("&8Agricultura"));
        details.add(Component.empty());
        details.add(parse("&7Posibles drops:"));
        for (FarmDrop d : cropDrops) addDropDetail(details, d.itemId(), d.getAmount(), d.getWeight(), totalWeight);
        return details;
    }

    private static @NotNull List<Component> getLoreForaging(List<ForagingDrop> treeDrops, double totalWeight) {
        List<Component> details = new ArrayList<>();
        details.add(parse("&8Recolección"));
        details.add(Component.empty());
        details.add(parse("&7Posibles drops:"));
        for (ForagingDrop d : treeDrops) addDropDetail(details, d.itemId(), d.getAmount(), d.getWeight(), totalWeight);
        return details;
    }

    private static void addDropDetail(List<Component> details, String itemId, int amount, double weight, double totalWeight) {
        double chance = (weight / totalWeight) * 100;
        var template = ItemRegistry.getDropTemplates().get(itemId);
        String dName = template != null ? template.displayName() : itemId;
        Rarity dRarity = template != null ? Rarity.fromString(template.rarity()) : Rarity.COMUN;

        details.add(parse(" &8• ")
                .append(dRarity.format(dName))
                .append(parse(" &7x" + amount + " &f" + String.format("%.1f", chance) + "%")));
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
}