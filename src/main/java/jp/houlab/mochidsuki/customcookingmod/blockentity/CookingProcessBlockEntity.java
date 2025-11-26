package jp.houlab.mochidsuki.customcookingmod.blockentity;

import jp.houlab.mochidsuki.customcookingmod.block.IHHeaterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Extended cooking block entity that manages cooking process
 * Stores ingredients, cooking state, and intermediate results
 */
public abstract class CookingProcessBlockEntity extends CookingBlockEntity {
    // Cooking process data
    private final List<ItemStack> ingredients = new ArrayList<>();
    private String currentAction = "";  // "stir_fry", "simmer", "boil", etc.
    private int cookingProgress = 0;     // Ticks
    private int cookingTime = 0;         // Required ticks
    private boolean isCooking = false;

    public CookingProcessBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * Add ingredient to cooking process
     */
    public boolean addIngredient(ItemStack ingredient) {
        if (ingredients.size() >= getMaxIngredients()) {
            return false;
        }
        ingredients.add(ingredient.copy());
        setChanged();
        return true;
    }

    /**
     * Get maximum number of ingredients
     */
    protected int getMaxIngredients() {
        return 9; // Default: 9 slots like a chest
    }

    /**
     * Get all ingredients
     */
    public List<ItemStack> getIngredients() {
        return new ArrayList<>(ingredients);
    }

    /**
     * Clear all ingredients
     */
    public void clearIngredients() {
        ingredients.clear();
        setChanged();
    }

    /**
     * Check if has any ingredients
     */
    public boolean hasIngredients() {
        return !ingredients.isEmpty();
    }

    /**
     * Start cooking with specified action
     */
    public boolean startCooking(String action, int durationTicks) {
        if (isCooking || !hasIngredients()) {
            return false;
        }
        this.currentAction = action;
        this.cookingTime = durationTicks;
        this.cookingProgress = 0;
        this.isCooking = true;
        setChanged();
        return true;
    }

    /**
     * Stop cooking process
     */
    public void stopCooking() {
        this.isCooking = false;
        this.cookingProgress = 0;
        setChanged();
    }

    /**
     * Check if this cooking block requires a heat source (IH) below
     * Override in subclasses to specify heat source requirement
     */
    protected boolean requiresHeatSource() {
        return false; // Default: independent operation
    }

    /**
     * Get heat multiplier from heat source below
     * Returns 1.0 for independent blocks, or IH heat multiplier for dependent blocks
     */
    protected float getHeatMultiplier() {
        if (!requiresHeatSource()) {
            return 1.0f; // Independent operation at normal speed
        }
        // Check for IH below
        if (level != null) {
            float multiplier = IHHeaterBlock.getHeatMultiplierFromBelow(level, worldPosition);
            return multiplier;
        }
        return 0.0f; // No heat source
    }

    /**
     * Check if heat source is available (for dependent blocks)
     */
    protected boolean hasHeatSource() {
        if (!requiresHeatSource()) {
            return true; // Independent blocks always have "heat"
        }
        return getHeatMultiplier() > 0.0f;
    }

    /**
     * Tick cooking progress
     * Call this from tick() method in subclasses
     */
    protected void tickCooking() {
        if (!isCooking) {
            return;
        }

        // Check heat source
        if (!hasHeatSource()) {
            // No heat - stop cooking
            stopCooking();
            return;
        }

        // Apply heat multiplier to cooking speed
        float heatMultiplier = getHeatMultiplier();
        cookingProgress += Math.max(1, (int) heatMultiplier);

        if (cookingProgress >= cookingTime) {
            // Cooking complete
            onCookingComplete();
        }

        setChanged();
    }

    /**
     * Called when cooking completes
     * Override in subclasses to handle completion
     */
    protected void onCookingComplete() {
        // Create intermediate or final result
        // This will be implemented by specific cooking blocks
        isCooking = false;
        cookingProgress = 0;
    }

    /**
     * Check if currently cooking
     */
    public boolean isCooking() {
        return isCooking;
    }

    /**
     * Get current cooking action
     */
    public String getCurrentAction() {
        return currentAction;
    }

    /**
     * Get cooking progress (0.0 to 1.0)
     */
    public float getCookingProgress() {
        if (cookingTime == 0) {
            return 0.0f;
        }
        return (float) cookingProgress / (float) cookingTime;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        // Save ingredients
        ListTag ingredientsList = new ListTag();
        for (ItemStack ingredient : ingredients) {
            CompoundTag itemTag = new CompoundTag();
            ingredient.save(itemTag);
            ingredientsList.add(itemTag);
        }
        tag.put("ingredients", ingredientsList);

        // Save cooking state
        tag.putString("current_action", currentAction);
        tag.putInt("cooking_progress", cookingProgress);
        tag.putInt("cooking_time", cookingTime);
        tag.putBoolean("is_cooking", isCooking);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        // Load ingredients
        ingredients.clear();
        ListTag ingredientsList = tag.getList("ingredients", Tag.TAG_COMPOUND);
        for (int i = 0; i < ingredientsList.size(); i++) {
            CompoundTag itemTag = ingredientsList.getCompound(i);
            ItemStack ingredient = ItemStack.of(itemTag);
            if (!ingredient.isEmpty()) {
                ingredients.add(ingredient);
            }
        }

        // Load cooking state
        this.currentAction = tag.getString("current_action");
        this.cookingProgress = tag.getInt("cooking_progress");
        this.cookingTime = tag.getInt("cooking_time");
        this.isCooking = tag.getBoolean("is_cooking");
    }
}
