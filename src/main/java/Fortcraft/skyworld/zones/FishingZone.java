package Fortcraft.skyworld.zones;

import Fortcraft.skyworld.Skyworld;
import Fortcraft.skyworld.fishing.FishingAbundance;
import Fortcraft.skyworld.fishing.FishingBiome;
import Fortcraft.skyworld.fishing.FishingDrop;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;

import java.util.*;

public class FishingZone extends Zone {

    private TextDisplay infoDisplay;
    private FishingBiome biome; // REFERENCIA AL BIOMA
    private FishingAbundance abundance;
    private final List<ItemDisplay> decorativeFish = new ArrayList<>();

    public FishingZone(
            String id,
            World world,
            BoundingBox box,
            ConfigurationSection config
    ) {
        super(id, world, box);
        this.abundance = FishingAbundance.rollInitial();
        String biomeId = config.getString("biome", "default");
        this.biome = Skyworld.getInstance().getManagerHandler().getZoneManager().getFishingBiome(biomeId);

        if (this.biome == null) {
            Skyworld.getInstance().getLogger().severe("Biome not found for zone " + id + ": " + biomeId);
        }
    }

    public boolean canFish() {
        return abundance != FishingAbundance.VACIO;
    }

    public void processCatch() {
        // 30% de bajar de estado
        double degradationChance = 0.30;
        if (Math.random() < degradationChance) {
            this.abundance = abundance.getNextLower();
            updateDisplay(); // Actualizar el holograma al instante
        }
    }

    public void resetAbundance() {
        this.abundance = FishingAbundance.rollInitial();
        updateDisplay();
    }

    public void updateDisplay() {
        if (infoDisplay == null || !infoDisplay.isValid()) return;

        String text = "§b§l⛵ ZONA DE PESCA ⛵\n" +
                "§7Estado: " + abundance.getDisplayName() + "\n" +
                "§7Items: §e" + (biome != null ? biome.getDrops().size() : 0);

        infoDisplay.text(LegacyComponentSerializer.legacySection().deserialize(text));
    }

    public void createDisplay() {
        if (infoDisplay != null && !infoDisplay.isDead()) return;

        // Calculamos el centro horizontal (X, Z) y el techo de la zona (MaxY)
        double centerX = box.getMinX() + (box.getWidthX() / 2);
        double centerZ = box.getMinZ() + (box.getWidthZ() / 2);
        double spawnY = box.getMaxY() + 1.0; // 2 bloques por encima del límite superior

        Location loc = new Location(world, centerX, spawnY, centerZ);

        this.infoDisplay = world.spawn(loc, TextDisplay.class, display -> {
            display.setBillboard(TextDisplay.Billboard.CENTER);
            display.setPersistent(false); // Importante: se recrea al cargar el server
            display.setDefaultBackground(true);
            String text = "§b§l⛵ ZONA DE PESCA ⛵\n" +
                    "§7Estado: " + abundance.getDisplayName() + "\n" +
                    "§7Items: §e" + (biome != null ? biome.getDrops().size() : 0);

            display.text(LegacyComponentSerializer.legacySection().deserialize(text));
        });
    }

    public void removeDisplay() {
        if (infoDisplay != null) {
            infoDisplay.remove();
        }
    }

    public List<FishingDrop> getDrops() {
        return biome != null ? biome.getDrops() : Collections.emptyList();
    }

    public int rollRarity() {
        if (biome == null) return 1;
        return biome.rollRarity(); // Delega al bioma
    }

    public FishingDrop rollDrop(int rarity) {
        if (biome == null) return null;
        return biome.rollDrop(rarity); // El bioma decide qué dar
    }

    public void spawnFish() {

        double centerX = box.getMinX() + box.getWidthX() / 2.0;
        double centerZ = box.getMinZ() + box.getWidthZ() / 2.0;
        double y = box.getMaxY() - 1.1;

        Location center = new Location(world, centerX, y, centerZ);

        for (int i = 0; i < 2; i++) {
            ItemDisplay fish = world.spawn(center, ItemDisplay.class, display -> {
                display.setItemStack(new ItemStack(Material.COD));
                display.setPersistent(false);
                display.setBillboard(Display.Billboard.FIXED);

                Transformation transformation = display.getTransformation();
                transformation.getScale().set(0.6f, 0.6f, 0.6f);
                transformation.getTranslation().set(0, 0, 0); // MUY IMPORTANTE
                display.setTransformation(transformation);

                display.setInterpolationDuration(100);
                display.setInterpolationDelay(0);
            });

            decorativeFish.add(fish);
        }
    }

    public void updateFishPositions(double baseAngle) {
        for (int i = 0; i < decorativeFish.size(); i++) {
            ItemDisplay fish = decorativeFish.get(i);
            if (fish == null || !fish.isValid()) continue;

            double angle = baseAngle + (i * Math.PI);

            float offX = (float) Math.cos(angle);
            float offZ = (float) Math.sin(angle);

            Transformation trans = fish.getTransformation();

            trans.getTranslation().set(offX, -0.6, offZ);

            float yawRad = (float) (angle + Math.PI / 2);
            Quaternionf rotYaw = new Quaternionf().rotateY(-yawRad);
            Quaternionf rotX = new Quaternionf().rotateZ((float) Math.toRadians(45f));
            Quaternionf finalRot = new Quaternionf(rotYaw).mul(rotX);

            trans.getLeftRotation().set(finalRot);

            fish.setTransformation(trans);
        }
    }

    @Override
    public void tick() {

        double y = box.getMaxY()-1.1;
        world.spawnParticle(Particle.END_ROD, box.getMinX(), y, box.getMinZ(), 1, 0.1, 0, 0.1, 0);
        world.spawnParticle(Particle.END_ROD, box.getMaxX(), y, box.getMinZ(), 1, 0.1, 0, 0.1, 0);
        world.spawnParticle(Particle.END_ROD, box.getMinX(), y, box.getMaxZ(), 1, 0.1, 0, 0.1, 0);
        world.spawnParticle(Particle.END_ROD, box.getMaxX(), y, box.getMaxZ(), 1, 0.1, 0, 0.1, 0);
    }
}

