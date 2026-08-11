package Fortcraft.skyworld.excavation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class ExcavationNode {

    private final Location location;
    private final Material originalMaterial; // Guardamos el material original del bloque

    private ItemDisplay itemDisplay;         // El visual del ítem que va asomando
    private BlockFace targetFace;            // La cara vectorial seleccionada
    private ItemStack rewardItem;            // El ítem asignado a este nodo
    private ExcavationDrop pendingDrop;      // Objeto drop pendiente a entregar

    private int progress = 0;
    private final int maxProgress = 100;

    public ExcavationNode(Location location, Material originalMaterial) {
        this.location = location;
        this.originalMaterial = originalMaterial;
    }

    /**
     * Inicializa el ítem visual la primera vez que el jugador empieza a cepillar.
     */
    public void initRewardVisual(BlockFace targetFace, ItemStack rewardItem) {
        if (this.itemDisplay != null) return;

        this.targetFace = targetFace;
        this.rewardItem = rewardItem;

        // Spawneamos el ItemDisplay centrado en el bloque
        Location spawnLoc = location.clone().add(0.5, 0.5, 0.5);
        this.itemDisplay = (ItemDisplay) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);
        this.itemDisplay.setItemStack(rewardItem);

        // Forzamos el modo FIXED para que siempre esté "de pie" (vertical y plano)
        this.itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);

        // Desactivamos gravedad
        this.itemDisplay.setPersistent(false);

        // Forzamos nivel de luz máximo (15 luz de bloque, 15 luz de cielo)
        // para ignorar la sombra del bloque sólido
        this.itemDisplay.setBrightness(new Display.Brightness(15, 15));

        // Render inicial
        updateItemTransformation();
    }

    public void addProgress(int amount) {
        this.progress = Math.min(maxProgress, this.progress + amount);
        updateItemTransformation();
    }

    /**
     * Actualiza la posición y escala del ItemDisplay según el % de progreso.
     */
    private void updateItemTransformation() {
        if (itemDisplay == null || targetFace == null) return;

        float percentage = (float) progress / maxProgress; // De 0.0 a 1.0

        // Direcciones vectoriales de la cara
        Vector3f dir = new Vector3f(
                targetFace.getModX(),
                targetFace.getModY(),
                targetFace.getModZ()
        );

        // Distancia de salida del ítem
        float offsetDistance = -0.1f + (percentage * 0.80f);
        Vector3f translation = new Vector3f(dir).mul(offsetDistance);

        float scaleVal = 0.55f;
        Vector3f scale = new Vector3f(scaleVal, scaleVal, scaleVal);

        // Calculamos la rotación para que siempre esté de pie y mire hacia la cara de salida
        float rotY = 0f;
        switch (targetFace) {
            case NORTH -> rotY = (float) -Math.PI / 2f;    // 180 + 90 = 270 (equivale a -90 grados)
            case EAST  -> rotY = 0f;                       // -90 + 90 = 0 grados
            case WEST  -> rotY = (float) Math.PI;          // 90 + 90 = 180 grados
            case SOUTH -> rotY = (float) Math.PI / 2f;     // 0 + 90 = 90 grados
            default    -> rotY = (float) Math.PI / 2f;     // UP y DOWN +90 grados
        }

        // Aplicamos la rotación estrictamente sobre el eje Y (0, 1, 0)
        AxisAngle4f leftRot = new AxisAngle4f(rotY, 0f, 1f, 0f);

        // Aplicamos la transformación final
        Transformation transformation = new Transformation(
                translation,
                leftRot, // Usamos nuestra rotación calculada en lugar de todo 0
                scale,
                new AxisAngle4f(0, 0, 0, 0)
        );

        itemDisplay.setTransformation(transformation);
    }

    public void cleanup() {
        if (itemDisplay != null && !itemDisplay.isDead()) {
            itemDisplay.remove();
        }
    }

    // Getters y Setters
    public Location getLocation() { return location; }
    public Material getOriginalMaterial() { return originalMaterial; }
    public ItemDisplay getItemDisplay() { return itemDisplay; }
    public ItemStack getRewardItem() { return rewardItem; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }

    public ExcavationDrop getPendingDrop() { return pendingDrop; }
    public void setPendingDrop(ExcavationDrop pendingDrop) { this.pendingDrop = pendingDrop; }
}