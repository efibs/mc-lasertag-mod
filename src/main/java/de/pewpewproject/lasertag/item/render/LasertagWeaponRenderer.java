package de.pewpewproject.lasertag.item.render;

import com.mojang.blaze3d.systems.RenderSystem;
import de.pewpewproject.lasertag.item.LasertagWeaponItem;
import de.pewpewproject.lasertag.item.model.LasertagWeaponLightsModel;
import de.pewpewproject.lasertag.item.model.LasertagWeaponModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Renderer for the lasertarget
 *
 * @author Étienne Muser
 */
public class LasertagWeaponRenderer extends GeoItemRenderer<LasertagWeaponItem> {
    private static final LasertagWeaponModel MODEL = new LasertagWeaponModel();

    private static final LasertagWeaponLightsModel  LIGHTS_MODEL = new LasertagWeaponLightsModel();

    public LasertagWeaponRenderer() {
        super(MODEL);
    }

    @Override
    public void render(LasertagWeaponItem animatable, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int packedLight, ItemStack stack) {
        this.modelProvider = MODEL;
        super.render(animatable, matrices, vertexConsumers, packedLight, stack);

        this.modelProvider = LIGHTS_MODEL;

        RenderLayer cameo = RenderLayer.getEyes(LIGHTS_MODEL.getTextureResource(null));
        matrices.push();

        matrices.translate(0.5F, 0.51F, 0.5F);

        var lightsModel = LIGHTS_MODEL.getModel(LIGHTS_MODEL.getModelResource(null));

        // Default color is black
        AtomicReference<Float> r = new AtomicReference<>((float) 0);
        AtomicReference<Float> g = new AtomicReference<>((float) 0);
        AtomicReference<Float> b = new AtomicReference<>((float) 0);

        var entity = stack.getHolder();

        // If player is activated
        if (entity instanceof PlayerEntity) {

            // Get the game managers
            var gameManager = MinecraftClient.getInstance().world.getClientLasertagManager();
            var activationManager = gameManager.getActivationManager();
            var teamsManager = gameManager.getTeamsManager();
            var teamsConfigState = gameManager.getSyncedState().getTeamsConfigState();

            boolean isDeactivated = activationManager.isDeactivated(entity.getUuid());

            if (!isDeactivated) {
                teamsManager.getTeamOfPlayer(entity.getUuid()).ifPresent(teamId -> {

                    var team = teamsConfigState.getTeamOfId(teamId).orElseThrow();

                    var color = team.color();

                    r.set(color.r() / 255.0F);
                    g.set(color.g() / 255.0F);
                    b.set(color.b() / 255.0F);
                });
            }
        }

        RenderSystem.setShaderTexture(0, LIGHTS_MODEL.getTextureResource(null));

        this.render(lightsModel, animatable, 1.0F, cameo, matrices, vertexConsumers, vertexConsumers.getBuffer(cameo), 255, OverlayTexture.DEFAULT_UV, r.get(), g.get(), b.get(), 1.0F);
        matrices.pop();
    }
}
