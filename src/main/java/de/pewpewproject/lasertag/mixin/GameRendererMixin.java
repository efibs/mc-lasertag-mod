package de.pewpewproject.lasertag.mixin;

import de.pewpewproject.lasertag.block.Blocks;
import de.pewpewproject.lasertag.client.KeyBindings;
import de.pewpewproject.lasertag.item.Items;
import de.pewpewproject.lasertag.lasertaggame.settings.SettingDescription;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Mixin into the GameRenderer.class
 *
 * @author Étienne Muser
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    public abstract MinecraftClient getClient();

    /**
     * Redirect the renderWorld() in render() to always render the outline of the lasertag flag block
     * even tho the player is in adventure game mode
     *
     * @param instance The world renderer
     * @param matrices The matrix stack used for rendering
     * @param tickDelta
     * @param limitTime
     * @param renderBlockOutline
     * @param camera
     * @param gameRenderer
     * @param lightmapTextureManager
     * @param positionMatrix
     */
    @Redirect(method = "renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;FJZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/util/math/Matrix4f;)V"))
    private void onRender(WorldRenderer instance, MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix) {

        // Get the client
        var client = this.getClient();

        // Get the crosshair target
        var hitResult = client.crosshairTarget;

        // Sanity check
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {

            // Get the targetet block state
            var blockState = client.world.getBlockState(((BlockHitResult)hitResult).getBlockPos());

            // If the block is a lasertag flag
            if (blockState.isOf(Blocks.LASERTAG_FLAG_BLOCK)) {

                // Always render the flag outline
                renderBlockOutline = true;
            }
        }

        // Call render on the world renderer
        client.worldRenderer.render(matrices, tickDelta, limitTime, renderBlockOutline, camera, (GameRenderer)(Object)this, lightmapTextureManager, positionMatrix);
    }

    /**
     * Implements the laserweapon zoom
     *
     * @param camera
     * @param tickDelta
     * @param changingFov
     * @param cir
     * @param d The current calculated FOV
     */
    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D", at = @At("TAIL"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir, double d) {

        // Get the minecraft client
        var client = MinecraftClient.getInstance();

        // Get if the player is holding a lasertag weapon
        var playerIsHoldingWeapon = client.player.isHolding(Items.LASERTAG_WEAPON);

        // If the player is holding a weapon
        if (KeyBindings.isWeaponZoomPressed() && playerIsHoldingWeapon) {

            // Get the weapon zoom setting
            var zoomFactor = client.world.getClientLasertagManager().getSettingsManager().<Long>get(SettingDescription.WEAPON_ZOOM);

            // Calculate the new fov
            var newFov = d / zoomFactor;

            // Change fov to zoom in
            cir.setReturnValue(newFov);
        }
    }
}
