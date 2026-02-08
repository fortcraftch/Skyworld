package Fortcraft.skyworld.zones;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegionZone extends Zone {

    private final String title;
    private final String subtitle;
    private final Sound enterSound;
    private final List<PotionEffect> effects;
    private final Biome biome;

    public RegionZone(
            String id,
            World world,
            BoundingBox box,
            String title,
            String subtitle,
            Sound enterSound,
            List<PotionEffect> effects,
            Biome biome
    ) {
        super(id, world, box);
        this.title = title;
        this.subtitle = subtitle;
        this.enterSound = enterSound;
        this.effects = effects;
        this.biome = biome;
    }

    public static RegionZone fromConfig(
            String id,
            World world,
            BoundingBox box,
            ConfigurationSection config
    ) {
        if (config == null) return null;

        // Extraemos la sección específica de 'region' para los datos visuales
        ConfigurationSection regionSection = config.getConfigurationSection("region");

        // Si no existe el nodo 'region', usamos el config principal como fallback
        // para no romper la lógica de títulos/efectos si están en la raíz
        ConfigurationSection data = (regionSection != null) ? regionSection : config;

        String title = translate(data.getString("title", ""));
        String subtitle = translate(data.getString("subtitle", ""));

        Sound enterSound = null;
        if (data.contains("enter-sound")) {
            try {
                enterSound = Sound.valueOf(data.getString("enter-sound").toUpperCase());
            } catch (Exception ignored) {}
        }

        List<PotionEffect> effects = new ArrayList<>();
        ConfigurationSection effSec = data.getConfigurationSection("effects");
        if (effSec != null) {
            for (String effectName : effSec.getKeys(false)) {
                ConfigurationSection e = effSec.getConfigurationSection(effectName);
                PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
                if (type != null) {
                    int amplifier = e.getInt("amplifier", 0);
                    effects.add(new PotionEffect(type, 100, amplifier));
                }
            }
        }

        Biome biome = null;
        if (data.contains("biome")) {
            try {
                biome = Biome.valueOf(data.getString("biome").toUpperCase());
                applyBiomeToArea(world, box, biome);
            } catch (Exception ignored) {}
        }

        // Creamos la instancia
        RegionZone zone = new RegionZone(id, world, box, title, subtitle, enterSound, effects, biome);

        // --- NUEVA LÓGICA DE SPAWN POINT ---
        if (config.contains("spawn-point")) {
            ConfigurationSection sp = config.getConfigurationSection("spawn-point");
            if (sp != null) {
                Location spawnLoc = new Location(
                        world,
                        sp.getDouble("x"),
                        sp.getDouble("y"),
                        sp.getDouble("z")
                );
                // Aprovechamos el setter de la clase abstracta Zone
                zone.setSpawnPoint(spawnLoc);
            }
        }

        return zone;
    }

    public void onEnter(Player player) {

        if (enterSound != null) {
            player.playSound(player.getLocation(), enterSound, 1f, 1f);
        }

        player.sendTitle(
                title,
                subtitle,
                10, 40, 10
        );

        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
    }

    public void onExit(Player player) {
        // Quitar efectos
        for (PotionEffect effect : effects) {
            player.removePotionEffect(effect.getType());
        }
    }

    public static void applyBiomeToArea(World world, BoundingBox box, Biome biome) {
        int minX = (int) Math.floor(box.getMinX());
        int maxX = (int) Math.ceil(box.getMaxX());
        int minY = (int) Math.floor(box.getMinY());
        int maxY = (int) Math.ceil(box.getMaxY());
        int minZ = (int) Math.floor(box.getMinZ());
        int maxZ = (int) Math.ceil(box.getMaxZ());

        // Conjunto para almacenar chunks únicos que debemos refrescar
        Set<Chunk> chunksToRefresh = new HashSet<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    world.setBiome(x, y, z, biome);
                }
                // Agregamos el chunk correspondiente al set
                chunksToRefresh.add(world.getChunkAt(x >> 4, z >> 4));
            }
        }

        // Refrescamos cada chunk solo 1 vez
        for (Chunk chunk : chunksToRefresh) {
            chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
        }
    }

    // Metodo de utilidad para traducir colores
    private static String translate(String input) {
        return input.replace('&', '§');
    }

    @Override
    public void tick() {

    }
}

