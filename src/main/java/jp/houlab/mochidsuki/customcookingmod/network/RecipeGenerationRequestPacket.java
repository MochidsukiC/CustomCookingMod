package jp.houlab.mochidsuki.customcookingmod.network;

import com.mojang.logging.LogUtils;
import jp.houlab.mochidsuki.customcookingmod.ai.RecipeGenerator;
import jp.houlab.mochidsuki.customcookingmod.blockentity.AIKitchenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Client to Server packet
 * Sent when player requests AI to generate a recipe
 */
public class RecipeGenerationRequestPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String dishName;
    private final String category;
    private final BlockPos kitchenPos;

    public RecipeGenerationRequestPacket(String dishName, String category, BlockPos kitchenPos) {
        this.dishName = dishName;
        this.category = category;
        this.kitchenPos = kitchenPos;
    }

    public static void encode(RecipeGenerationRequestPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.dishName);
        buf.writeUtf(packet.category);
        buf.writeBlockPos(packet.kitchenPos);
    }

    public static RecipeGenerationRequestPacket decode(FriendlyByteBuf buf) {
        String dishName = buf.readUtf();
        String category = buf.readUtf();
        BlockPos kitchenPos = buf.readBlockPos();
        return new RecipeGenerationRequestPacket(dishName, category, kitchenPos);
    }

    public static void handle(RecipeGenerationRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                LOGGER.error("Received RecipeGenerationRequestPacket but sender is null");
                return;
            }

            LOGGER.info("Received recipe generation request from {}: {} (category: {})",
                    player.getName().getString(), packet.dishName, packet.category);

            // Generate recipe using AI (run async to avoid blocking server thread)
            RecipeGenerator generator = new RecipeGenerator();

            // For now, we'll run this synchronously
            // TODO: Move to async thread pool
            RecipeGenerator.RecipeData recipeData = generator.generateRecipeForDish(packet.dishName, packet.category);

            if (recipeData != null) {
                // Store the cooked food in the AI Kitchen block entity
                BlockEntity blockEntity = player.level().getBlockEntity(packet.kitchenPos);
                if (blockEntity instanceof AIKitchenBlockEntity kitchen) {
                    kitchen.storeFood(
                            recipeData.dishName,
                            recipeData.totalWeightGrams,
                            recipeData.nutritionPer100g,
                            recipeData.saturationPer100g
                    );
                    LOGGER.info("Stored {}g of {} in AI Kitchen at {}",
                            recipeData.totalWeightGrams, recipeData.dishName, packet.kitchenPos);
                }

                // Send response back to client
                ModNetworking.sendToPlayer(new RecipeGenerationResponsePacket(true, recipeData), player);
                LOGGER.info("Successfully generated recipe for: {}", packet.dishName);
            } else {
                // Send failure response
                ModNetworking.sendToPlayer(new RecipeGenerationResponsePacket(false, null), player);
                LOGGER.error("Failed to generate recipe for: {}", packet.dishName);
            }
        });
        context.setPacketHandled(true);
    }
}
