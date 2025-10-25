package jp.houlab.mochidsuki.customcookingmod.blockentity;

import jp.houlab.mochidsuki.customcookingmod.menu.AIKitchenMenu;
import jp.houlab.mochidsuki.customcookingmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for AI Kitchen
 * Handles AI recipe generation requests and stores cooked food
 */
public class AIKitchenBlockEntity extends CookingBlockEntity implements MenuProvider {

    public AIKitchenBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AI_KITCHEN_BE.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.customcookingmod.ai_kitchen");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AIKitchenMenu(containerId, playerInventory, this);
    }
}
