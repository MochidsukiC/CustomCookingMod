package jp.houlab.mochidsuki.customcookingmod.block;

import jp.houlab.mochidsuki.customcookingmod.blockentity.IHHeaterBlockEntity;
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
 * IH Heater Block
 * Versatile cooking block that supports multiple cooking methods based on tool used
 * - Spatula: stir-fry
 * - Spoon: simmer
 * - Empty hand: boil/fry (auto-detect based on ingredients)
 */
public class IHHeaterBlock extends BaseEntityBlock {

    public IHHeaterBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IHHeaterBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof IHHeaterBlockEntity heater) {
                IHHeaterBlockEntity.tick(level1, pos, state1, heater);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IHHeaterBlockEntity heater) {
                ItemStack heldItem = player.getItemInHand(hand);

                // If holding cooking tool, start cooking with appropriate action
                if (isSpatula(heldItem)) {
                    return handleCookingStart(heater, player, level, pos, CookingAction.STIR_FRY);
                } else if (isSpoon(heldItem)) {
                    return handleCookingStart(heater, player, level, pos, CookingAction.SIMMER);
                }
                // If holding ingredient, add it
                else if (isIngredient(heldItem)) {
                    return handleIngredientAddition(heater, heldItem, player, level, pos);
                }
                // Empty hand with ingredients - auto-detect cooking method
                else if (heldItem.isEmpty() && heater.hasIngredients() && !heater.isCooking()) {
                    CookingAction action = detectCookingMethod(heater);
                    return handleCookingStart(heater, player, level, pos, action);
                }
                // Empty hand - show status
                else if (heldItem.isEmpty()) {
                    return handleStatusCheck(heater, player);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Detect appropriate cooking method based on ingredients
     */
    private CookingAction detectCookingMethod(IHHeaterBlockEntity heater) {
        int ingredientCount = heater.getIngredients().size();

        // Simple heuristic:
        // - 1-2 ingredients: FRY
        // - 3+ ingredients: BOIL
        if (ingredientCount <= 2) {
            return CookingAction.FRY;
        } else {
            return CookingAction.BOIL;
        }
    }

    /**
     * Handle starting cooking with specific action
     */
    private InteractionResult handleCookingStart(IHHeaterBlockEntity heater, Player player,
                                                 Level level, BlockPos pos, CookingAction action) {
        if (heater.isCooking()) {
            player.displayClientMessage(
                    Component.literal("§eAlready cooking..."),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (!heater.hasIngredients()) {
            player.displayClientMessage(
                    Component.literal("§cNo ingredients on IH heater!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (heater.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§cIH heater already has cooked food! Remove it first."),
                    true
            );
            return InteractionResult.FAIL;
        }

        // Start cooking
        boolean started = heater.startCooking(
                action.getId(),
                action.getDefaultDurationTicks()
        );

        if (started) {
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.5F, 1.0F);
            player.displayClientMessage(
                    Component.literal("§aStarted " + action.getDisplayName().toLowerCase() + "!"),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    /**
     * Handle adding ingredient to IH heater
     */
    private InteractionResult handleIngredientAddition(IHHeaterBlockEntity heater, ItemStack ingredient,
                                                       Player player, Level level, BlockPos pos) {
        if (heater.isCooking()) {
            player.displayClientMessage(
                    Component.literal("§cCannot add ingredients while cooking!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (heater.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§cIH heater already has cooked food! Remove it first."),
                    true
            );
            return InteractionResult.FAIL;
        }

        boolean added = heater.addIngredient(ingredient);
        if (added) {
            ingredient.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
            player.displayClientMessage(
                    Component.literal("§aAdded " + ingredient.getHoverName().getString() + " to IH heater"),
                    true
            );
            return InteractionResult.SUCCESS;
        } else {
            player.displayClientMessage(
                    Component.literal("§cIH heater is full!"),
                    true
            );
            return InteractionResult.FAIL;
        }
    }

    /**
     * Handle status check
     */
    private InteractionResult handleStatusCheck(IHHeaterBlockEntity heater, Player player) {
        if (heater.isCooking()) {
            float progress = heater.getCookingProgress() * 100;
            player.displayClientMessage(
                    Component.literal(String.format("§eCooking: %.0f%%", progress)),
                    true
            );
        } else if (heater.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§aFood ready: " + heater.getStoredFoodType()),
                    true
            );
        } else if (heater.hasIngredients()) {
            player.displayClientMessage(
                    Component.literal("§eIngredients: " + heater.getIngredients().size() +
                                    " items. Use spatula/spoon or empty hand to cook."),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal("§7Empty IH heater"),
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
     * Check if item is a spoon
     */
    private boolean isSpoon(ItemStack stack) {
        return stack.getItem().toString().contains("spoon") ||
               stack.getItem().getDescriptionId().contains("spoon");
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
            if (blockEntity instanceof IHHeaterBlockEntity heater) {
                // Drop all ingredients
                for (ItemStack ingredient : heater.getIngredients()) {
                    popResource(level, pos, ingredient);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
