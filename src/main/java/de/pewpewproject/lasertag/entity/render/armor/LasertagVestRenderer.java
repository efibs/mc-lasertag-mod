package de.pewpewproject.lasertag.entity.render.armor;

import de.pewpewproject.lasertag.item.Items;
import de.pewpewproject.lasertag.item.LasertagVestItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;
import software.bernie.geckolib3.util.GeoUtils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Renderer for the lasertag vest
 *
 * @author Étienne Muser
 */
public class LasertagVestRenderer extends GeoArmorRenderer<LasertagVestItem> {

    private static final LasertagVestModel VEST_MODEL = new LasertagVestModel();
    private static final LasertagVestLightsModel LIGHTS_MODEL = new LasertagVestLightsModel();

    public LasertagVestRenderer() {
        super(VEST_MODEL);

        this.headBone = "armorHead";
        this.bodyBone = "armorBody";
        this.rightArmBone = "armorRightArm";
        this.leftArmBone = "armorLeftArm";
        this.rightLegBone = "armorRightLeg";
        this.leftLegBone = "armorLeftLeg";
        this.rightBootBone = "armorRightBoot";
        this.leftBootBone = "armorLeftBoot";
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack, LivingEntity entity,
                       EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> contextModel) {
        this.setModel(VEST_MODEL);
        super.render(matrices, vertexConsumers, stack, entity, slot, light, contextModel);

        this.setModel(LIGHTS_MODEL);
        RenderLayer cameo = RenderLayer.getEyes(LIGHTS_MODEL.getTextureResource(null));
        matrices.push();

        matrices.translate(0.0D, 1.497F, 0.0D);
        matrices.scale(-1.005F, -1.0F, 1.005F);

        var lightsModel = LIGHTS_MODEL.getModel(LIGHTS_MODEL.getModelResource(null));

        if (this.bodyBone != null) {
            IBone bodyBone = LIGHTS_MODEL.getBone(this.bodyBone);
            GeoUtils.copyRotations(contextModel.body, bodyBone);
            bodyBone.setPositionX(contextModel.body.pivotX);
            bodyBone.setPositionY(-contextModel.body.pivotY);
            bodyBone.setPositionZ(contextModel.body.pivotZ);
        }

        // Default color is black
        AtomicReference<Float> r = new AtomicReference<>((float) 0);
        AtomicReference<Float> g = new AtomicReference<>((float) 0);
        AtomicReference<Float> b = new AtomicReference<>((float) 0);

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

        this.render(lightsModel, (LasertagVestItem) Items.LASERTAG_VEST, 1.0F, cameo, matrices, vertexConsumers, vertexConsumers.getBuffer(cameo), 255, OverlayTexture.DEFAULT_UV, r.get(), g.get(), b.get(), 1.0F);
        matrices.pop();
    }
}
