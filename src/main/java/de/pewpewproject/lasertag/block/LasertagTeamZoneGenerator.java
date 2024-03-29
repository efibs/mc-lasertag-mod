package de.pewpewproject.lasertag.block;

import de.pewpewproject.lasertag.block.entity.LasertagCustomBlockTickable;
import de.pewpewproject.lasertag.block.entity.LasertagTeamZoneGeneratorBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Étienne Muser
 */
public class LasertagTeamZoneGenerator extends BlockWithEntity {

    protected LasertagTeamZoneGenerator(Settings settings) {
        super(settings);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {

        // If is not on the server
        if (!(world instanceof ServerWorld serverWorld)) {

            return;
        }

        // If was not a state change that resulted in the block being broken
        if (state.isOf(newState.getBlock())) {
            return;
        }

        // Get the entity
        var blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof LasertagTeamZoneGeneratorBlockEntity teamZoneGeneratorBlockEntity) {

            // Get the block tick manager
            var blockTickManager = serverWorld.getServerLasertagManager().getBlockTickManager();

            // Unregister the block entity ticker
            blockTickManager.unregisterTicker((LasertagCustomBlockTickable) blockEntity);
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {

        // If player does not have permission to start the game
        if (!player.hasPermissionLevel(1)) {

            // Send feedback
            player.sendMessage(Text.translatable("chat.message.team_zone_generator_not_enough_permissions").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof LasertagTeamZoneGeneratorBlockEntity lasertagTeamZoneGeneratorBlockEntity) {
            lasertagTeamZoneGeneratorBlockEntity.openScreen(player);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LasertagTeamZoneGeneratorBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {

        // Get the stacks nbt data
        var nbt = BlockItem.getBlockEntityNbt(stack);

        // Sanity check
        if (nbt == null) {
            return;
        }

        var teamName = "Not given";
        var radiusString = "Not given";
        var heightString = "Not given";

        if (nbt.contains("teamName")) {
            teamName = nbt.getString("teamName");
        }

        if (nbt.contains("radius")) {
            radiusString = String.valueOf(nbt.getInt("radius"));
        }

        if (nbt.contains("height")) {
            heightString = String.valueOf(nbt.getInt("height"));
        }

        tooltip.add(Text.literal("Team name: " + teamName));
        tooltip.add(Text.literal("Radius: " + radiusString));
        tooltip.add(Text.literal("Height: " + heightString));
    }
}
