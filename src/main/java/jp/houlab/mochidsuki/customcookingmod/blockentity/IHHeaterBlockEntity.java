package jp.houlab.mochidsuki.customcookingmod.blockentity;

import jp.houlab.mochidsuki.customcookingmod.cooking.CookingAction;
import jp.houlab.mochidsuki.customcookingmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity for IH Heater
 * Supports multiple cooking actions depending on the tool used
 */
public class IHHeaterBlockEntity extends CookingProcessBlockEntity {

    public IHHeaterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.IH_HEATER_BE.get(), pos, blockState);
    }

    /**
     * Server tick method called by the block
     */
    public static void tick(Level level, BlockPos pos, BlockState state, IHHeaterBlockEntity blockEntity) {
        if (blockEntity.isCooking()) {
            blockEntity.tickCooking();
        }
    }

    @Override
    protected void onCookingComplete() {
        // Generate result name based on cooking action
        String resultName = generateResultName(getCurrentAction());

        // Calculate total nutrition and saturation
        float totalNutrition = calculateTotalNutrition();
        float totalSaturation = calculateTotalSaturation();

        // Calculate total weight (each ingredient = 100g)
        int totalWeight = calculateTotalWeight();

        // Store the cooked food
        storeFood(
            resultName,
            totalWeight,
            totalNutrition / (totalWeight / 100.0f), // Per 100g
            totalSaturation / (totalWeight / 100.0f)
        );

        // Clear ingredients
        clearIngredients();
    }

    /**
     * Generate result name based on cooking action and ingredients
     */
    private String generateResultName(String actionId) {
        CookingAction action = CookingAction.fromId(actionId);
        if (action == null) {
            return "Unknown Food";
        }

        // Get main ingredient name
        String mainIngredient = "Mixed Ingredients";
        if (!getIngredients().isEmpty()) {
            mainIngredient = getIngredients().get(0).getHoverName().getString();
            if (getIngredients().size() > 1) {
                mainIngredient = "Mixed Ingredients";
            }
        }

        return action.getResultPrefix() + " " + mainIngredient;
    }

    /**
     * Calculate total nutrition from all ingredients
     */
    private float calculateTotalNutrition() {
        float total = 0.0f;
        for (ItemStack ingredient : getIngredients()) {
            FoodProperties food = ingredient.getItem().getFoodProperties();
            if (food != null) {
                total += food.getNutrition() * ingredient.getCount();
            }
        }
        return total;
    }

    /**
     * Calculate total saturation from all ingredients
     */
    private float calculateTotalSaturation() {
        float total = 0.0f;
        for (ItemStack ingredient : getIngredients()) {
            FoodProperties food = ingredient.getItem().getFoodProperties();
            if (food != null) {
                total += food.getSaturationModifier() * ingredient.getCount();
            }
        }
        return total;
    }

    /**
     * Calculate total weight (each ingredient = 100g)
     */
    private int calculateTotalWeight() {
        return getIngredients().size() * 100;
    }
}
