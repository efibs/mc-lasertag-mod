package de.pewpewproject.lasertag.lasertaggame.state.management.server.implementation;

import de.pewpewproject.lasertag.LasertagMod;
import de.pewpewproject.lasertag.lasertaggame.arena.ArenaBoundsDto;
import de.pewpewproject.lasertag.lasertaggame.state.management.server.IArenaManager;
import de.pewpewproject.lasertag.lasertaggame.state.management.server.IBlockTickManager;
import de.pewpewproject.lasertag.lasertaggame.state.management.server.ISpawnpointManager;
import de.pewpewproject.lasertag.lasertaggame.state.synced.IPlayerNamesState;
import de.pewpewproject.lasertag.networking.NetworkingConstants;
import de.pewpewproject.lasertag.networking.server.ServerEventSending;
import de.pewpewproject.lasertag.worldgen.chunkgen.ArenaChunkGenerator;
import de.pewpewproject.lasertag.worldgen.chunkgen.ArenaChunkGeneratorConfig;
import de.pewpewproject.lasertag.worldgen.chunkgen.template.ArenaTemplate;
import de.pewpewproject.lasertag.worldgen.chunkgen.template.TemplateRegistry;
import de.pewpewproject.lasertag.worldgen.chunkgen.type.ArenaType;
import de.pewpewproject.lasertag.worldgen.chunkgen.type.ProceduralArenaType;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Clearable;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Implementation of IArenaManager for the server lasertag game
 *
 * @author Étienne Muser
 */
public class ArenaManager implements IArenaManager {

    private final MinecraftServer server;
    private final ISpawnpointManager spawnpointManager;
    private final IPlayerNamesState playerNamesState;
    private final IBlockTickManager blockTickManager;

    private boolean isLoading = false;

    public ArenaManager(MinecraftServer server,
                        ISpawnpointManager spawnpointManager,
                        IPlayerNamesState playerNamesState,
                        IBlockTickManager blockTickManager) {
        this.server = server;
        this.spawnpointManager = spawnpointManager;
        this.playerNamesState = playerNamesState;
        this.blockTickManager = blockTickManager;
    }

    //region Public methods

    @Override
    public boolean loadArena(ArenaType newArenaType, ProceduralArenaType newProceduralArenaType) {

        // Start the generation
        LasertagMod.LOGGER.info("Starting to load new arena '" + newArenaType.translatableName + "(" + newProceduralArenaType.translatableName + ")'");

        // Check if this is an arena world
        var chunkGenerator = Objects.requireNonNull(server.getSaveProperties()
                .getGeneratorOptions()
                .getDimensions()
                .get(DimensionOptions.OVERWORLD))
                .chunkGenerator;

        // If the chunk generator is not an arena chunk generator
        if (!(chunkGenerator instanceof ArenaChunkGenerator arenaChunkGenerator)) {
            LasertagMod.LOGGER.warn("Cannot reload map in non-arena world");
            return false;
        }

        var singleThreadExecutor = Executors.newSingleThreadExecutor();
        singleThreadExecutor.execute(() -> loadArenaInner(arenaChunkGenerator, newArenaType, newProceduralArenaType));

        return true;
    }

    private void loadArenaInner(ArenaChunkGenerator arenaChunkGenerator,
                                ArenaType newArenaType,
                                ProceduralArenaType newProceduralArenaType) {

        // Show loading screen
        this.sendMapLoadProgressEvent("Starting the generation of the arena", 0);
        this.isLoading = true;

        // Can be called on the relieveExecutor to relieve the server thread
        Runnable relieveAction = () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        };

        // Create old arena template
        var oldArenaType = arenaChunkGenerator.getConfig().getType();
        var oldArenaProceduralType = arenaChunkGenerator.getConfig().getProceduralType();
        var oldArenaTemplate = TemplateRegistry.getTemplate(oldArenaType, oldArenaProceduralType, arenaChunkGenerator.getConfig().getSeed());

        // Start time measurement
        var blockPlaceStartTime = System.currentTimeMillis();

        // Teleport all players back to origin for their own safety
        server.execute(this::teleportPlayersToOrigin);

