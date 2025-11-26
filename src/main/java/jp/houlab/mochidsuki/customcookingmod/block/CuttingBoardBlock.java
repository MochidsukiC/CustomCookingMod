package jp.houlab.mochidsuki.customcookingmod.block;

import jp.houlab.mochidsuki.customcookingmod.blockentity.CuttingBoardBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Cutting Board Block
 * Players can place ingredients on it and chop them with a knife
 */
public class CuttingBoardBlock extends BaseEntityBlock {

    public CuttingBoardBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CuttingBoardBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CuttingBoardBlockEntity cuttingBoard) {
                ItemStack heldItem = player.getItemInHand(hand);

                // Check if player is holding a knife
                if (isKnife(heldItem)) {
                    return handleKnifeInteraction(cuttingBoard, heldItem, player, level, pos);
                }
                // Check if player is holding an ingredient
                else if (isChoppableIngredient(heldItem)) {
                    return handleIngredientPlacement(cuttingBoard, heldItem, player, level, pos);
                }
                // Empty hand - pick up ingredient
                else if (heldItem.isEmpty() && cuttingBoard.hasIngredient()) {
                    return handleIngredientPickup(cuttingBoard, player, level, pos);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Handle knife interaction to chop ingredient
     */
    private InteractionResult handleKnifeInteraction(CuttingBoardBlockEntity cuttingBoard,
                                                     ItemStack knife, Player player,
                                                     Level level, BlockPos pos) {
        if (!cuttingBoard.hasIngredient()) {
            player.displayClientMessage(
                    Component.literal("§cNo ingredient on cutting board!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (cuttingBoard.isChopped()) {
            player.displayClientMessage(
                    Component.literal("§cIngredient already chopped!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        // Chop the ingredient
        cuttingBoard.chopIngredient();

        // Play chopping sound
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);

        // Damage knife
        knife.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));

        player.displayClientMessage(
                Component.literal("§aIngredient chopped!"),
                true
        );

        return InteractionResult.SUCCESS;
    }

    /**
     * Handle ingredient placement
     */
    private InteractionResult handleIngredientPlacement(CuttingBoardBlockEntity cuttingBoard,
                                                       ItemStack ingredient, Player player,
                                                       Level level, BlockPos pos) {
        if (cuttingBoard.hasIngredient()) {
            player.displayClientMessage(
                    Component.literal("§cCutting board already has an ingredient!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        // Place ingredient
        cuttingBoard.placeIngredient(ingredient);
        ingredient.shrink(1);

        // Play placement sound
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

        player.displayClientMessage(
                Component.literal("§aIngredient placed on cutting board"),
                true
        );

        return InteractionResult.SUCCESS;
    }

    /**
     * Handle ingredient pickup
     */
    private InteractionResult handleIngredientPickup(CuttingBoardBlockEntity cuttingBoard,
                                                     Player player, Level level, BlockPos pos) {
        ItemStack ingredient = cuttingBoard.removeIngredient();

        // If chopped, create chopped version
        if (cuttingBoard.isChopped()) {
            ingredient = createChoppedVersion(ingredient);
        }

        // Give to player
        if (!player.getInventory().add(ingredient)) {
            player.drop(ingredient, false);
        }

        // Play pickup sound
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                       ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);

        return InteractionResult.SUCCESS;
    }

    /**
     * Check if item is a knife
     */
    private boolean isKnife(ItemStack stack) {
        // Check for KaleidoscopeCookery's kitchen_knife
        String itemId = stack.getItem().toString();
        return itemId.contains("kitchen_knife") ||
               stack.getItem().getDescriptionId().contains("kitchen_knife");
    }

    /**
     * Check if item can be chopped
     */
    private boolean isChoppableIngredient(ItemStack stack) {
        // Check if it's a food item or specific ingredient
        return stack.isEdible() ||
               stack.getItem().toString().contains("vegetable") ||
               stack.getItem().toString().contains("meat") ||
               stack.getItem().getDescriptionId().contains("tomato") ||
               stack.getItem().getDescriptionId().contains("lettuce") ||
               stack.getItem().getDescriptionId().contains("carrot") ||
               stack.getItem().getDescriptionId().contains("potato");
    }

    /**
     * Create chopped version of ingredient
     */
    private ItemStack createChoppedVersion(ItemStack original) {
        // Create a copy with NBT tag indicating it's chopped
        ItemStack chopped = original.copy();
        chopped.getOrCreateTag().putBoolean("chopped", true);

        // Add "Chopped" prefix to display name
        String originalName = original.getHoverName().getString();
        chopped.setHoverName(Component.literal("Chopped " + originalName));

        return chopped;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CuttingBoardBlockEntity cuttingBoard) {
                // Drop ingredient if any
                if (cuttingBoard.hasIngredient()) {
                    ItemStack ingredient = cuttingBoard.removeIngredient();
                    popResource(level, pos, ingredient);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
