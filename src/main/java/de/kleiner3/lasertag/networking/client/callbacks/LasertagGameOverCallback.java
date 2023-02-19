package de.kleiner3.lasertag.networking.client.callbacks;

import de.kleiner3.lasertag.client.hud.LasertagHudOverlay;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

/**
 * Callback to handle the lasertag game over network event
 *
 * @author Étienne Muser
 */
public class LasertagGameOverCallback implements ClientPlayNetworking.PlayChannelHandler {
    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        synchronized (LasertagHudOverlay.renderData.gameTimerLock) {
            if (LasertagHudOverlay.renderData.gameTimer != null) {
                LasertagHudOverlay.renderData.gameTimer.shutdown();
                LasertagHudOverlay.renderData.gameTimer = null;
                LasertagHudOverlay.renderData.gameTime = 0;
            }
        }

        LasertagHudOverlay.renderData.shouldRenderNameTags = true;
    }
}