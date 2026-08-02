package Fortcraft.skyworld.managers;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.listeners.NPCListener;
import Fortcraft.skyworld.npcs.NPCMenuType;
import Fortcraft.skyworld.npcs.NPCSerializable;
import Fortcraft.skyworld.npcs.SkyBlockNPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class NPCManager implements Manager, Listener {

    private final Map<String, SkyBlockNPC> npcData = new HashMap<>();
    private final List<LivingEntity> spawnedEntities = new ArrayList<>();

    private File npcFile;
    private FileConfiguration npcConfig;

    @Override
    public void load() {

        ConfigurationSerialization.registerClass(NPCSerializable.class);

        npcFile = new File(Skyworld.getInstance().getDataFolder(), "npcs.yml");
        npcConfig = YamlConfiguration.loadConfiguration(npcFile);

        Bukkit.getPluginManager().registerEvents(new NPCListener(this), Skyworld.getInstance());
        Bukkit.getPluginManager().registerEvents(this, Skyworld.getInstance());
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        loadNPCsFromConfig();
    }

    private void loadNPCsFromConfig() {
        npcData.clear();

        ConfigurationSection section = npcConfig.getConfigurationSection("NPCs");
        if (section == null) return;

        for (String id : section.getKeys(false)) {

            NPCSerializable ser = (NPCSerializable) npcConfig.get("NPCs." + id);
            if (ser == null) continue;

            if (ser.getLocation() == null) {
                Skyworld.getInstance().getLogger().warning("Mundo no encontrado para NPC: " + id);
                continue;
            }

            Location location = ser.getLocation();
            location.getChunk().load();

            List<Entity> entities = location.getNearbyEntities(2, 2, 2).stream().toList();
            LivingEntity entity = entities.isEmpty() ? null : (LivingEntity) entities.getFirst();

            if (entity == null) entity = createNPC(ser.getName(), EntityType.valueOf(ser.getType()), location, ser.getMenuType());

            SkyBlockNPC data = new SkyBlockNPC(id, ser.getName(), entity, ser.getLocation(), ser.getMenuType());
            npcData.put(id, data);
        }

        Skyworld.getInstance().getLogger().info("Cargados " + npcData.size() + " NPCs.");
    }

    @Override
    public void unload() {

    }

    /**
     * Crea un nuevo NPC, lo guarda en config y lo spawnea.
     * Úsalo en tu comando /create
     */

    public LivingEntity createNPC(String name, EntityType type, Location loc, NPCMenuType menuType) {

        LivingEntity entity = spawnEntity(name, type, loc);
        String id = entity.getUniqueId().toString();

        SkyBlockNPC npc = new SkyBlockNPC(id, name, entity, loc, menuType);
        saveNPC(npc);
        return entity;
    }

    private LivingEntity spawnEntity(String name, EntityType type, Location loc) {

        LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, type);

        entity.setAI(false);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setRemoveWhenFarAway(false);
        entity.setSilent(true);
        entity.setCollidable(true);
        entity.customName(Fortcraft.skyworld.utils.ColorUtils.format(name));
        entity.setCustomNameVisible(true);

        spawnedEntities.add(entity);

        return entity;
    }

    private void saveNPC(SkyBlockNPC skyBlockNPC) {
        npcData.put(skyBlockNPC.getId(), skyBlockNPC);
        NPCSerializable ser = new NPCSerializable(skyBlockNPC.getName(), skyBlockNPC.getId(), skyBlockNPC.getEntity().getType().toString(), skyBlockNPC.getMenuType(), skyBlockNPC.getLocation());
        npcConfig.set("NPCs."+skyBlockNPC.getId(), ser);
        saveFile();
    }

    public void deleteNPC(String id) {

        if (!npcData.containsKey(id)) return;

        List<LivingEntity> entities = npcData.get(id).getLocation().getNearbyLivingEntities(2).stream().filter(e -> e.getUniqueId().toString().equals(id)).toList();
        if(entities.isEmpty()) return;

        spawnedEntities.remove(entities.getFirst());
        entities.getFirst().remove();

        npcConfig.set("NPCs." + id, null);
        saveFile();

        npcData.remove(id);
    }

    private void saveFile() {
        try {
            npcConfig.save(npcFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SkyBlockNPC getNPCData(String id) {
        return npcData.get(id);
    }

    public @Nullable LivingEntity getEntity(String id) {
        return npcData.containsKey(id) ? npcData.get(id).getEntity() : null;
    }

    private void updateRotations() {
        if (spawnedEntities.isEmpty()) return;

        for (LivingEntity npcEntity : spawnedEntities) {
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