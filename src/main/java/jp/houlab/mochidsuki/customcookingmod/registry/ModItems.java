package jp.houlab.mochidsuki.customcookingmod.registry;

import jp.houlab.mochidsuki.customcookingmod.CustomcookingmodMain;
import jp.houlab.mochidsuki.customcookingmod.item.FoodContainerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CustomcookingmodMain.MODID);

    // 基本原材料アイテム (Basic Ingredients)
    public static final RegistryObject<Item> RICE = ITEMS.register("rice",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SALT = ITEMS.register("salt",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MIRIN = ITEMS.register("mirin",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COOKING_SAKE = ITEMS.register("cooking_sake",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MISO = ITEMS.register("miso",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COOKING_OIL = ITEMS.register("cooking_oil",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SESAME_OIL = ITEMS.register("sesame_oil",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PEPPER = ITEMS.register("pepper",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> POTATO_STARCH = ITEMS.register("potato_starch",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SOY_SAUCE = ITEMS.register("soy_sauce",
            () -> new Item(new Item.Properties()));

    // 調理失敗アイテム (Failed Cooking Item)
    public static final RegistryObject<Item> FAILED_DISH = ITEMS.register("failed_dish",
            () -> new Item(new Item.Properties()));

    // 調理器具アイテム (Cooking Tools)
    // Note: kitchen_knife and pot are provided by KaleidoscopeCookery

    public static final RegistryObject<Item> SPATULA = ITEMS.register("spatula",
            () -> new Item(new Item.Properties().durability(128)));

    public static final RegistryObject<Item> LADLE = ITEMS.register("ladle",
            () -> new Item(new Item.Properties().durability(128)));

    public static final RegistryObject<Item> FRYING_PAN = ITEMS.register("frying_pan",
            () -> new Item(new Item.Properties().durability(256)));

    public static final RegistryObject<Item> BOWL = ITEMS.register("bowl",
            () -> new Item(new Item.Properties()));

    // 計量器具 (Measuring Tools)
    public static final RegistryObject<Item> MEASURING_CUP = ITEMS.register("measuring_cup",
            () -> new Item(new Item.Properties().durability(256)));

    public static final RegistryObject<Item> KITCHEN_SCALE = ITEMS.register("kitchen_scale",
            () -> new Item(new Item.Properties().durability(512)));

    public static final RegistryObject<Item> SPOON = ITEMS.register("spoon",
            () -> new Item(new Item.Properties().durability(128)));

    // 容器アイテム (Container Items)
    // Containers can hold cooked food with weight-based system
    public static final RegistryObject<Item> PLASTIC_CONTAINER = ITEMS.register("plastic_container",
            () -> new FoodContainerItem(new Item.Properties().stacksTo(16), 200)); // 200g capacity

    public static final RegistryObject<Item> PLATE = ITEMS.register("plate",
            () -> new FoodContainerItem(new Item.Properties().stacksTo(16), 300)); // 300g capacity

    public static final RegistryObject<Item> LARGE_PLATE = ITEMS.register("large_plate",
            () -> new FoodContainerItem(new Item.Properties().stacksTo(16), 500)); // 500g capacity

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
