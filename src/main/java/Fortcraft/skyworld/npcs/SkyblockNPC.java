package Fortcraft.skyworld.npcs;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class SkyblockNPC {
    private final String id;
    private final String name;
    private final EntityType type;
    private final Location location;
    private final String menuType; // "SHOP_BLOCKS", "SHOP_FARMING", "QUEST", etc.

    public SkyblockNPC(String id, String name, EntityType type, Location location, String menuType) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.location = location;
        this.menuType = menuType;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public EntityType getType() { return type; }
    public Location getLocation() { return location; }
    public String getMenuType() { return menuType; }
}