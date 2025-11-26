package jp.houlab.mochidsuki.customcookingmod.blockentity;

import jp.houlab.mochidsuki.customcookingmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity for Cutting Board
 * Stores an ingredient that can be chopped with a knife
 */
public class CuttingBoardBlockEntity extends BlockEntity {
    private ItemStack ingredient = ItemStack.EMPTY;
    private boolean isChopped = false;

    public CuttingBoardBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CUTTING_BOARD_BE.get(), pos, blockState);
    }

    /**
     * Check if there's an ingredient on the cutting board
     */
    public boolean hasIngredient() {
        return !ingredient.isEmpty();
    }

    /**
     * Get the ingredient on the cutting board
     */
    public ItemStack getIngredient() {
        return ingredient;
    }

    /**
     * Place an ingredient on the cutting board
     */
    public void placeIngredient(ItemStack stack) {
        this.ingredient = stack.copy();
        this.isChopped = false;
        setChanged();
    }

    /**
     * Remove and return the ingredient from the cutting board
     */
    public ItemStack removeIngredient() {
        ItemStack result = ingredient.copy();
        this.ingredient = ItemStack.EMPTY;
        this.isChopped = false;
        setChanged();
        return result;
    }

    /**
     * Check if the ingredient is chopped
     */
    public boolean isChopped() {
        return isChopped;
    }

    /**
     * Mark the ingredient as chopped
     */
    public void chopIngredient() {
        this.isChopped = true;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("ingredient", ingredient.save(new CompoundTag()));
        tag.putBoolean("is_chopped", isChopped);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.ingredient = ItemStack.of(tag.getCompound("ingredient"));
        this.isChopped = tag.getBoolean("is_chopped");
    }
}
