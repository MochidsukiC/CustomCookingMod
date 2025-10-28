package jp.houlab.mochidsuki.customcookingmod.block;

import jp.houlab.mochidsuki.customcookingmod.blockentity.FryingPanBlockEntity;
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
 * Frying Pan Block
 * Players can add ingredients and fry them
 * Requires IH heater below to operate
 */
public class FryingPanBlock extends BaseEntityBlock {

    public FryingPanBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FryingPanBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof FryingPanBlockEntity pan) {
                FryingPanBlockEntity.tick(level1, pos, state1, pan);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FryingPanBlockEntity pan) {
                ItemStack heldItem = player.getItemInHand(hand);

                // If holding spatula, start frying
                if (isSpatula(heldItem)) {
                    return handleSpatulaInteraction(pan, player, level, pos);
                }
                // If holding ingredient, add it
                else if (isIngredient(heldItem)) {
                    return handleIngredientAddition(pan, heldItem, player, level, pos);
                }
                // Empty hand - show status
                else if (heldItem.isEmpty()) {
                    return handleStatusCheck(pan, player);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Handle spatula interaction to start frying
     */
    private InteractionResult handleSpatulaInteraction(FryingPanBlockEntity pan, Player player, Level level, BlockPos pos) {
        if (pan.isCooking()) {
            player.displayClientMessage(
                    Component.literal("§eAlready cooking..."),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (!pan.hasIngredients()) {
            player.displayClientMessage(
                    Component.literal("§cNo ingredients in frying pan!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        // Check for IH heater below
        if (!pan.hasHeatSource()) {
            player.displayClientMessage(
                    Component.literal("§cNo IH heater below! Place frying pan on an IH heater."),
                    true
            );
            return InteractionResult.FAIL;
        }

        // Start frying
        boolean started = pan.startCooking(
                CookingAction.FRY.getId(),
                CookingAction.FRY.getDefaultDurationTicks()
        );

        if (started) {
            level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.5F, 1.0F);
            player.displayClientMessage(
                    Component.literal("§aStarted frying!"),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    /**
     * Handle adding ingredient to frying pan
     */
    private InteractionResult handleIngredientAddition(FryingPanBlockEntity pan, ItemStack ingredient, Player player, Level level, BlockPos pos) {
        if (pan.isCooking()) {
            player.displayClientMessage(
                    Component.literal("§cCannot add ingredients while cooking!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (pan.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§cFrying pan already has cooked food! Remove it first."),
                    true
            );
            return InteractionResult.FAIL;
        }

        boolean added = pan.addIngredient(ingredient);
        if (added) {
            ingredient.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
            player.displayClientMessage(
                    Component.literal("§aAdded " + ingredient.getHoverName().getString() + " to frying pan"),
                    true
            );
            return InteractionResult.SUCCESS;
        } else {
            player.displayClientMessage(
                    Component.literal("§cFrying pan is full!"),
                    true
            );
            return InteractionResult.FAIL;
        }
    }

    /**
     * Handle status check
     */
    private InteractionResult handleStatusCheck(FryingPanBlockEntity pan, Player player) {
        if (pan.isCooking()) {
            float progress = pan.getCookingProgress() * 100;
            player.displayClientMessage(
                    Component.literal(String.format("§eFrying: %.0f%%", progress)),
                    true
            );
        } else if (pan.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§aFood ready: " + pan.getStoredFoodType()),
                    true
            );
        } else if (pan.hasIngredients()) {
            player.displayClientMessage(
                    Component.literal("§eIngredients: " + pan.getIngredients().size() + " items. Use spatula to start frying."),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal("§7Empty frying pan"),
                    true
            );
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Check if item is a spatula
     */
    private boolean isSpatula(ItemStack stack) {
        return stack.getItem().toString().contains("spatula") ||
               stack.getItem().getDescriptionId().contains("spatula");
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
            if (blockEntity instanceof FryingPanBlockEntity pan) {
                // Drop all ingredients
                for (ItemStack ingredient : pan.getIngredients()) {
                    popResource(level, pos, ingredient);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
