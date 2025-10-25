package jp.houlab.mochidsuki.customcookingmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base class for food container items (plastic containers, plates, etc.)
 * Containers have a capacity (in grams) and can hold cooked food
 */
public class FoodContainerItem extends Item {
    private final int capacityGrams;

    public FoodContainerItem(Properties properties, int capacityGrams) {
        super(properties);
        this.capacityGrams = capacityGrams;
    }

    public int getCapacityGrams() {
        return capacityGrams;
    }

    /**
     * Get the current weight of food in this container
     */
    public static int getCurrentWeight(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("weight")) {
            return 0;
        }
        return tag.getInt("weight");
    }

    /**
     * Get the food type stored in this container
     */
    public static String getFoodType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("food_type")) {
            return "";
        }
        return tag.getString("food_type");
    }

    /**
     * Get nutrition per 100g
     */
    public static float getNutritionPer100g(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("nutrition_per_100g")) {
            return 0.0f;
        }
        return tag.getFloat("nutrition_per_100g");
    }

    /**
     * Get saturation per 100g
     */
    public static float getSaturationPer100g(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("saturation_per_100g")) {
            return 0.0f;
        }
        return tag.getFloat("saturation_per_100g");
    }

    /**
     * Create a filled container with food
     */
    public static ItemStack createFilledContainer(Item containerItem, int weightGrams, String foodType,
                                                   float nutritionPer100g, float saturationPer100g) {
        ItemStack stack = new ItemStack(containerItem);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("weight", weightGrams);
        tag.putString("food_type", foodType);
        tag.putFloat("nutrition_per_100g", nutritionPer100g);
        tag.putFloat("saturation_per_100g", saturationPer100g);
        return stack;
    }

    /**
     * Check if the container is empty
     */
    public static boolean isEmpty(ItemStack stack) {
        return getCurrentWeight(stack) == 0;
    }

    /**
     * Check if the container is full
     */
    public boolean isFull(ItemStack stack) {
        return getCurrentWeight(stack) >= capacityGrams;
    }

    /**
     * Get remaining capacity
     */
    public int getRemainingCapacity(ItemStack stack) {
        return Math.max(0, capacityGrams - getCurrentWeight(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.translatable("tooltip.customcookingmod.container.capacity", capacityGrams)
                .withStyle(ChatFormatting.GRAY));

        int currentWeight = getCurrentWeight(stack);
        if (currentWeight > 0) {
            String foodType = getFoodType(stack);
            tooltip.add(Component.translatable("tooltip.customcookingmod.container.contents",
                    foodType, currentWeight).withStyle(ChatFormatting.YELLOW));

            float nutrition = getNutritionPer100g(stack);
            float saturation = getSaturationPer100g(stack);

            // Calculate total nutrition for current weight
            float totalNutrition = (nutrition * currentWeight) / 100.0f;
            float totalSaturation = (saturation * currentWeight) / 100.0f;

            tooltip.add(Component.translatable("tooltip.customcookingmod.container.nutrition",
                    String.format("%.1f", totalNutrition)).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("tooltip.customcookingmod.container.saturation",
                    String.format("%.1f", totalSaturation)).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.customcookingmod.container.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        int currentWeight = getCurrentWeight(stack);
        if (currentWeight > 0) {
            String foodType = getFoodType(stack);
            // e.g., "Yakisoba (200g) - Plastic Container"
            return Component.translatable("item.customcookingmod.filled_container",
                    foodType, currentWeight, super.getName(stack));
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Only allow eating if container has food
        if (!isEmpty(stack)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            int weight = getCurrentWeight(stack);
            float nutritionPer100g = getNutritionPer100g(stack);
            float saturationPer100g = getSaturationPer100g(stack);

            // Calculate nutrition based on weight
            int nutrition = Math.round((nutritionPer100g * weight) / 100.0f);
            float saturation = (saturationPer100g * weight) / 100.0f;

            // Restore hunger and saturation
            player.getFoodData().eat(nutrition, saturation);

            // Play eating sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BURP, player.getSoundSource(), 1.0F, 1.0F);

            // Trigger advancement
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
                serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            }

            // Return empty container
            ItemStack emptyContainer = new ItemStack(stack.getItem());
            return emptyContainer;
        }

        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        // Eating time in ticks (32 ticks = 1.6 seconds, faster than normal food)
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_EAT;
    }
}
