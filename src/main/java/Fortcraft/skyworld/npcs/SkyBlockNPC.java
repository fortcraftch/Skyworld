package Fortcraft.skyworld.npcs;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;


public class SkyBlockNPC {
    
    private String id;
    private String name;
    private LivingEntity entity;
    private Location location;
    private NPCMenuType menuType;
    
    public SkyBlockNPC(String id, String name, LivingEntity entity, Location location, NPCMenuType menuType) {
        
        this.id = id;
        this.name = name;
        this.entity = entity;
        this.location = location;
        this.menuType = menuType;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public void setEntity(LivingEntity entity) {
        this.entity = entity;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public NPCMenuType getMenuType() {
        return menuType;
    }

    public void setMenuType(NPCMenuType menuType) {
        this.menuType = menuType;
    }
}