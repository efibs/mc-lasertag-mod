package de.pewpewproject.lasertag.networking.server.callbacks;

import de.pewpewproject.lasertag.LasertagMod;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Callback for the client trigger save settings preset network event
 *
 * @author Étienne Muser
 */
public class ClientTriggerSavePresetCallback implements ServerPlayNetworking.PlayChannelHandler {
    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {

        try {

            // Get the game managers
            var gameManager = server.getOverworld().getServerLasertagManager();
            var settingsPresetsManager = gameManager.getSettingsPresetsManager();
            var settingsPresetsNamesManger = gameManager.getSettingsPresetsNameManager();

            // Read name from buffer
            var presetName = buf.readString();

            settingsPresetsManager.savePreset(presetName);
            settingsPresetsNamesManger.addPresetName(presetName);
        } catch (Exception ex) {
            LasertagMod.LOGGER.error("Error in ClientTriggerSavePresetCallback", ex);
            throw ex;
        }
    }
}
