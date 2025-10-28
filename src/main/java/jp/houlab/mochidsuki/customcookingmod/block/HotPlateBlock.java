package jp.houlab.mochidsuki.customcookingmod.block;

import jp.houlab.mochidsuki.customcookingmod.blockentity.HotPlateBlockEntity;
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
 * Hot Plate Block
 * Players can add ingredients and cook them by stir-frying
 */
public class HotPlateBlock extends BaseEntityBlock {

    public HotPlateBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HotPlateBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof HotPlateBlockEntity hotPlate) {
                HotPlateBlockEntity.tick(level1, pos, state1, hotPlate);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HotPlateBlockEntity hotPlate) {
                ItemStack heldItem = player.getItemInHand(hand);

                // If holding spatula, start/stop cooking
                if (isSpatula(heldItem)) {
                    return handleSpatulaInteraction(hotPlate, player, level, pos);
                }
                // If holding ingredient, add it
                else if (isIngredient(heldItem)) {
                    return handleIngredientAddition(hotPlate, heldItem, player, level, pos);
                }
                // Empty hand - show status
                else if (heldItem.isEmpty()) {
                    return handleStatusCheck(hotPlate, player);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Handle spatula interaction to start cooking
     */
    private InteractionResult handleSpatulaInteraction(HotPlateBlockEntity hotPlate, Player player, Level level, BlockPos pos) {
        if (hotPlate.isCooking()) {
            player.displayClientMessage(
                    Component.literal("§eAlready cooking..."),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (!hotPlate.hasIngredients()) {
            player.displayClientMessage(
                    Component.literal("§cNo ingredients on hot plate!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        // Start stir-frying
        boolean started = hotPlate.startCooking(
                CookingAction.STIR_FRY.getId(),
                CookingAction.STIR_FRY.getDefaultDurationTicks()
        );

        if (started) {
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.5F, 1.0F);
            player.displayClientMessage(
                    Component.literal("§aStarted stir-frying!"),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    /**
     * Handle adding ingredient to hot plate
     */
    private InteractionResult handleIngredientAddition(HotPlateBlockEntity hotPlate, ItemStack ingredient, Player player, Level level, BlockPos pos) {
        if (hotPlate.isCooking()) {
            player.displayClientMessage(
                    Component.literal("§cCannot add ingredients while cooking!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (hotPlate.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§cHot plate already has cooked food! Remove it first."),
                    true
            );
            return InteractionResult.FAIL;
        }

        boolean added = hotPlate.addIngredient(ingredient);
        if (added) {
            ingredient.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
            player.displayClientMessage(
                    Component.literal("§aAdded " + ingredient.getHoverName().getString() + " to hot plate"),
                    true
            );
            return InteractionResult.SUCCESS;
        } else {
            player.displayClientMessage(
                    Component.literal("§cHot plate is full!"),
                    true
            );
            return InteractionResult.FAIL;
        }
    }

    /**
     * Handle status check
     */
    private InteractionResult handleStatusCheck(HotPlateBlockEntity hotPlate, Player player) {
        if (hotPlate.isCooking()) {
            float progress = hotPlate.getCookingProgress() * 100;
            player.displayClientMessage(
                    Component.literal(String.format("§eCooking: %.0f%%", progress)),
                    true
            );
        } else if (hotPlate.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§aFood ready: " + hotPlate.getStoredFoodType()),
                    true
            );
        } else if (hotPlate.hasIngredients()) {
            player.displayClientMessage(
                    Component.literal("§eIngredients: " + hotPlate.getIngredients().size() + " items. Use spatula to cook."),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal("§7Empty hot plate"),
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
            if (blockEntity instanceof HotPlateBlockEntity hotPlate) {
                // Drop all ingredients
                for (ItemStack ingredient : hotPlate.getIngredients()) {
                    popResource(level, pos, ingredient);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
