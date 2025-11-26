package jp.houlab.mochidsuki.customcookingmod.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base class for cooking block entities that can hold cooked food
 * Stores food data including weight, type, and nutrition values
 */
public abstract class CookingBlockEntity extends BlockEntity {
    // Food storage data
    private String storedFoodType = "";
    private int storedWeightGrams = 0;
    private float nutritionPer100g = 0.0f;
    private float saturationPer100g = 0.0f;

    public CookingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * Check if this cooking block has food stored
     */
    public boolean hasFood() {
        return storedWeightGrams > 0 && !storedFoodType.isEmpty();
    }

    /**
     * Get the current food type stored
     */
    public String getStoredFoodType() {
        return storedFoodType;
    }

    /**
     * Get the current weight of stored food (in grams)
     */
    public int getStoredWeightGrams() {
        return storedWeightGrams;
    }

    /**
     * Get nutrition per 100g
     */
    public float getNutritionPer100g() {
        return nutritionPer100g;
    }

    /**
     * Get saturation per 100g
     */
    public float getSaturationPer100g() {
        return saturationPer100g;
    }

    /**
     * Store cooked food in this block
     */
    public void storeFood(String foodType, int weightGrams, float nutritionPer100g, float saturationPer100g) {
        this.storedFoodType = foodType;
        this.storedWeightGrams = weightGrams;
        this.nutritionPer100g = nutritionPer100g;
        this.saturationPer100g = saturationPer100g;
        setChanged();
    }

    /**
     * Take food from this cooking block
     * Returns the amount actually taken (may be less than requested)
     */
    public int takeFood(int requestedGrams) {
        if (!hasFood()) {
            return 0;
        }

        int actualAmount = Math.min(requestedGrams, storedWeightGrams);
        storedWeightGrams -= actualAmount;

        if (storedWeightGrams <= 0) {
            // Clear all food data when empty
            clearFood();
        } else {
            setChanged();
        }

        return actualAmount;
    }

    /**
     * Clear all stored food
     */
    public void clearFood() {
        this.storedFoodType = "";
        this.storedWeightGrams = 0;
        this.nutritionPer100g = 0.0f;
        this.saturationPer100g = 0.0f;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putString("stored_food_type", storedFoodType);
        tag.putInt("stored_weight_grams", storedWeightGrams);
        tag.putFloat("nutrition_per_100g", nutritionPer100g);
        tag.putFloat("saturation_per_100g", saturationPer100g);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.storedFoodType = tag.getString("stored_food_type");
        this.storedWeightGrams = tag.getInt("stored_weight_grams");
        this.nutritionPer100g = tag.getFloat("nutrition_per_100g");
        this.saturationPer100g = tag.getFloat("saturation_per_100g");
    }
}
