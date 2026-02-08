package Fortcraft.skyworld.mobdisplay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;


import java.text.DecimalFormat;

public class HealthBarUtil {

    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("#.#");

    public static Component createHealthBar(double health, double maxHealth) {
        int totalBars = (int) (maxHealth / 2);
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

        String healthStr = ONE_DECIMAL.format(health);
        String maxHealthStr = ONE_DECIMAL.format(maxHealth);

        bar = bar.append(Component.text(
                " " + healthStr + "/" + maxHealthStr,
                NamedTextColor.GRAY
        ));

        return bar;
    }
}

