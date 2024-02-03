package de.kleiner3.lasertag.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into the ServerPlayerEntity.class
 *
 * @author Étienne Muser
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {

        ServerPlayerEntity player = ((ServerPlayerEntity)(Object)this);

        // Get the game managers
        var gameManager = player.getWorld().getServerLasertagManager();
        var gameModeManager = gameManager.getGameModeManager();

        // Get the server
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.execute(() -> gameModeManager.getGameMode().onPlayerDeath(server, player, damageSource));
        }
    }
}
