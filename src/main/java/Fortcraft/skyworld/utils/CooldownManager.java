package Fortcraft.skyworld.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private static final Map<UUID, Long> interactCooldown = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TIME = 250;

    public static boolean isOnCooldown(UUID uuid) {
        long now = System.currentTimeMillis();
        return interactCooldown.containsKey(uuid) && (now - interactCooldown.get(uuid) < COOLDOWN_TIME);
    }

    public static void setCooldown(UUID uuid) {
        interactCooldown.put(uuid, System.currentTimeMillis());
    }

    public static void remove(UUID uuid) {
        interactCooldown.remove(uuid);
    }
}