        // Remove all blocks of the old arena
        var oldArenaBounds = this.calculateBounds(oldArenaTemplate);
        server.execute(() -> this.removeOldBlocks(oldArenaBounds));

        // Clear spawn-point cache if necessary and reset arena structure placer
        if (!oldArenaType.equals(newArenaType) || oldArenaType.equals(ArenaType.PROCEDURAL)) {
            spawnpointManager.clearSpawnpointCache();
        }

        // Remove all entities except the players
        server.execute(this::removeEntities);

        // Relieve The server thread by waiting
        relieveAction.run();

        // Set new arena chunk generator config
        var newSeed = new Random().nextLong();
        arenaChunkGenerator.setConfig(new ArenaChunkGeneratorConfig(newArenaType.ordinal(), newProceduralArenaType.ordinal(), newSeed));
        var newArenaTemplate = TemplateRegistry.getTemplate(newArenaType, newProceduralArenaType, newSeed);

        // Generate new arena
        var newArenaBounds = this.calculateBounds(newArenaTemplate);
        generateArena(arenaChunkGenerator, newArenaBounds, relieveAction);

        // Set biomes
        server.execute(() -> this.setBiomes(newArenaBounds, arenaChunkGenerator));

        // Teleport players back to origin
        server.execute(this::teleportPlayersToOrigin);

        // Calculate the union of the new and old arena
        var updateBounds = this.calculateUnion(oldArenaBounds, newArenaBounds);

