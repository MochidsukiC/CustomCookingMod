package jp.houlab.mochidsuki.customcookingmod.block;

import jp.houlab.mochidsuki.customcookingmod.blockentity.OvenBlockEntity;
import jp.houlab.mochidsuki.customcookingmod.cooking.CookingAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Oven Block
 * Players can add ingredients and bake them
 */
public class OvenBlock extends BaseEntityBlock {

    public OvenBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OvenBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof OvenBlockEntity oven) {
                OvenBlockEntity.tick(level1, pos, state1, oven);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof OvenBlockEntity oven) {
                ItemStack heldItem = player.getItemInHand(hand);

                // If empty hand and not cooking, start baking
                if (heldItem.isEmpty() && !oven.isCooking()) {
                    return handleBakeStart(oven, player, level, pos);
                }
                // If holding ingredient, add it
                else if (isIngredient(heldItem)) {
                    return handleIngredientAddition(oven, heldItem, player, level, pos);
                }
                // Empty hand while cooking - show status
                else if (heldItem.isEmpty()) {
                    return handleStatusCheck(oven, player);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Handle starting bake process
     */
    private InteractionResult handleBakeStart(OvenBlockEntity oven, Player player, Level level, BlockPos pos) {
        if (!oven.hasIngredients()) {
            player.displayClientMessage(
                    Component.literal("§cNo ingredients in oven!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (oven.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§cOven already has cooked food! Remove it first."),
                    true
            );
            return InteractionResult.FAIL;
        }

        // Start baking
        boolean started = oven.startCooking(
                CookingAction.BAKE.getId(),
                CookingAction.BAKE.getDefaultDurationTicks()
        );

        if (started) {
            level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.5F, 1.0F);
            player.displayClientMessage(
                    Component.literal("§aStarted baking!"),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    /**
     * Handle adding ingredient to oven
     */
    private InteractionResult handleIngredientAddition(OvenBlockEntity oven, ItemStack ingredient, Player player, Level level, BlockPos pos) {
        if (oven.isCooking()) {
            player.displayClientMessage(
                    Component.literal("§cCannot add ingredients while baking!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (oven.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§cOven already has cooked food! Remove it first."),
                    true
            );
            return InteractionResult.FAIL;
        }

        boolean added = oven.addIngredient(ingredient);
        if (added) {
            ingredient.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
            player.displayClientMessage(
                    Component.literal("§aAdded " + ingredient.getHoverName().getString() + " to oven"),
                    true
            );
            return InteractionResult.SUCCESS;
        } else {
            player.displayClientMessage(
                    Component.literal("§cOven is full!"),
                    true
            );
            return InteractionResult.FAIL;
        }
    }

    /**
     * Handle status check
     */
    private InteractionResult handleStatusCheck(OvenBlockEntity oven, Player player) {
        if (oven.isCooking()) {
            float progress = oven.getCookingProgress() * 100;
            player.displayClientMessage(
                    Component.literal(String.format("§eBaking: %.0f%%", progress)),
                    true
            );
        } else if (oven.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§aFood ready: " + oven.getStoredFoodType()),
                    true
            );
        } else if (oven.hasIngredients()) {
            player.displayClientMessage(
                    Component.literal("§eIngredients: " + oven.getIngredients().size() + " items. Right-click with empty hand to start baking."),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal("§7Empty oven"),
                    true
            );
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Check if item can be used as ingredient
     */
    private boolean isIngredient(ItemStack stack) {
        return stack.isEdible() ||
               stack.getItem().toString().contains("chopped") ||
               stack.getOrCreateTag().getBoolean("chopped");
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof OvenBlockEntity oven) {
                // Drop all ingredients
                for (ItemStack ingredient : oven.getIngredients()) {
                    popResource(level, pos, ingredient);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
