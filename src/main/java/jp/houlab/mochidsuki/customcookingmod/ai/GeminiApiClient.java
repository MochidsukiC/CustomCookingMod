package jp.houlab.mochidsuki.customcookingmod.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import jp.houlab.mochidsuki.customcookingmod.Config;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Google Gemini API Client
 * Handles communication with Google's Gemini 1.5 Pro API
 */
public class GeminiApiClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private final HttpClient httpClient;

    public GeminiApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Config.geminiTimeoutSeconds))
                .build();
    }

    /**
     * Send a prompt to Gemini API and get response asynchronously
     *
     * @param prompt The prompt to send to the AI
     * @return CompletableFuture with the AI's response
     */
    public CompletableFuture<String> generateRecipe(String prompt) {
        if (Config.geminiApiKey == null || Config.geminiApiKey.isEmpty()) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Gemini API key not configured"));
            return future;
        }

        // Build request body
        JsonObject requestBody = buildRequestBody(prompt);
        String requestBodyString = GSON.toJson(requestBody);

        // Build request
        String url = Config.geminiApiEndpoint + "?key=" + Config.geminiApiKey;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build();

        // Send request asynchronously
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        LOGGER.error("Gemini API returned error code: {}", response.statusCode());
                        LOGGER.error("Response body: {}", response.body());
                        throw new RuntimeException("Gemini API error: " + response.statusCode());
                    }
                    return parseResponse(response.body());
                })
                .exceptionally(throwable -> {
                    LOGGER.error("Failed to call Gemini API", throwable);
                    return null;
                });
    }

    /**
     * Build JSON request body for Gemini API
     */
    private JsonObject buildRequestBody(String prompt) {
        JsonObject requestBody = new JsonObject();

        // Create contents array
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();

        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);

        content.add("parts", parts);
        contents.add(content);

        requestBody.add("contents", contents);

        // Add generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("maxOutputTokens", 2048);
        requestBody.add("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * Parse Gemini API response and extract generated text
     */
    private String parseResponse(String responseBody) {
        try {
            JsonObject responseJson = GSON.fromJson(responseBody, JsonObject.class);

            if (!responseJson.has("candidates")) {
                LOGGER.error("No candidates in response");
                return null;
            }

            JsonArray candidates = responseJson.getAsJsonArray("candidates");
            if (candidates.size() == 0) {
                LOGGER.error("Empty candidates array");
                return null;
            }

            JsonObject candidate = candidates.get(0).getAsJsonObject();
            JsonObject content = candidate.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");

            if (parts.size() == 0) {
                LOGGER.error("Empty parts array");
                return null;
            }

            String text = parts.get(0).getAsJsonObject().get("text").getAsString();
            LOGGER.info("Gemini API response received successfully");
            return text;

        } catch (Exception e) {
            LOGGER.error("Failed to parse Gemini API response", e);
            return null;
        }
    }

    /**
     * Synchronous version of generateRecipe (blocks until response is received)
     */
    public String generateRecipeSync(String prompt) {
        try {
            return generateRecipe(prompt).get();
        } catch (Exception e) {
            LOGGER.error("Failed to generate recipe", e);
            return null;
        }
    }
}
