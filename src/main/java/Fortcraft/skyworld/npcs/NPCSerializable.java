package Fortcraft.skyworld.npcs;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class NPCSerializable implements ConfigurationSerializable {

    private final String name;
    private final String id;
    private final String type;
    private final NPCMenuType menuType;
    private final Location location;

    public NPCSerializable(String name, String id, String type, NPCMenuType menuType, Location location) {

        this.name = name;
        this.id = id;
        this.type = type;
        this.menuType = menuType;
        this.location = location;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {

        Map<String, Object> map = new HashMap<>();

        map.put("name", name);
        map.put("id", id);
        map.put("type", type);
        map.put("menuType", menuType.toString());
        map.put("location", location);

        return map;
    }

    public static NPCSerializable deserialize(Map<String, Object> map) {

        String name = (String) map.get("name");
        String id = (String) map.get("id");
        String type = (String) map.get("type");
        NPCMenuType menuType = NPCMenuType.valueOf((String) map.get("menuType"));
        Location loc = (Location) map.get("location");


        return new NPCSerializable(name, id, type, menuType, loc);
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public NPCMenuType getMenuType() {
        return menuType;
    }

    public Location getLocation() {
        return location;
    }
}
