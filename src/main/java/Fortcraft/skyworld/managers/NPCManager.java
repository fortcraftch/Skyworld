package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.listeners.NPCListener;
import Fortcraft.skyworld.npcs.SkyblockNPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NPCManager implements Manager {

    private final Map<String, SkyblockNPC> npcData = new HashMap<>();
    private final List<Entity> spawnedEntities = new ArrayList<>();
    private final NamespacedKey npcKey = new NamespacedKey(Skyworld.getInstance(), "is_npc");
    private final NamespacedKey npcIdKey = new NamespacedKey(Skyworld.getInstance(), "npc_id");

    private File npcFile;
    private FileConfiguration npcConfig;
    private int taskID = -1;

    @Override
    public void load() {
        // Inicializar archivo
        npcFile = new File(Skyworld.getInstance().getDataFolder(), "npcs.yml");
        if (!npcFile.exists()) {
            Skyworld.getInstance().saveResource("npcs.yml", false);
        }
        npcConfig = YamlConfiguration.loadConfiguration(npcFile);

        Bukkit.getPluginManager().registerEvents(new NPCListener(this), Skyworld.getInstance());

        // Retrasamos la carga para asegurar que los mundos estén listos
        Bukkit.getScheduler().runTaskLater(Skyworld.getInstance(), () -> {
            loadFromConfig(); // <--- Cargar desde archivo
            spawnAll();       // <--- Spawnear físicos

            // Iniciar rotación
            taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Skyworld.getInstance(), this::updateRotations, 0L, 2L);
        }, 20L);
    }

    @Override
    public void unload() {
        if (taskID != -1) Bukkit.getScheduler().cancelTask(taskID);
        despawnAll();
    }

    /**
     * Carga los NPCs desde el archivo npcs.yml a la memoria.
     */
    private void loadFromConfig() {
        npcData.clear();
        ConfigurationSection section = npcConfig.getConfigurationSection("npcs");

        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String name = section.getString(id + ".name");
            String typeStr = section.getString(id + ".type");
            String menuType = section.getString(id + ".menu");

            // Cargar ubicación
            String worldName = section.getString(id + ".location.world");
            double x = section.getDouble(id + ".location.x");
            double y = section.getDouble(id + ".location.y");
            double z = section.getDouble(id + ".location.z");
            float yaw = (float) section.getDouble(id + ".location.yaw");
            float pitch = (float) section.getDouble(id + ".location.pitch");

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                Skyworld.getInstance().getLogger().warning("Mundo no encontrado para NPC: " + id);
                continue;
            }

            Location loc = new Location(world, x, y, z, yaw, pitch);
            EntityType type = EntityType.valueOf(typeStr);

            SkyblockNPC npc = new SkyblockNPC(id, name, type, loc, menuType);
            npcData.put(id, npc);
        }
        Skyworld.getInstance().getLogger().info("Cargados " + npcData.size() + " NPCs.");
    }

    /**
     * Crea un nuevo NPC, lo guarda en config y lo spawnea.
     * Úsalo en tu comando /create
     */
    public void createNPC(String id, String name, EntityType type, Location loc, String menuType) {
        SkyblockNPC npc = new SkyblockNPC(id, name, type, loc, menuType);
        npcData.put(id, npc);
        saveToConfig(npc); // Guardar en archivo
        spawnEntity(npc);  // Spawnear visualmente
    }

    /**
     * Borra un NPC, lo quita del config y lo des-spawnea.
     * Úsalo en tu comando /remove
     */
    public void deleteNPC(String id) {
        if (!npcData.containsKey(id)) return;

        // 1. Quitar del config
        npcConfig.set("npcs." + id, null);
        saveFile();

        // 2. Despawnear entidad física
        SkyblockNPC npc = npcData.get(id);
        spawnedEntities.removeIf(entity -> {
            if (entity.getPersistentDataContainer().has(npcIdKey, PersistentDataType.STRING)) {
                String storedId = entity.getPersistentDataContainer().get(npcIdKey, PersistentDataType.STRING);
                if (storedId != null && storedId.equals(id)) {
                    entity.remove();
                    return true;
                }
            }
            return false;
        });

        // 3. Quitar de memoria
        npcData.remove(id);
    }

    private void saveToConfig(SkyblockNPC npc) {
        String path = "npcs." + npc.getId();
        npcConfig.set(path + ".name", npc.getName());
        npcConfig.set(path + ".type", npc.getType().name());
        npcConfig.set(path + ".menu", npc.getMenuType());

        Location loc = npc.getLocation();
        npcConfig.set(path + ".location.world", loc.getWorld().getName());
        npcConfig.set(path + ".location.x", loc.getX());
        npcConfig.set(path + ".location.y", loc.getY());
        npcConfig.set(path + ".location.z", loc.getZ());
        npcConfig.set(path + ".location.yaw", loc.getYaw());
        npcConfig.set(path + ".location.pitch", loc.getPitch());

        saveFile();
    }

    private void saveFile() {
        try {
            npcConfig.save(npcFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void spawnAll() {
        despawnAll();
        npcData.values().forEach(this::spawnEntity);
    }

    public void despawnAll() {
        for (Entity ent : spawnedEntities) {
            if (ent != null) ent.remove();
        }
        spawnedEntities.clear();
    }

    public SkyblockNPC getNPCById(String id) {
        return npcData.get(id);
    }

    private void spawnEntity(SkyblockNPC npc) {
        Location loc = npc.getLocation();
        if (loc.getWorld() == null) return;

        // --- IMPORTANTE: Limpieza preventiva ---
        // Borramos entidades viejas en el mismo sitio para evitar duplicados al recargar
        loc.getWorld().getNearbyEntities(loc, 1, 1, 1).forEach(e -> {
            if (e.getPersistentDataContainer().has(npcKey, PersistentDataType.BYTE)) {
                e.remove();
            }
        });

        LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, npc.getType());

        entity.setAI(false);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setRemoveWhenFarAway(false);
        entity.setSilent(true);
        entity.setCollidable(true);
        entity.customName(Fortcraft.skyworld.utils.ColorUtils.format(npc.getName()));
        entity.setCustomNameVisible(true);

        // Evitamos que se guarde en el archivo del mundo (Chunk)
        // ya que nosotros lo gestionamos via npcs.yml
        entity.setPersistent(false);

        // Metadatos para lógica
        entity.setMetadata("NPC_DATA_ID", new org.bukkit.metadata.FixedMetadataValue(Skyworld.getInstance(), npc.getId()));

        // Datos persistentes para identificación
        entity.getPersistentDataContainer().set(npcKey, PersistentDataType.BYTE, (byte) 1);
        entity.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, npc.getId());

        spawnedEntities.add(entity);
    }

    private void updateRotations() {
        if (spawnedEntities.isEmpty()) return;

        for (Entity npcEntity : spawnedEntities) {
            if (npcEntity == null || !npcEntity.isValid()) continue;

            Player closest = null;
            double closestDist = 48.0;

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getWorld().equals(npcEntity.getWorld())) continue;
                double dist = p.getLocation().distanceSquared(npcEntity.getLocation());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = p;
                }
            }

            if (closest != null) {
                lookAt(npcEntity, closest.getEyeLocation());
            }
        }
    }

    private void lookAt(Entity entity, Location target) {
        Location npcLoc = entity.getLocation();
        Vector direction = target.toVector().subtract(npcLoc.toVector()).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        float pitch = (float) Math.toDegrees(-Math.asin(direction.getY()));

        entity.setRotation(yaw, pitch);
    }
}