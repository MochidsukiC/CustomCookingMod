package jp.houlab.mochidsuki.customcookingmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Dynamic Food Item
 * Represents a food item that is generated dynamically by AI
 */
public class DynamicFoodItem extends Item {

    public DynamicFoodItem(int nutrition, float saturation) {
        super(new Item.Properties()
                .food(new FoodProperties.Builder()
                        .nutrition(nutrition)
                        .saturationMod(saturation)
                        .build())
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        // Add custom tooltip for AI-generated food
        if (stack.hasTag() && stack.getTag().contains("dish_name")) {
            String dishName = stack.getTag().getString("dish_name");
            tooltipComponents.add(Component.literal(dishName).withStyle(ChatFormatting.GOLD));
        }

        // Display expiration time
        if (stack.hasTag() && stack.getTag().contains("created_time")) {
            long createdTime = stack.getTag().getLong("created_time");
            int expirationHours = stack.getTag().getInt("expiration_hours");

            if (level != null) {
                long currentTime = level.getGameTime();
                long elapsedTicks = currentTime - createdTime;
                long elapsedHours = elapsedTicks / 1000; // Minecraft: 1000 ticks = 1 hour (roughly)

                long remainingHours = expirationHours - elapsedHours;

                if (remainingHours <= 0) {
                    tooltipComponents.add(Component.literal("Expired").withStyle(ChatFormatting.RED));
                } else if (remainingHours < 12) {
                    tooltipComponents.add(Component.literal("Expires soon: " + remainingHours + "h").withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltipComponents.add(Component.literal("Fresh for: " + remainingHours + "h").withStyle(ChatFormatting.GREEN));
                }
            }
        }

        // AI-generated tag
        tooltipComponents.add(Component.literal("AI-Generated Recipe").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Add enchantment glint effect to AI-generated items
        return stack.hasTag() && stack.getTag().contains("ai_generated");
    }
}
