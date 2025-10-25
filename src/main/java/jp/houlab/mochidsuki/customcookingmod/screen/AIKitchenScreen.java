package jp.houlab.mochidsuki.customcookingmod.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import jp.houlab.mochidsuki.customcookingmod.CustomcookingmodMain;
import jp.houlab.mochidsuki.customcookingmod.menu.AIKitchenMenu;
import jp.houlab.mochidsuki.customcookingmod.network.ModNetworking;
import jp.houlab.mochidsuki.customcookingmod.network.RecipeGenerationRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * AI Kitchen Screen
 * Client-side GUI for the AI Kitchen
 */
public class AIKitchenScreen extends AbstractContainerScreen<AIKitchenMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CustomcookingmodMain.MODID, "textures/gui/ai_kitchen.png");

    private EditBox dishNameInput;
    private EditBox categoryInput;
    private Button generateButton;
    private Component statusMessage;

    public AIKitchenScreen(AIKitchenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
        this.statusMessage = Component.empty();
    }

    @Override
    protected void init() {
        super.init();

        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;

        // Dish name input field
        this.dishNameInput = new EditBox(this.font, relX + 10, relY + 20, 140, 20,
                Component.translatable("gui.customcookingmod.ai_kitchen.dish_name"));
        this.dishNameInput.setMaxLength(50);
        this.dishNameInput.setHint(Component.translatable("gui.customcookingmod.ai_kitchen.dish_name_hint"));
        this.addRenderableWidget(this.dishNameInput);

        // Category input field
        this.categoryInput = new EditBox(this.font, relX + 10, relY + 45, 140, 20,
                Component.translatable("gui.customcookingmod.ai_kitchen.category"));
        this.categoryInput.setMaxLength(30);
        this.categoryInput.setHint(Component.translatable("gui.customcookingmod.ai_kitchen.category_hint"));
        this.addRenderableWidget(this.categoryInput);

        // Generate button
        this.generateButton = Button.builder(
                Component.translatable("gui.customcookingmod.ai_kitchen.generate"),
                button -> onGenerateClicked())
                .bounds(relX + 10, relY + 70, 140, 20)
                .build();
        this.addRenderableWidget(this.generateButton);
    }

    private void onGenerateClicked() {
        String dishName = this.dishNameInput.getValue().trim();
        String category = this.categoryInput.getValue().trim();

        if (dishName.isEmpty()) {
            this.statusMessage = Component.translatable("gui.customcookingmod.ai_kitchen.error.empty_name");
            return;
        }

        if (category.isEmpty()) {
            category = "general"; // Default category
        }

        // Send packet to server to request recipe generation
        ModNetworking.sendToServer(new RecipeGenerationRequestPacket(dishName, category));
        this.statusMessage = Component.translatable("gui.customcookingmod.ai_kitchen.generating");
        this.generateButton.active = false;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;

        // Draw the background texture (if it exists, otherwise draw a simple background)
        guiGraphics.blit(TEXTURE, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Render status message
        if (!this.statusMessage.getString().isEmpty()) {
            int relX = (this.width - this.imageWidth) / 2;
            int relY = (this.height - this.imageHeight) / 2;
            guiGraphics.drawString(this.font, this.statusMessage, relX + 10, relY + 95, 0xFFFFFF, false);
        }
    }

    public void setStatusMessage(Component message) {
        this.statusMessage = message;
        this.generateButton.active = true;
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        String dishName = this.dishNameInput.getValue();
        String category = this.categoryInput.getValue();
        super.resize(minecraft, width, height);
        this.dishNameInput.setValue(dishName);
        this.categoryInput.setValue(category);
    }
}
