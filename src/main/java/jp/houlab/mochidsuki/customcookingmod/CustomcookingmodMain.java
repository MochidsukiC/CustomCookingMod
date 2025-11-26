package jp.houlab.mochidsuki.customcookingmod;

import com.mojang.logging.LogUtils;
import jp.houlab.mochidsuki.customcookingmod.network.ModNetworking;
import jp.houlab.mochidsuki.customcookingmod.registry.ModBlockEntities;
import jp.houlab.mochidsuki.customcookingmod.registry.ModBlocks;
import jp.houlab.mochidsuki.customcookingmod.registry.ModItems;
import jp.houlab.mochidsuki.customcookingmod.registry.ModMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CustomcookingmodMain.MODID)
public class CustomcookingmodMain {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "customcookingmod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "customcookingmod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a creative tab for CustomCookingMod
    public static final RegistryObject<CreativeModeTab> CUSTOM_COOKING_TAB = CREATIVE_MODE_TABS.register("custom_cooking_tab",
        () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> new ItemStack(ModItems.RICE.get()))
            .title(Component.translatable("itemGroup.customcookingmod.custom_cooking_tab"))
            .displayItems((parameters, output) -> {
                // 基本原材料
                output.accept(ModItems.RICE.get());
                output.accept(ModItems.SALT.get());
                output.accept(ModItems.MIRIN.get());
                output.accept(ModItems.COOKING_SAKE.get());
                output.accept(ModItems.MISO.get());
                output.accept(ModItems.COOKING_OIL.get());
                output.accept(ModItems.SESAME_OIL.get());
                output.accept(ModItems.PEPPER.get());
                output.accept(ModItems.POTATO_STARCH.get());
                output.accept(ModItems.FAILED_DISH.get());

                // 調理器具アイテム
                // Note: kitchen_knife and pot are provided by KaleidoscopeCookery
                output.accept(ModItems.SPATULA.get());
                output.accept(ModItems.LADLE.get());
                output.accept(ModItems.FRYING_PAN.get());
                output.accept(ModItems.BOWL.get());

                // 調理器具ブロック
                // Note: cutting_board (chopping_board) is provided by KaleidoscopeCookery
                output.accept(ModBlocks.AI_KITCHEN.get());
                output.accept(ModBlocks.IH_HEATER.get());
                output.accept(ModBlocks.OVEN.get());
                output.accept(ModBlocks.RICE_COOKER.get());
                output.accept(ModBlocks.MICROWAVE.get());
                output.accept(ModBlocks.HOT_PLATE.get());
            }).build());

    public CustomcookingmodMain() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register our custom registries
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Common setup code
        LOGGER.info("CustomCookingMod - Common Setup");
        LOGGER.info("AI-Powered Dynamic Cooking System Initialized");

        // Register networking
        event.enqueueWork(() -> {
            ModNetworking.register();
            LOGGER.info("Network channels registered");
        });

        // Log API configuration status (without exposing the key)
        if (Config.geminiApiKey != null && !Config.geminiApiKey.isEmpty()) {
            LOGGER.info("Gemini API Key configured");
        } else {
            LOGGER.warn("Gemini API Key not configured! Please set it in the config file.");
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            // Register screens
            event.enqueueWork(() -> {
                net.minecraft.client.gui.screens.MenuScreens.register(
                        jp.houlab.mochidsuki.customcookingmod.registry.ModMenuTypes.AI_KITCHEN_MENU.get(),
                        jp.houlab.mochidsuki.customcookingmod.screen.AIKitchenScreen::new
                );
            });
        }
    }
}
