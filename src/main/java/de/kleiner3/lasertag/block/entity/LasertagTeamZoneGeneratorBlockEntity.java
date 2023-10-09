package de.kleiner3.lasertag.block.entity;

import de.kleiner3.lasertag.common.types.Tuple;
import de.kleiner3.lasertag.entity.Entities;
import de.kleiner3.lasertag.lasertaggame.management.LasertagGameManager;
import de.kleiner3.lasertag.lasertaggame.management.team.TeamDto;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Étienne Muser
 */
public class LasertagTeamZoneGeneratorBlockEntity extends BlockEntity implements LasertagCustomBlockTickable {

    private int radius = 5;
    private int height = 10;
    private String teamName = "";
    private final List<Tuple<BlockPos, Direction>> borderCache;
    private final Set<BlockPos> zoneCache;

    public LasertagTeamZoneGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(Entities.LASERTAG_TEAM_ZONE_GENERATOR_BLOCK_ENTITY, pos, state);

        borderCache = new ArrayList<>();
        zoneCache = new HashSet<>();
    }

    public void serverTick(ServerWorld world) {

        // Do nothing if a game is running
        if (world.getServer().getLasertagServerManager().isGameRunning()) {
            return;
        }

        // Get all players
        var players = world.getPlayers();

        // For every player
        playerLoop: for (var player : players) {

            // Get the players bounding box
            var playerBoundingBox = player.getBoundingBox();

            // Go through every border block to check if player intersects the border
            for (var borderBlock : this.borderCache) {

                // Get the blocks position
                var blockPos = borderBlock.x();

                // Build the block bounding box
                var blockBoundingBox = new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                                           blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1);

                // If player intersects the border
                if (playerBoundingBox.intersects(blockBoundingBox)) {

                    // Get the team
                    var teamOptional = LasertagGameManager.getInstance().getTeamManager().getTeamConfigManager().getTeamOfName(this.teamName);

                    // Join the team
                    teamOptional.ifPresent(team -> playerJoinTeamIfNecessary(world, player, team));

                    // Continue with next player
                    continue playerLoop;
                }
            }

            // Go through every border block again to check if the player intersects the first block outside the border.
            // Since the first loop went fully through, the player does not intersect the border. If the player
            // intersects only the first block outside the border and not the border itself, he will be thrown out of
            // his team.
            for (var borderBlock : this.borderCache) {
                // Get the block position of the first block outside the border
                var blockPos = borderBlock.x().add(borderBlock.y().getVector());

                // build teh block bounding box
                var blockBoundingBox = new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                                           blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1);

                // If player intersects the border
                if (playerBoundingBox.intersects(blockBoundingBox)) {

                    // Get the team
                    var teamOptional = LasertagGameManager.getInstance().getTeamManager().getTeamOfPlayer(player.getUuid());

                    // Leave the team
                    teamOptional.ifPresent(team -> {
                        LasertagGameManager.getInstance().getTeamManager().playerLeaveHisTeam(world, player.getUuid());
                        player.getInventory().clear();
                        player.sendMessage(Text.literal("You left your team"), true);
                    });

                    // Continue with next player
                    continue playerLoop;
                }
            }
        }
    }

    private static void playerJoinTeamIfNecessary(ServerWorld world, PlayerEntity player, TeamDto team) {

        // Get the team manager
        var teamManager = LasertagGameManager.getInstance().getTeamManager();

        // Get the players team
        var playersTeamOptional = teamManager.getTeamOfPlayer(player.getUuid());

        // If player is in a team
        if (playersTeamOptional.isPresent()) {
            // If player is already in the team, do nothing
            if (playersTeamOptional.get().equals(team)) {
                return;
            }
        }

        // Join team
        var joinSucceeded = teamManager.playerJoinTeam(world, team, player);

        // If join did not succeed
        if (!joinSucceeded) {
            player.sendMessage(Text.literal("That team is already full.").formatted(Formatting.RED), false);
        } else {
            player.sendMessage(Text.literal("You joined team " + team.name()), true);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {

        nbt.putInt("radius", this.radius);
        nbt.putInt("height", this.height);
        nbt.putString("teamName", this.teamName);

        var borderList = new NbtList();

        this.borderCache.forEach(border -> {
            var nbtBorder = new NbtCompound();
            nbtBorder.putInt("x", border.x().getX());
            nbtBorder.putInt("y", border.x().getY());
            nbtBorder.putInt("z", border.x().getZ());
            nbtBorder.putInt("direction", border.y().ordinal());
            borderList.add(nbtBorder);
        });

        nbt.put("borders", borderList);

        super.writeNbt(nbt);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);

        if (world.isClient) {
            return;
        }

        // Register this as a ticker
        world.getServer().getLasertagServerManager().getBlockTickManager().registerTicker(this);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.borderCache.clear();

        super.readNbt(nbt);

        this.radius = nbt.getInt("radius");
        this.height = nbt.getInt("height");
        this.teamName = nbt.getString("teamName");

        var borderList = nbt.getList("borders", NbtCompound.COMPOUND_TYPE);

        borderList.forEach(nbtElement -> {
            var nbtBorder = (NbtCompound)nbtElement;

            var x = nbtBorder.getInt("x");
            var y = nbtBorder.getInt("y");
            var z = nbtBorder.getInt("z");
            var directionOrdinal = nbtBorder.getInt("direction");

            var blockPos = new BlockPos(x, y, z);
            var direction = Direction.values()[directionOrdinal];

            this.borderCache.add(new Tuple<>(blockPos, direction));
        });
    }

    public String getTeamName() {
        return this.teamName;
    }

    public void setTeamName(String newTeamName) {
        this.teamName = newTeamName;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    public void openScreen(PlayerEntity player) {
        if (player.getEntityWorld().isClient) {
            player.openLasertagTeamZoneGeneratorScreen(this);
        }
    }

    public void generateZone() {

        // Clear borders cache
        this.borderCache.clear();

        // get the world the entity is in
        var world = this.getWorld();

        // If it is not a server world, do nothing
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        // Get the first block state and save it as the search block state
        var searchBlock = serverWorld.getBlockState(this.pos.add(0, 1, 0));

        // Get the boundaries
        var entityPos = this.getPos();
        var minX = entityPos.getX() - this.radius;
        var maxX = entityPos.getX() + this.radius;
        var minZ = entityPos.getZ() - this.radius;
        var maxZ = entityPos.getZ() + this.radius;
        var minY = Math.max(entityPos.getY() - this.height, -64);
        var maxY = Math.min(entityPos.getX() + this.height, 320);

        // Initialize the stack
        var stack = new Stack<BlockPos>();

        // Clear the zone cache
        this.zoneCache.clear();

        // Put the first element on the stack
        var pos = this.pos.add(0, 1, 0);
        stack.push(pos);

        while (!stack.isEmpty()) {
            var nextPos = stack.pop();

            // Check if this pos has already been added
            if (this.zoneCache.contains(nextPos)) {
                continue;
            }

            // Add the block to the set
            this.zoneCache.add(nextPos);

            for (var direction : Direction.values()) {
                var addedPos = nextPos.add(direction.getVector());

                // Get the block state
                var blockState = serverWorld.getBlockState(addedPos);

                // Check boundaries
                if (addedPos.getX() < minX ||
                    addedPos.getX() > maxX ||
                    addedPos.getZ() < minZ ||
                    addedPos.getZ() > maxZ ||
                    addedPos.getY() < minY ||
                    addedPos.getY() > maxY) {

                    // We are at a border. Check if the next block is also the search block or air
                    if (blockState.equals(searchBlock) || blockState.equals(Blocks.AIR.getDefaultState())) {
                        // If the next block is also a search block or air, this block needs to be rendered.
                        // Save this face in the border cache
                        borderCache.add(new Tuple<>(nextPos, direction));
                    }

                    continue;
                }

                // If is not the search block
                if (!blockState.equals(searchBlock)) {
                    continue;
                }

                // Add the new block bos to the stack
                stack.push(addedPos);
            }
        }

        // Get the block state at this position
        var blockState = world.getBlockState(this.pos);

        // Notify clients that the block entity has changed
        serverWorld.updateListeners(this.pos, blockState, blockState, Block.NOTIFY_LISTENERS);
    }

    public List<Tuple<BlockPos, Direction>> getBorderCache() {
        return this.borderCache;
    }
}
