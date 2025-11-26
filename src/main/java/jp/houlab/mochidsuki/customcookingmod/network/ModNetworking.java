package jp.houlab.mochidsuki.customcookingmod.network;

import jp.houlab.mochidsuki.customcookingmod.CustomcookingmodMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Network handler for CustomCookingMod
 * Manages packet registration and sending
 */
public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CustomcookingmodMain.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        // Client to Server packets
        CHANNEL.messageBuilder(RecipeGenerationRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RecipeGenerationRequestPacket::decode)
                .encoder(RecipeGenerationRequestPacket::encode)
                .consumerMainThread(RecipeGenerationRequestPacket::handle)
                .add();

        // Server to Client packets
        CHANNEL.messageBuilder(RecipeGenerationResponsePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RecipeGenerationResponsePacket::decode)
                .encoder(RecipeGenerationResponsePacket::encode)
                .consumerMainThread(RecipeGenerationResponsePacket::handle)
                .add();
    }

    /**
     * Send packet to server
     */
    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }

    /**
     * Send packet to specific player
     */
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /**
     * Send packet to all players
     */
    public static <MSG> void sendToAllPlayers(MSG message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }
}
