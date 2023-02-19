package de.kleiner3.lasertag.networking.client.callbacks;

import de.kleiner3.lasertag.client.hud.LasertagHudOverlay;
import de.kleiner3.lasertag.lasertaggame.timing.PreGameCountDownTimerTask;
import de.kleiner3.lasertag.lasertaggame.settings.LasertagSettingsManager;
import de.kleiner3.lasertag.lasertaggame.settings.SettingNames;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Callback for the lasertag game started network event
 *
 * @author Étienne Muser
 */
public class LasertagGameStartedCallback implements ClientPlayNetworking.PlayChannelHandler {
    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        // TODO: Assert that this method does nothing if game is already running
        LasertagHudOverlay.renderData.progress = 0.0;
        LasertagHudOverlay.renderData.startingIn = (int)(long) LasertagSettingsManager.get(SettingNames.START_TIME);
        LasertagHudOverlay.renderData.shouldRenderNameTags = false;

        // Start pregame count down timer
        var preGameCountDownTimer = Executors.newSingleThreadScheduledExecutor();
        preGameCountDownTimer.scheduleAtFixedRate(new PreGameCountDownTimerTask(preGameCountDownTimer), 1, 1, TimeUnit.SECONDS);
    }
}