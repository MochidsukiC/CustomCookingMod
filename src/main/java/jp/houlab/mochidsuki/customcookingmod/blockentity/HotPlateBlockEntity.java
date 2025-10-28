package jp.houlab.mochidsuki.customcookingmod.blockentity;

import jp.houlab.mochidsuki.customcookingmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity for Hot Plate
 * Supports stir-frying and frying actions
 */
public class HotPlateBlockEntity extends CookingProcessBlockEntity {

    public HotPlateBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.HOT_PLATE_BE.get(), pos, blockState);
    }

    /**
     * Tick method called every game tick
     */
    public static void tick(Level level, BlockPos pos, BlockState state, HotPlateBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // Progress cooking if active
        if (blockEntity.isCooking()) {
            blockEntity.tickCooking();
        }
    }

    @Override
    protected void onCookingComplete() {
        super.onCookingComplete();

        // Generate result item based on ingredients and action
        String action = getCurrentAction();
        String resultName = generateResultName(action);

        // Calculate nutrition based on ingredients
        float totalNutrition = calculateTotalNutrition();
        float totalSaturation = calculateTotalSaturation();
        int totalWeight = calculateTotalWeight();

        // Store the result
        storeFood(resultName, totalWeight, totalNutrition, totalSaturation);

        // Clear ingredients after cooking
        clearIngredients();
    }

    /**
     * Generate result name based on action and ingredients
     */
    private String generateResultName(String action) {
        if (getIngredients().isEmpty()) {
            return "Unknown Dish";
        }

        // Get main ingredient name
        String mainIngredient = getIngredients().get(0).getHoverName().getString();

        // Add action prefix
        switch (action) {
            case "stir_fry":
                return "Stir-fried " + mainIngredient;
            case "fry":
                return "Fried " + mainIngredient;
            default:
                return "Cooked " + mainIngredient;
        }
    }

    /**
     * Calculate total nutrition from ingredients
     */
    private float calculateTotalNutrition() {
        float total = 0.0f;
        for (var ingredient : getIngredients()) {
            if (ingredient.isEdible() && ingredient.getFoodProperties(null) != null) {
                total += ingredient.getFoodProperties(null).getNutrition();
            }
        }
        return total;
    }

    /**
     * Calculate total saturation from ingredients
     */
    private float calculateTotalSaturation() {
        float total = 0.0f;
        for (var ingredient : getIngredients()) {
            if (ingredient.isEdible() && ingredient.getFoodProperties(null) != null) {
                total += ingredient.getFoodProperties(null).getSaturationModifier();
            }
        }
        return total;
    }

    /**
     * Calculate total weight (assume 100g per item for now)
     */
    private int calculateTotalWeight() {
        return getIngredients().size() * 100;
    }
}
