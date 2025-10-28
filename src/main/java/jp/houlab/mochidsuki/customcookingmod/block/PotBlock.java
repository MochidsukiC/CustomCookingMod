package jp.houlab.mochidsuki.customcookingmod.block;

import jp.houlab.mochidsuki.customcookingmod.blockentity.PotBlockEntity;
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
 * Pot Block
 * Players can add ingredients and cook them by simmering or boiling
 */
public class PotBlock extends BaseEntityBlock {

    public PotBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PotBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof PotBlockEntity pot) {
                PotBlockEntity.tick(level1, pos, state1, pot);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PotBlockEntity pot) {
                ItemStack heldItem = player.getItemInHand(hand);

                // If holding spoon, start cooking (simmer or boil)
                if (isSpoon(heldItem)) {
                    return handleSpoonInteraction(pot, player, level, pos);
                }
                // If holding ingredient, add it
                else if (isIngredient(heldItem)) {
                    return handleIngredientAddition(pot, heldItem, player, level, pos);
                }
                // Empty hand - show status
                else if (heldItem.isEmpty()) {
                    return handleStatusCheck(pot, player);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Handle spoon interaction to start cooking
     */
    private InteractionResult handleSpoonInteraction(PotBlockEntity pot, Player player, Level level, BlockPos pos) {
        if (pot.isCooking()) {
            player.displayClientMessage(
                    Component.literal("§eAlready cooking..."),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (!pot.hasIngredients()) {
            player.displayClientMessage(
                    Component.literal("§cNo ingredients in pot!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        // Check if ingredients contain liquid or need simmering (simple heuristic: if >2 ingredients, simmer, else boil)
        CookingAction action = pot.getIngredients().size() > 2 ? CookingAction.SIMMER : CookingAction.BOIL;

        // Start cooking
        boolean started = pot.startCooking(
                action.getId(),
                action.getDefaultDurationTicks()
        );

        if (started) {
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5F, 1.0F);
            player.displayClientMessage(
                    Component.literal("§aStarted " + action.getDisplayName().toLowerCase() + "!"),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    /**
     * Handle adding ingredient to pot
     */
    private InteractionResult handleIngredientAddition(PotBlockEntity pot, ItemStack ingredient, Player player, Level level, BlockPos pos) {
        if (pot.isCooking()) {
            player.displayClientMessage(
                    Component.literal("§cCannot add ingredients while cooking!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (pot.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§cPot already has cooked food! Remove it first."),
                    true
            );
            return InteractionResult.FAIL;
        }

        boolean added = pot.addIngredient(ingredient);
        if (added) {
            ingredient.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
            player.displayClientMessage(
                    Component.literal("§aAdded " + ingredient.getHoverName().getString() + " to pot"),
                    true
            );
            return InteractionResult.SUCCESS;
        } else {
            player.displayClientMessage(
                    Component.literal("§cPot is full!"),
                    true
            );
            return InteractionResult.FAIL;
        }
    }

    /**
     * Handle status check
     */
    private InteractionResult handleStatusCheck(PotBlockEntity pot, Player player) {
        if (pot.isCooking()) {
            float progress = pot.getCookingProgress() * 100;
            player.displayClientMessage(
                    Component.literal(String.format("§eCooking: %.0f%%", progress)),
                    true
            );
        } else if (pot.hasFood()) {
            player.displayClientMessage(
                    Component.literal("§aFood ready: " + pot.getStoredFoodType()),
                    true
            );
        } else if (pot.hasIngredients()) {
            player.displayClientMessage(
                    Component.literal("§eIngredients: " + pot.getIngredients().size() + " items. Use spoon to cook."),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal("§7Empty pot"),
                    true
            );
        }
        return InteractionResult.SUCCESS;
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
            if (blockEntity instanceof PotBlockEntity pot) {
                // Drop all ingredients
                for (ItemStack ingredient : pot.getIngredients()) {
                    popResource(level, pos, ingredient);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
