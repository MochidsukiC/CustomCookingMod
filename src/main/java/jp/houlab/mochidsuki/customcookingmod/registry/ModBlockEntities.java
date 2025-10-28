package jp.houlab.mochidsuki.customcookingmod.registry;

import jp.houlab.mochidsuki.customcookingmod.CustomcookingmodMain;
import jp.houlab.mochidsuki.customcookingmod.blockentity.AIKitchenBlockEntity;
import jp.houlab.mochidsuki.customcookingmod.blockentity.CuttingBoardBlockEntity;
import jp.houlab.mochidsuki.customcookingmod.blockentity.HotPlateBlockEntity;
import jp.houlab.mochidsuki.customcookingmod.blockentity.IHHeaterBlockEntity;
import jp.houlab.mochidsuki.customcookingmod.blockentity.OvenBlockEntity;
import jp.houlab.mochidsuki.customcookingmod.blockentity.PotBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CustomcookingmodMain.MODID);

    public static final RegistryObject<BlockEntityType<AIKitchenBlockEntity>> AI_KITCHEN_BE =
            BLOCK_ENTITIES.register("ai_kitchen_be", () ->
                    BlockEntityType.Builder.of(AIKitchenBlockEntity::new,
                            ModBlocks.AI_KITCHEN.get()).build(null));

    public static final RegistryObject<BlockEntityType<CuttingBoardBlockEntity>> CUTTING_BOARD_BE =
            BLOCK_ENTITIES.register("cutting_board_be", () ->
                    BlockEntityType.Builder.of(CuttingBoardBlockEntity::new,
                            ModBlocks.CUTTING_BOARD.get()).build(null));

    public static final RegistryObject<BlockEntityType<HotPlateBlockEntity>> HOT_PLATE_BE =
            BLOCK_ENTITIES.register("hot_plate_be", () ->
                    BlockEntityType.Builder.of(HotPlateBlockEntity::new,
                            ModBlocks.HOT_PLATE.get()).build(null));

    public static final RegistryObject<BlockEntityType<PotBlockEntity>> POT_BE =
            BLOCK_ENTITIES.register("pot_be", () ->
                    BlockEntityType.Builder.of(PotBlockEntity::new,
                            ModBlocks.POT.get()).build(null));

    public static final RegistryObject<BlockEntityType<OvenBlockEntity>> OVEN_BE =
            BLOCK_ENTITIES.register("oven_be", () ->
                    BlockEntityType.Builder.of(OvenBlockEntity::new,
                            ModBlocks.OVEN.get()).build(null));

    public static final RegistryObject<BlockEntityType<IHHeaterBlockEntity>> IH_HEATER_BE =
            BLOCK_ENTITIES.register("ih_heater_be", () ->
                    BlockEntityType.Builder.of(IHHeaterBlockEntity::new,
                            ModBlocks.IH_HEATER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
