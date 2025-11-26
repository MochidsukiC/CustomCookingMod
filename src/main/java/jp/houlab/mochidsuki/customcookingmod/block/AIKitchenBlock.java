package jp.houlab.mochidsuki.customcookingmod.block;

import jp.houlab.mochidsuki.customcookingmod.blockentity.AIKitchenBlockEntity;
import jp.houlab.mochidsuki.customcookingmod.item.FoodContainerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * AI Kitchen Block
 * Main block for AI-powered recipe generation
 */
public class AIKitchenBlock extends BaseEntityBlock {

    public AIKitchenBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AIKitchenBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AIKitchenBlockEntity kitchen) {
                ItemStack heldItem = player.getItemInHand(hand);

                // Check if player is holding a food container
                if (heldItem.getItem() instanceof FoodContainerItem container) {
                    // Try to transfer food from kitchen to container
                    if (kitchen.hasFood()) {
                        // Check if container is empty
                        if (FoodContainerItem.isEmpty(heldItem)) {
                            int capacity = container.getCapacityGrams();
                            int availableWeight = kitchen.getStoredWeightGrams();
                            int transferAmount = Math.min(capacity, availableWeight);

                            // Take food from kitchen
                            int actualAmount = kitchen.takeFood(transferAmount);

                            // Create filled container
                            ItemStack filledContainer = FoodContainerItem.createFilledContainer(
                                    heldItem.getItem(),
                                    actualAmount,
                                    kitchen.getStoredFoodType(),
                                    kitchen.getNutritionPer100g(),
                                    kitchen.getSaturationPer100g()
                            );

                            // Decrease held item count and give filled container
                            heldItem.shrink(1);
                            if (!player.getInventory().add(filledContainer)) {
                                player.drop(filledContainer, false);
                            }

                            player.displayClientMessage(
                                    Component.literal("§aTransferred " + actualAmount + "g of " +
                                            kitchen.getStoredFoodType() + " to container"),
                                    true
                            );

                            return InteractionResult.SUCCESS;
                        } else {
                            player.displayClientMessage(
                                    Component.literal("§cContainer is not empty!"),
                                    true
                            );
                            return InteractionResult.FAIL;
                        }
                    } else {
                        player.displayClientMessage(
                                Component.literal("§cNo food in this cooking block!"),
                                true
                        );
                        return InteractionResult.FAIL;
                    }
                } else {
                    // Not holding a container, open GUI
                    NetworkHooks.openScreen((ServerPlayer) player, kitchen, pos);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AIKitchenBlockEntity) {
                // Drop items if any
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
