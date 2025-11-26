package jp.houlab.mochidsuki.customcookingmod.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe Generator using AI
 * Handles the generation of recipes based on player input and AI responses
 */
public class RecipeGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private final GeminiApiClient apiClient;

    public RecipeGenerator() {
        this.apiClient = new GeminiApiClient();
    }

    /**
     * Generate a recipe for the given dish name
     *
     * @param dishName The name of the dish to create
     * @param category The category of the dish (e.g., "donburi", "dessert")
     * @return RecipeData containing the generated recipe information
     */
    public RecipeData generateRecipeForDish(String dishName, String category) {
        String prompt = buildPrompt(dishName, category);
        LOGGER.info("Generating recipe for: {} (category: {})", dishName, category);

        String aiResponse = apiClient.generateRecipeSync(prompt);
        if (aiResponse == null || aiResponse.isEmpty()) {
            LOGGER.error("Failed to get AI response for dish: {}", dishName);
            return null;
        }

        return parseRecipeData(aiResponse, dishName);
    }

    /**
     * Build prompt for Gemini API
     */
    private String buildPrompt(String dishName, String category) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("あなたはMinecraftの料理マイスターです。\n\n");
        prompt.append("プレイヤーが「").append(dishName).append("」を作りたいと言っています。\n");
        prompt.append("カテゴリ: ").append(category).append("\n\n");

        prompt.append("以下の利用可能な材料とアイテムを使って、このレシピを考案してください:\n\n");
        prompt.append("=== 基本原材料（本Mod） ===\n");
        prompt.append("- customcookingmod:rice (米)\n");
        prompt.append("- customcookingmod:salt (塩)\n");
        prompt.append("- customcookingmod:mirin (みりん)\n");
        prompt.append("- customcookingmod:cooking_sake (料理酒)\n");
        prompt.append("- customcookingmod:miso (みそ)\n");
        prompt.append("- customcookingmod:soy_sauce (醤油)\n");
        prompt.append("- customcookingmod:cooking_oil (食用油)\n");
        prompt.append("- customcookingmod:sesame_oil (ごま油)\n");
        prompt.append("- customcookingmod:pepper (こしょう)\n");
        prompt.append("- customcookingmod:potato_starch (片栗粉)\n\n");

        prompt.append("=== KaleidoscopeCookeryの食材 ===\n");
        prompt.append("- kaleidoscope_cookery:tomato (トマト)\n");
        prompt.append("- kaleidoscope_cookery:lettuce (レタス)\n");
        prompt.append("- kaleidoscope_cookery:chili (唐辛子)\n");
        prompt.append("- kaleidoscope_cookery:rice (米)\n");
        prompt.append("- kaleidoscope_cookery:raw_lamb_chops (生羊肉)\n");
        prompt.append("- kaleidoscope_cookery:raw_pork_belly (豚バラ肉)\n");
        prompt.append("- kaleidoscope_cookery:raw_cow_offal (牛内臓)\n");
        prompt.append("- kaleidoscope_cookery:oil (油)\n\n");

        prompt.append("=== バニラMinecraftの食材 ===\n");
        prompt.append("- minecraft:wheat (小麦)\n");
        prompt.append("- minecraft:sugar (砂糖)\n");
        prompt.append("- minecraft:egg (卵)\n");
        prompt.append("- minecraft:water_bucket (水)\n");
        prompt.append("- minecraft:beef, pork, chicken, mutton (各種肉)\n");
        prompt.append("- minecraft:potato, carrot, beetroot (各種野菜)\n\n");

        prompt.append("=== 利用可能な調理法 ===\n");
        prompt.append("【本Mod】\n");
        prompt.append("1. IHヒーターで焼く (customcookingmod:ih_heater + customcookingmod:frying_pan)\n");
        prompt.append("2. ボウルでかき混ぜる (customcookingmod:bowl + customcookingmod:spatula)\n");
        prompt.append("3. オーブンにかける (customcookingmod:oven)\n");
        prompt.append("4. ご飯を炊く (customcookingmod:rice_cooker)\n");
        prompt.append("5. レンチン (customcookingmod:microwave)\n");
        prompt.append("6. ホットプレートで焼く (customcookingmod:hot_plate)\n\n");

        prompt.append("【KaleidoscopeCookery】\n");
        prompt.append("7. ストーブで焼く (kaleidoscope_cookery:stove)\n");
        prompt.append("8. 鍋でゆでる (kaleidoscope_cookery:pot)\n");
        prompt.append("9. 大鍋で煮込む (kaleidoscope_cookery:stockpot)\n");
        prompt.append("10. 蒸し器で蒸す (kaleidoscope_cookery:steamer)\n");
        prompt.append("11. まな板で切る (kaleidoscope_cookery:chopping_board + kaleidoscope_cookery:kitchen_knife)\n");
        prompt.append("12. 粉挽き機で挽く (kaleidoscope_cookery:millstone)\n\n");

        prompt.append("以下のJSON形式で応答してください:\n\n");
        prompt.append("重要なルール:\n");
        prompt.append("1. レシピは100gあたりの分量で指定してください\n");
        prompt.append("2. 材料は2種類の単位があります:\n");
        prompt.append("   - amountType=\"grams\": 粉や液体など個数カウントできないもの（塩、醤油、小麦粉、水、油など）\n");
        prompt.append("   - amountType=\"count\": 個数でカウントできる固形物（トマト、肉、卵、ニンジンなど）\n");
        prompt.append("3. totalWeightGramsは100固定にしてください\n");
        prompt.append("4. nutritionPer100gとsaturationPer100gには100gあたりの満腹度回復量を指定\n\n");
        prompt.append("{\n");
        prompt.append("  \"dishName\": \"" + dishName + "\",\n");
        prompt.append("  \"totalWeightGrams\": 100,\n");
        prompt.append("  \"ingredients\": [\n");
        prompt.append("    {\"item\": \"minecraft:wheat\", \"amountType\": \"grams\", \"amount\": 30.0},\n");
        prompt.append("    {\"item\": \"customcookingmod:salt\", \"amountType\": \"grams\", \"amount\": 0.5},\n");
        prompt.append("    {\"item\": \"minecraft:egg\", \"amountType\": \"count\", \"amount\": 0.05},\n");
        prompt.append("    {\"item\": \"kaleidoscope_cookery:tomato\", \"amountType\": \"count\", \"amount\": 0.1}\n");
        prompt.append("  ],\n");
        prompt.append("  \"steps\": [\n");
        prompt.append("    {\"action\": \"mix_in_bowl\", \"description\": \"ボウルで小麦と塩を混ぜる\"},\n");
        prompt.append("    {\"action\": \"cook_in_oven\", \"description\": \"オーブンで焼く\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"nutritionPer100g\": 2.5,\n");
        prompt.append("  \"saturationPer100g\": 0.3,\n");
        prompt.append("  \"expirationHours\": 48\n");
        prompt.append("}\n\n");

        prompt.append("IMPORTANT: JSONのみを返してください。説明文は不要です。");

        return prompt.toString();
    }

    /**
     * Parse AI response into RecipeData
     */
    private RecipeData parseRecipeData(String aiResponse, String dishName) {
        try {
            // Extract JSON from response (remove markdown code blocks if present)
            String jsonString = extractJSON(aiResponse);

            JsonObject json = GSON.fromJson(jsonString, JsonObject.class);

            RecipeData recipeData = new RecipeData();
            recipeData.dishName = json.get("dishName").getAsString();
            recipeData.totalWeightGrams = json.get("totalWeightGrams").getAsInt();
            recipeData.nutritionPer100g = json.get("nutritionPer100g").getAsFloat();
            recipeData.saturationPer100g = json.get("saturationPer100g").getAsFloat();
            recipeData.expirationHours = json.get("expirationHours").getAsInt();

            // Parse ingredients
            JsonArray ingredientsJson = json.getAsJsonArray("ingredients");
            recipeData.ingredients = new ArrayList<>();
            for (int i = 0; i < ingredientsJson.size(); i++) {
                JsonObject ingredient = ingredientsJson.get(i).getAsJsonObject();
                String itemId = ingredient.get("item").getAsString();
                String amountType = ingredient.get("amountType").getAsString();
                float amount = ingredient.get("amount").getAsFloat();
                recipeData.ingredients.add(new RecipeData.Ingredient(itemId, amountType, amount));
            }

            // Parse cooking steps
            JsonArray stepsJson = json.getAsJsonArray("steps");
            recipeData.steps = new ArrayList<>();
            for (int i = 0; i < stepsJson.size(); i++) {
                JsonObject step = stepsJson.get(i).getAsJsonObject();
                String action = step.get("action").getAsString();
                String description = step.get("description").getAsString();
                recipeData.steps.add(new RecipeData.CookingStep(action, description));
            }

            LOGGER.info("Successfully parsed recipe data for: {}", dishName);
            return recipeData;

        } catch (Exception e) {
            LOGGER.error("Failed to parse recipe data from AI response", e);
            return null;
        }
    }

    /**
     * Extract JSON from AI response (handles markdown code blocks)
     */
    private String extractJSON(String response) {
        // Remove markdown code blocks if present
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    /**
     * Data class for recipe information
     * Uses weight-based system with nutrition per 100g
     */
    public static class RecipeData {
        public String dishName;
        public int totalWeightGrams;  // Total weight of the dish when cooked
        public List<Ingredient> ingredients;
        public List<CookingStep> steps;
        public float nutritionPer100g;  // Nutrition value per 100g
        public float saturationPer100g;  // Saturation value per 100g
        public int expirationHours;

        public static class Ingredient {
            public String itemId;
            public String amountType;  // "grams" for seasonings/powders, "count" for solid items
            public float amount;  // Quantity (grams or count)

            public Ingredient(String itemId, String amountType, float amount) {
                this.itemId = itemId;
                this.amountType = amountType;
                this.amount = amount;
            }
        }

        public static class CookingStep {
            public String action;
            public String description;

            public CookingStep(String action, String description) {
                this.action = action;
                this.description = description;
            }
        }
    }
}
