package jp.houlab.mochidsuki.customcookingmod.menu;

import jp.houlab.mochidsuki.customcookingmod.blockentity.AIKitchenBlockEntity;
import jp.houlab.mochidsuki.customcookingmod.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * AI Kitchen Menu
 * Handles the container/inventory for the AI Kitchen GUI
 */
public class AIKitchenMenu extends AbstractContainerMenu {
    private final AIKitchenBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    // For client-side creation
    public AIKitchenMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // For server-side creation
    public AIKitchenMenu(int containerId, Inventory playerInv, BlockEntity blockEntity) {
        super(ModMenuTypes.AI_KITCHEN_MENU.get(), containerId);
        this.blockEntity = (AIKitchenBlockEntity) blockEntity;
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        // Add player inventory slots
        addPlayerInventory(playerInv);
        addPlayerHotbar(playerInv);

        // Add block entity inventory slots if needed
        // For now, this is a simple interface without item slots
        // The AI generation will be triggered through network packets
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            if (index < 36) {
                // Moving from player inventory
                if (!this.moveItemStackTo(stack, 36, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving to player inventory
                if (!this.moveItemStackTo(stack, 0, 36, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.levelAccess, player, this.blockEntity.getBlockState().getBlock());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public AIKitchenBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
