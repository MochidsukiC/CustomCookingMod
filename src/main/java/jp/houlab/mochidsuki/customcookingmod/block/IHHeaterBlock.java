package jp.houlab.mochidsuki.customcookingmod.block;

import jp.houlab.mochidsuki.customcookingmod.blockentity.IHHeaterBlockEntity;
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
 * IH Heater Block
 * Heat source for cooking blocks placed above it
 * Right-click to cycle heat levels: OFF → LOW → MEDIUM → HIGH → OFF
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

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IHHeaterBlockEntity ih) {
                ItemStack heldItem = player.getItemInHand(hand);

                // Right-click to cycle heat level
                if (heldItem.isEmpty() || !player.isCrouching()) {
                    ih.cycleHeatLevel();
                    IHHeaterBlockEntity.HeatLevel newLevel = ih.getHeatLevel();

                    // Play sound based on new level
                    if (newLevel == IHHeaterBlockEntity.HeatLevel.OFF) {
                        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
                    } else {
                        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.5F, 1.0F);
                    }

                    // Display heat level
                    String color = getColorForHeatLevel(newLevel);
                    player.displayClientMessage(
                            Component.literal(color + "IH Heat: " + newLevel.getDisplayName()),
                            true
                    );

                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Get display color for heat level
     */
    private String getColorForHeatLevel(IHHeaterBlockEntity.HeatLevel level) {
        switch (level) {
            case OFF:
                return "§7"; // Gray
            case LOW:
                return "§e"; // Yellow
            case MEDIUM:
                return "§6"; // Gold
            case HIGH:
                return "§c"; // Red
            default:
                return "§f"; // White
        }
    }

    /**
     * Static helper method to check if there's an IH heater below a position
     */
    public static IHHeaterBlockEntity getIHBelow(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockEntity blockEntity = level.getBlockEntity(below);
        if (blockEntity instanceof IHHeaterBlockEntity) {
            return (IHHeaterBlockEntity) blockEntity;
        }
        return null;
    }

    /**
     * Static helper method to get heat multiplier from IH below
     */
    public static float getHeatMultiplierFromBelow(Level level, BlockPos pos) {
        IHHeaterBlockEntity ih = getIHBelow(level, pos);
        if (ih != null && ih.isHeating()) {
            return ih.getSpeedMultiplier();
        }
        return 0.0f; // No heat source
    }
}
