package de.kleiner3.lasertag.networking.client.callbacks;

import de.kleiner3.lasertag.LasertagMod;
import de.kleiner3.lasertag.client.screen.LasertagGameManagerScreen;
import de.kleiner3.lasertag.client.screen.LasertagGameManagerSettingsScreen;
import de.kleiner3.lasertag.lasertaggame.gamemode.GameModes;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

/**
 * Callback for the game mode sync network event
 *
 * @author Étienne Muser
 */
public class GameModeSyncCallback implements ClientPlayNetworking.PlayChannelHandler {
    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {

        try {

            // Get the game managers
            var gameManager = client.world.getClientLasertagManager();
            var gameModeManager = gameManager.getGameModeManager();

            // Get the game mode translatable name
            var gameModeTranslatableName = buf.readString();

            // Get the new game mode
            var newGameMode = GameModes.GAME_MODES.get(gameModeTranslatableName);

            // Set the new game mode
            gameModeManager.setGameMode(newGameMode);

            // If client has game manager screen open
            if (client.currentScreen instanceof LasertagGameManagerScreen lasertagGameManagerScreen) {
                lasertagGameManagerScreen.reloadGameMode();
            } else if (client.currentScreen instanceof LasertagGameManagerSettingsScreen lasertagGameManagerSettingsScreen) {
                lasertagGameManagerSettingsScreen.resetList();
            }
        } catch (Exception ex) {
            LasertagMod.LOGGER.error("Error in GameModeSyncCallback", ex);
            throw ex;
        }
    }
}
