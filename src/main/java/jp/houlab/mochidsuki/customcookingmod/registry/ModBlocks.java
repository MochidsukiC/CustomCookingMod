package jp.houlab.mochidsuki.customcookingmod.registry;

import jp.houlab.mochidsuki.customcookingmod.CustomcookingmodMain;
import jp.houlab.mochidsuki.customcookingmod.block.AIKitchenBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CustomcookingmodMain.MODID);

    // AIキッチンブロック (AI Kitchen Block)
    public static final RegistryObject<Block> AI_KITCHEN = registerBlock("ai_kitchen",
            () -> new AIKitchenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // IHヒーター (IH Heater)
    public static final RegistryObject<Block> IH_HEATER = registerBlock("ih_heater",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // オーブン (Oven)
    public static final RegistryObject<Block> OVEN = registerBlock("oven",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // 炊飯器 (Rice Cooker)
    public static final RegistryObject<Block> RICE_COOKER = registerBlock("rice_cooker",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // 電子レンジ (Microwave)
    public static final RegistryObject<Block> MICROWAVE = registerBlock("microwave",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // ホットプレート (Hot Plate)
    public static final RegistryObject<Block> HOT_PLATE = registerBlock("hot_plate",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // Note: Cutting board (chopping_board) and pot are provided by KaleidoscopeCookery

    // Helper method to register block with item
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
