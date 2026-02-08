package Fortcraft.skyworld.mobdisplay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;

public class MobDisplay {

    private final LivingEntity mob;
    private final TextDisplay textDisplay;
    private Location lastKnownLocation;

    public MobDisplay(LivingEntity mob) {
        this.mob = mob;

        World world = mob.getWorld();
        Location loc = mob.getLocation().clone().add(0, mob.getHeight() + 0.5, 0);

        this.textDisplay = world.spawn(loc, TextDisplay.class, display -> {
            display.setBillboard(TextDisplay.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setDefaultBackground(false);
            display.setPersistent(false);
        });

        update();
    }

    public void update() {
        if (mob.isDead()) return;

        String name = mob.getName();
        int level = getMobLevel(); // placeholder
        double health = mob.getHealth();
        double maxHealth = mob.getMaxHealth();

        Component text = Component.text()
                .append(Component.text("[Nivel " + level + "] ", NamedTextColor.DARK_GRAY))
                .append(Component.text(name, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(HealthBarUtil.createHealthBar(health, maxHealth))
                .build();

        textDisplay.text(text);
    }

    public void teleportSmooth() {
        Location currentLoc = mob.getLocation().clone().add(0, mob.getHeight() + 0.6, 0);

        // Solo actualizar si la distancia es significativa
        if (lastKnownLocation != null && lastKnownLocation.distanceSquared(currentLoc) < 0.01) return;

        textDisplay.setTeleportDuration(2); // suaviza el movimiento
        textDisplay.teleport(currentLoc);

        lastKnownLocation = currentLoc.clone();
    }

    public void remove() {
        textDisplay.remove();
    }

    private int getMobLevel() {
        // luego lo definiremos (stats, mundo, rareza, etc)
        return 1;
    }

    public LivingEntity getMob() {
        return mob;
    }
}