        // Execute final step
        server.execute(() -> {

            // Final logging
            var blockPlaceDuration = System.currentTimeMillis() - blockPlaceStartTime;
            LasertagMod.LOGGER.info(String.format(Locale.ROOT, "Arena loaded. This took %d ms for %d chunks, or %02f ms per chunk", blockPlaceDuration, updateBounds.numChunks(), (float) blockPlaceDuration / (float) updateBounds.numChunks()));

            // Stop loading
            this.sendMapLoadProgressEvent("", -1.0);
            this.isLoading = false;
        });
    }

    @Override
    public boolean isLoading() {
        return this.isLoading;
    }

    //endregion

    //region Private methods

    /**
     * Teleports all players in the world to the origin.
     * Ignores offline players.
     */
    private void teleportPlayersToOrigin() {

        playerNamesState.forEachPlayer(playerUuid -> {
            var player = server.getPlayerManager().getPlayer(playerUuid);

            // Sanity check
            if (player == null) {
                // Don't handle offline players
                return;
            }

            player.requestTeleport(0.5F, 1, 0.5F);
        });
    }

    /**
     * Removes all blocks int the given arena bounds
     *
     * @param oldArenaBounds The bounds in which to remove all blocks
     */
    private void removeOldBlocks(ArenaBoundsDto oldArenaBounds) {

        // Reset custom block tickers
        blockTickManager.clear();

        // Get chunk manager
        var serverWorld = server.getOverworld();
        var serverChunkManager = serverWorld.getChunkManager();

        // Init progress variables
        var currentStepString = "Removing blocks from old arena";
        this.sendMapLoadProgressEvent(currentStepString, 0.0);
        var removeBlocksChunkIndex = new AtomicInteger(0);

        // Remove all blocks of the old arena
        forEachChunk(oldArenaBounds, (chunkX, chunkZ) -> {

            // Get the chunk position
            var chunkPos = new ChunkPos(chunkX, chunkZ);

            // Get the world chunk
            var worldChunk = serverChunkManager.getWorldChunk(chunkX, chunkZ, true);

            // If the chunk could not be retrieved
            if (worldChunk == null) {

                LasertagMod.LOGGER.warn("Load arena, remove old arena - Could not get chunk (" + chunkX + ", " + chunkZ + ")");
                return;
            }

            // Get the block positions of every block in the chunk
            var blockPositions = BlockPos.iterate(chunkPos.getStartX(),
                    serverWorld.getBottomY(),
                    chunkPos.getStartZ(),
                    chunkPos.getEndX(),
                    serverWorld.getTopY() - 1,
                    chunkPos.getEndZ());

            // For every block in the chunk
            for (var blockPos : blockPositions) {

                // get the current block state
                var currentBlockState = serverWorld.getBlockState(blockPos);

                // If is already air
                if (currentBlockState.equals(Blocks.AIR.getDefaultState())) {
                    // Continue with the next block
                    continue;
                }

                // Clear block entity
                var blockEntity = serverWorld.getBlockEntity(blockPos);
                Clearable.clear(blockEntity);

                // Set block to air. Block.FORCE_STATE so that no items drop (Flowers, seeds, etc)
                serverWorld.setBlockState(blockPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }

            worldChunk.clear();

            this.sendMapLoadProgressEvent(currentStepString,
                    (double) (removeBlocksChunkIndex.incrementAndGet()) / (double) oldArenaBounds.numChunks());
        });
    }

    /**
     * Removes all entities except players from the world
     */
    private void removeEntities() {

        server.getOverworld()
                .getEntitiesByType(TypeFilter.instanceOf(Entity.class), e -> !(e instanceof PlayerEntity))
                .forEach(Entity::discard);
    }

    /**
     * Generates the new arena.
     *
     * @param newArenaBounds The bounds of the new arena
     */
    private void generateArena(ChunkGenerator chunkGenerator,
                               ArenaBoundsDto newArenaBounds,
                               Runnable relieveAction) {

        // Get chunk manager
        var serverWorld = server.getOverworld();
        var serverChunkManager = serverWorld.getChunkManager();
        serverChunkManager.threadedAnvilChunkStorage.verifyChunkGenerator();

        // Init progress variables
        var currentStepChunkIndex = new AtomicInteger(0);
        var currentStepString = "Generating new arena";
        this.sendMapLoadProgressEvent(currentStepString, 0.0);

        // Start time measurement for this step
        var stepStartTime = System.currentTimeMillis();

        // Create counter
        var indexCounter = new AtomicInteger(0);

        // For each chunk
        forEachChunk(newArenaBounds, (chunkX, chunkZ) -> {

            // Get the world chunk
            var worldChunk = serverChunkManager.getWorldChunk(chunkX, chunkZ, true);

            // If the world chunk could not be retrieved
            if (worldChunk == null) {
                LasertagMod.LOGGER.warn("Load arena - Could not get chunk (" + chunkX + ", " + chunkZ + ")");
                return;
            }

            // Generation step for this chunk finished -> Send progress event
            server.execute(() -> {

                // Generate arena
                chunkGenerator.generateFeatures(serverWorld,
                        worldChunk,
                        null);

                // Spawn entities
                chunkGenerator.populateEntities(new ChunkRegion(serverWorld,
                        List.of(worldChunk),
                        ChunkStatus.FULL,
                        -1));

                // Send progress update
                this.sendMapLoadProgressEvent(currentStepString,
                        (double) currentStepChunkIndex.incrementAndGet() / (double) newArenaBounds.numChunks());
            });

            // If this was the 50th Chunk
            if (indexCounter.incrementAndGet() >= 50) {

                // Reset the counter
                indexCounter.set(0);

                // Relieve the server
                relieveAction.run();
            }
        });

        // Final logging
        server.execute(() -> LasertagMod.LOGGER.info("Generating Arena took " + (System.currentTimeMillis() - stepStartTime) + " ms"));
    }

    /**
     * Sets the biomes of the chunks of the new arena
     *
     * @param newArenaBounds      The bounds of the new arena
     * @param arenaChunkGenerator The arena chunk generator
     */
    private void setBiomes(ArenaBoundsDto newArenaBounds, ChunkGenerator arenaChunkGenerator) {

        // Init progress variables
        var currentStepString = "Setting biomes";
        this.sendMapLoadProgressEvent(currentStepString, 0.0);
        var markUpdateChunkIndex = new AtomicInteger(0);

        // Get chunk manager
        var serverWorld = server.getOverworld();
        var serverChunkManager = serverWorld.getChunkManager();

        // For each chunk
        forEachChunk(newArenaBounds, (chunkX, chunkZ) -> {

            var chunkPos = new ChunkPos(chunkX, chunkZ);
            var worldChunk = serverChunkManager.getWorldChunk(chunkX, chunkZ, false);

            // If the chunk could not be retrieved
            if (worldChunk == null) {
                LasertagMod.LOGGER.warn("Load arena, set biomes - Could not get chunk (" + chunkX + ", " + chunkZ + ")");
                return;
            }

            // Set the biome
            worldChunk.populateBiomes(arenaChunkGenerator.getBiomeSource(),
                    serverWorld.getChunkManager().getNoiseConfig().getMultiNoiseSampler());

            // Reload chunks on the clients to update biome
            for (var player : serverWorld.getPlayers()) {

                player.sendUnloadChunkPacket(chunkPos);
                serverChunkManager.threadedAnvilChunkStorage
                        .sendChunkDataPackets(player, new MutableObject<>(), worldChunk);
            }

            this.sendMapLoadProgressEvent(currentStepString,
                    (double) (markUpdateChunkIndex.incrementAndGet()) / (double) newArenaBounds.numChunks());
        });
    }

    /**
     * Calculates the chunk bounds (startX, startZ, endX, endZ and number of chunks)
     * for the given arena type
     *
     * @param template The arena template to calculate the bounds for
     * @return The calculated arena bounds dto
     */
    private ArenaBoundsDto calculateBounds(ArenaTemplate template) {

        var arenaSize = template.getArenaSize();
        Vec3i arenaOffset = template.getPlacementOffset();
        var startZ = -arenaOffset.getZ();
        var startX = -arenaOffset.getX();
        var endZ = startZ + arenaSize.getZ();
        var endX = startX + arenaSize.getX();
        var startChunkZ = ChunkSectionPos.getSectionCoord(startZ);
        var startChunkX = ChunkSectionPos.getSectionCoord(startX);
        var endChunkZ = ChunkSectionPos.getSectionCoord(endZ);
        var endChunkX = ChunkSectionPos.getSectionCoord(endX);
        var numChunks = (endChunkZ - startChunkZ + 1) * (endChunkX - startChunkX + 1);

        return new ArenaBoundsDto(startChunkX, startChunkZ, endChunkX, endChunkZ, numChunks);
    }

    /**
     * Calculates the union bounds of two arena bounds
     *
     * @param first  The first arena bounds
     * @param second The second arena bounds
     * @return The union of both bounds
     */
    private ArenaBoundsDto calculateUnion(ArenaBoundsDto first, ArenaBoundsDto second) {

        var unionStartChunkZ = Math.min(first.startZ(), second.startZ());
        var unionStartChunkX = Math.min(first.startX(), second.startX());
        var unionEndChunkZ = Math.max(first.endZ(), second.endZ());
        var unionEndChunkX = Math.max(first.endX(), second.endX());
        var unionNumChunks = (unionEndChunkZ - unionStartChunkZ + 1) * (unionEndChunkX - unionStartChunkX + 1);

        return new ArenaBoundsDto(unionStartChunkX, unionStartChunkZ, unionEndChunkX, unionEndChunkZ, unionNumChunks);
    }

    /**
     * Sends the progress event to all clients in the world
     *
     * @param stepString String describing what the current step is
     * @param progress   The progress in percent of the current step
     */
    private void sendMapLoadProgressEvent(String stepString, double progress) {

        // Create packet buffer
        var buf = new PacketByteBuf(Unpooled.buffer());

        buf.writeString(stepString);
        buf.writeDouble(progress);

        ServerEventSending.sendToEveryone(server, NetworkingConstants.MAP_LOADING_EVENT, buf);
    }

    /**
     * Executes a consumer on every chunk in the given bounds
     *
     * @param bounds The bounds
     * @param action The action to execute. First argument is the chunk x-position, second the chunk z-position
     */
    private void forEachChunk(ArenaBoundsDto bounds, BiConsumer<Integer, Integer> action) {

        // For every slice of chunks in z-direction
        for (var chunkZ = bounds.startZ(); chunkZ <= bounds.endZ(); ++chunkZ) {

            // For every chunk in the slice
            for (var chunkX = bounds.startX(); chunkX <= bounds.endX(); ++chunkX) {

                // Call the action
                action.accept(chunkX, chunkZ);
            }
        }
    }
//endregion
}
