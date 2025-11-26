package jp.houlab.mochidsuki.customcookingmod.blockentity;

import jp.houlab.mochidsuki.customcookingmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity for IH Heater
 * Acts as a heat source for cooking blocks placed above it
 */
public class IHHeaterBlockEntity extends BlockEntity {

    public enum HeatLevel {
        OFF(0, "Off", 0.0f),
        LOW(1, "Low", 0.5f),
        MEDIUM(2, "Medium", 1.0f),
        HIGH(3, "High", 1.5f);

        private final int id;
        private final String displayName;
        private final float speedMultiplier;

        HeatLevel(int id, String displayName, float speedMultiplier) {
            this.id = id;
            this.displayName = displayName;
            this.speedMultiplier = speedMultiplier;
        }

        public int getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public float getSpeedMultiplier() {
            return speedMultiplier;
        }

        public HeatLevel next() {
            int nextId = (id + 1) % values().length;
            return values()[nextId];
        }

        public static HeatLevel fromId(int id) {
            for (HeatLevel level : values()) {
                if (level.id == id) {
                    return level;
                }
            }
            return OFF;
        }
    }

    private HeatLevel heatLevel = HeatLevel.OFF;

    public IHHeaterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.IH_HEATER_BE.get(), pos, blockState);
    }

    /**
     * Get current heat level
     */
    public HeatLevel getHeatLevel() {
        return heatLevel;
    }

    /**
     * Set heat level
     */
    public void setHeatLevel(HeatLevel level) {
        this.heatLevel = level;
        setChanged();
    }

    /**
     * Cycle to next heat level
     */
    public void cycleHeatLevel() {
        this.heatLevel = this.heatLevel.next();
        setChanged();
    }

    /**
     * Check if this IH is providing heat
     */
    public boolean isHeating() {
        return heatLevel != HeatLevel.OFF;
    }

    /**
     * Get speed multiplier for cooking
     */
    public float getSpeedMultiplier() {
        return heatLevel.getSpeedMultiplier();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("HeatLevel", heatLevel.getId());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.heatLevel = HeatLevel.fromId(tag.getInt("HeatLevel"));
    }
}
