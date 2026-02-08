package Fortcraft.skyworld.managers.mobdisplay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class HealthBarUtil {

    public static Component createHealthBar(double health, double maxHealth) {
        int totalBars = (int) maxHealth / 2;
        double percent = health / maxHealth;
        int filledBars = (int) Math.round(totalBars * percent);

        Component bar = Component.empty();

        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                bar = bar.append(Component.text("|", NamedTextColor.GREEN));
            } else {
                bar = bar.append(Component.text("|", NamedTextColor.DARK_GRAY));
            }
        }

        int percentInt = (int) (percent * 100);
        bar = bar.append(Component.text(" " + health + "/" + maxHealth, NamedTextColor.GRAY));

        return bar;
    }
}

