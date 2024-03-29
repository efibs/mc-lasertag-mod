package de.pewpewproject.lasertag.networking.client.callbacks;

import de.pewpewproject.lasertag.LasertagMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

/**
 * Callback to handle the team eliminated network event
 *
 * @author Étienne Muser
 */
public class TeamEliminatedCallback implements ClientPlayNetworking.PlayChannelHandler {
    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {

        try {
            // Get the game managers
            var gameManager = client.world.getClientLasertagManager();
            var eliminationManager = gameManager.getEliminationManager();

            // Get the values from the buffer
            var teamId = buf.readInt();
            var teamSurviveTime = buf.readLong();

            eliminationManager.setTeamEliminated(teamId);
            eliminationManager.setTeamSurviveTime(teamId, teamSurviveTime);
        } catch (Exception ex) {
            LasertagMod.LOGGER.error("Error in TeamEliminatedCallback", ex);
            throw ex;
        }
    }
}
