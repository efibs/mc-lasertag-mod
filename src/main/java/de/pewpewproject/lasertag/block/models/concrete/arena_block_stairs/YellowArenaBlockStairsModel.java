package de.pewpewproject.lasertag.block.models.concrete.arena_block_stairs;

import de.pewpewproject.lasertag.block.models.EmissiveStairsModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Custom block model for the arena block stairs
 *
 * @author Étienne Muser
 */
@Environment(EnvType.CLIENT)
public class YellowArenaBlockStairsModel extends EmissiveStairsModel {
    public YellowArenaBlockStairsModel() {
        super(
                "block/arena_block_dark",
                "block/yellow_arena_block_glow",
                "block/arena_block_half_dark",
                "block/yellow_arena_block_half_glow",
                "block/arena_block_stair_side_dark",
                "block/yellow_arena_block_stair_side_glow"
        );
    }
}
