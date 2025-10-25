package jp.houlab.mochidsuki.customcookingmod.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import jp.houlab.mochidsuki.customcookingmod.ai.RecipeGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Server to Client packet
 * Sent when server finishes generating a recipe
 */
public class RecipeGenerationResponsePacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private final boolean success;
    private final RecipeGenerator.RecipeData recipeData;

    public RecipeGenerationResponsePacket(boolean success, RecipeGenerator.RecipeData recipeData) {
        this.success = success;
        this.recipeData = recipeData;
    }

    public static void encode(RecipeGenerationResponsePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.success);

        if (packet.success && packet.recipeData != null) {
            // Serialize RecipeData to JSON
            JsonObject json = new JsonObject();
            json.addProperty("dishName", packet.recipeData.dishName);
            json.addProperty("totalWeightGrams", packet.recipeData.totalWeightGrams);
            json.addProperty("nutritionPer100g", packet.recipeData.nutritionPer100g);
            json.addProperty("saturationPer100g", packet.recipeData.saturationPer100g);
            json.addProperty("expirationHours", packet.recipeData.expirationHours);

            // Serialize ingredients
            JsonObject ingredientsObj = new JsonObject();
            for (int i = 0; i < packet.recipeData.ingredients.size(); i++) {
                RecipeGenerator.RecipeData.Ingredient ing = packet.recipeData.ingredients.get(i);
                JsonObject ingObj = new JsonObject();
                ingObj.addProperty("item", ing.itemId);
                ingObj.addProperty("amountType", ing.amountType);
                ingObj.addProperty("amount", ing.amount);
                ingredientsObj.add("ing" + i, ingObj);
            }
            json.add("ingredients", ingredientsObj);

            // Serialize steps
            JsonObject stepsObj = new JsonObject();
            for (int i = 0; i < packet.recipeData.steps.size(); i++) {
                RecipeGenerator.RecipeData.CookingStep step = packet.recipeData.steps.get(i);
                JsonObject stepObj = new JsonObject();
                stepObj.addProperty("action", step.action);
                stepObj.addProperty("description", step.description);
                stepsObj.add("step" + i, stepObj);
            }
            json.add("steps", stepsObj);

            String jsonString = GSON.toJson(json);
            buf.writeUtf(jsonString);
        }
    }

    public static RecipeGenerationResponsePacket decode(FriendlyByteBuf buf) {
        boolean success = buf.readBoolean();

        if (!success) {
            return new RecipeGenerationResponsePacket(false, null);
        }

        // Deserialize JSON
        String jsonString = buf.readUtf();
        JsonObject json = GSON.fromJson(jsonString, JsonObject.class);

        RecipeGenerator.RecipeData recipeData = new RecipeGenerator.RecipeData();
        recipeData.dishName = json.get("dishName").getAsString();
        recipeData.totalWeightGrams = json.get("totalWeightGrams").getAsInt();
        recipeData.nutritionPer100g = json.get("nutritionPer100g").getAsFloat();
        recipeData.saturationPer100g = json.get("saturationPer100g").getAsFloat();
        recipeData.expirationHours = json.get("expirationHours").getAsInt();

        // Deserialize ingredients
        recipeData.ingredients = new java.util.ArrayList<>();
        JsonObject ingredientsObj = json.getAsJsonObject("ingredients");
        for (String key : ingredientsObj.keySet()) {
            JsonObject ingObj = ingredientsObj.getAsJsonObject(key);
            String itemId = ingObj.get("item").getAsString();
            String amountType = ingObj.get("amountType").getAsString();
            float amount = ingObj.get("amount").getAsFloat();
            recipeData.ingredients.add(new RecipeGenerator.RecipeData.Ingredient(itemId, amountType, amount));
        }

        // Deserialize steps
        recipeData.steps = new java.util.ArrayList<>();
        JsonObject stepsObj = json.getAsJsonObject("steps");
        for (String key : stepsObj.keySet()) {
            JsonObject stepObj = stepsObj.getAsJsonObject(key);
            String action = stepObj.get("action").getAsString();
            String description = stepObj.get("description").getAsString();
            recipeData.steps.add(new RecipeGenerator.RecipeData.CookingStep(action, description));
        }

        return new RecipeGenerationResponsePacket(true, recipeData);
    }

    public static void handle(RecipeGenerationResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Client-side handling
            Minecraft minecraft = Minecraft.getInstance();

            if (packet.success && packet.recipeData != null) {
                LOGGER.info("Received recipe generation response: {}", packet.recipeData.dishName);

                // Display success message to player
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§aRecipe generated successfully: §6" + packet.recipeData.dishName),
                            false
                    );

                    // TODO: Register the recipe dynamically
                    // TODO: Open recipe book or display recipe details
                }
            } else {
                LOGGER.error("Recipe generation failed");

                // Display error message
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§cFailed to generate recipe. Please check server logs."),
                            false
                    );
                }
            }
        });
        context.setPacketHandled(true);
    }
}
