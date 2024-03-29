package de.pewpewproject.lasertag.networking.server.callbacks;

import de.pewpewproject.lasertag.LasertagMod;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Callback to handle when a client hit another player
 *
 * @author Étienne Muser
 */
public class PlayerHitPlayerCallback implements ServerPlayNetworking.PlayChannelHandler {
    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {

        try {

            // Get the game managers
            var gameManager = server.getOverworld().getServerLasertagManager();

            var playerUuid = buf.readUuid();
            var targetUuid = buf.readUuid();

            gameManager.playerHitPlayer(playerUuid, targetUuid);
        } catch (Exception ex) {
            LasertagMod.LOGGER.error("Error in PlayerHitPlayerCallback", ex);
            throw ex;
        }
    }
}
