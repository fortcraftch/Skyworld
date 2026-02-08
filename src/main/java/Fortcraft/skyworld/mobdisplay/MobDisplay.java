package Fortcraft.skyworld.managers.mobdisplay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;

public class MobDisplay {

    private final LivingEntity mob;
    private final TextDisplay textDisplay;

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

    public void teleport() {
        Location loc = mob.getLocation().clone().add(0, mob.getHeight() + 0.5, 0);
        textDisplay.teleport(loc);
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
