package de.kleiner3.lasertag.common.util;

import de.kleiner3.lasertag.LasertagMod;
import net.minecraft.nbt.*;

import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Util class for NbtCompound operations
 *
 * @author Étienne Muser
 */
public class NbtUtil {
    /**
     * Converts a NbtCompound deserialized from a .litematic file and converts it into a NbtCompound with the format of a .nbt file
     * @param litematic The litematic to convert
     * @param mainRegionName The name of the main region
     * @return The converted nbt
     */
    public static NbtCompound convertLitematicToNbt(NbtCompound litematic, String mainRegionName) {
        // Create new nbt compound
        var nbt = new NbtCompound();

        // Get the regions element
        var regionsElement = litematic.get("Regions");

        // If the regions element is not a nbt compound
        if (!(regionsElement instanceof NbtCompound regionsCompound)) {
            return null;
        }

        // Get the main region element
        var mainRegionElement = regionsCompound.get(mainRegionName);

        // If the main region element is not a nbt compound
        if (!(mainRegionElement instanceof NbtCompound mainRegionCompound)) {
            return null;
        }

        // Add size nbt list
        if (!addSize(nbt, mainRegionCompound)) {
            return null;
        }

        // Add entities list
        if (!addEntities(nbt, mainRegionCompound)) {
            return null;
        }

        // Get size element
        var sizeElement = mainRegionCompound.get("Size");

        // Check
        if (!(sizeElement instanceof NbtCompound sizeCompound)) {
            return null;
        }

        // Get the palette
        var paletteElement = mainRegionCompound.get("BlockStatePalette");

        // Check
        if (!(paletteElement instanceof NbtList paletteList)) {
            return null;
        }

        // Add blocks list
        if (!addBlockList(nbt, mainRegionCompound, sizeCompound, paletteList)) {
            return null;
        }

        // Add tile entities
        if (!addTileEntities(nbt, mainRegionCompound, paletteList)) {
            return null;
        }

        // Add palette list
        nbt.put("palette", paletteElement);

        // Add data version int
        nbt.put("DataVersion", litematic.get("MinecraftDataVersion"));

        return nbt;
    }

    //region convertLitematicToNbt helper

    private static boolean addSize(NbtCompound nbt, NbtCompound litematicMainRegion) {
        // Get size element
        var sizeElement = litematicMainRegion.get("Size");

        // If is not nbt compound
        if (!(sizeElement instanceof NbtCompound sizeCompound)) {
            return false;
        }

        // Convert to list
        var sizeList = xyzCompoundToList(sizeCompound);

        // Put
        nbt.put("size", sizeList);

        return true;
    }

    private static boolean addEntities(NbtCompound nbt, NbtCompound litematicMainRegion) {
        // Get the entities element
        var entitiesElement = litematicMainRegion.get("Entities");

        // If is not nbt list
        if (!(entitiesElement instanceof NbtList entitiesList)) {
            return false;
        }

        // Create new list
        var list = new NbtList();

        // For every entity
        for (var entity : entitiesList) {
            // If is not nbtCompound
            if (!(entity instanceof NbtCompound entityCompound)) {
                return false;
            }

            // Add to list
            if (!addEntityToList(list, entityCompound)) {
                return false;
            }
        }

        // Put
        nbt.put("entities", list);

        return true;
    }

    private static boolean addEntityToList(NbtList list, NbtCompound litematicEntity) {
        // Get pos of entity
        var posElement = litematicEntity.get("Pos");

        // If pos is not nbt list
        if (!(posElement instanceof NbtList posList)) {
            return false;
        }

        // Create new entity
        var entity = new NbtCompound();

        // Add pos to entity
        entity.put("pos", posList);

        // Remove pos from litematica entity
        litematicEntity.remove("Pos");

        // Add nbt to entity
        entity.put("nbt", litematicEntity);

        // Add entity to list
        list.add(entity);

        return true;
    }


    private static boolean addTileEntities(NbtCompound nbt, NbtCompound litematicMainRegion, NbtList blockPalette) {
        // Get the tile entities element
        var tileEntitiesElement = litematicMainRegion.get("TileEntities");

        // If is not nbt list
        if (!(tileEntitiesElement instanceof NbtList tileEntitiesList)) {
            return false;
        }

        // Get the block list element
        var blockListElement = nbt.get("blocks");

        // If is not nbt list
        if (!(blockListElement instanceof  NbtList blockList)) {
            return false;
        }

        // For every tile entity
        for (var tileEntity : tileEntitiesList) {
            // If is not nbtCompound
            if (!(tileEntity instanceof NbtCompound tileEntityCompound)) {
                return false;
            }

            // Add to list
            if (!addTileEntityToList(blockList, tileEntityCompound, blockPalette)) {
                return false;
            }
        }

        return true;
    }

    private static boolean addTileEntityToList(NbtList list, NbtCompound litematicTileEntity, NbtList blockPalette) {
        // Get pos of entity
        var xPosElement = litematicTileEntity.get("x");
        var yPosElement = litematicTileEntity.get("y");
        var zPosElement = litematicTileEntity.get("z");

        // If pos is not nbt ints
        if (!(xPosElement instanceof NbtInt xPos)) {
            return false;
        }
        if (!(yPosElement instanceof NbtInt yPos)) {
            return false;
        }
        if (!(zPosElement instanceof NbtInt zPos)) {
            return false;
        }

        // Remove pos from litematic entity
        litematicTileEntity.remove("x");
        litematicTileEntity.remove("y");
        litematicTileEntity.remove("z");

        // Find the block
        var blocks = list.stream().filter(nbtElement -> {
            if (!(nbtElement instanceof NbtCompound nbtCompound)) {
                return false;
            }

            var posElement = nbtCompound.get("pos");

            if (!(posElement instanceof NbtList posList)) {
                return false;
            }

            var blockXPosElement = posList.get(0);
            var blockYPosElement = posList.get(1);
            var blockZPosElement = posList.get(2);

            if (!(blockXPosElement instanceof NbtInt blockXPos)) {
                return false;
            }
            if (!(blockYPosElement instanceof NbtInt blockYPos)) {
                return false;
            }
            if (!(blockZPosElement instanceof NbtInt blockZPos)) {
                return false;
            }

            return blockXPos.equals(xPos) &&
                    blockYPos.equals(yPos) &&
                    blockZPos.equals(zPos);
        });

        var blockElementOptional = blocks.findFirst();

        if (blockElementOptional.isEmpty()) {
            return false;
        }

        var blockElement = blockElementOptional.get();

        if (!(blockElement instanceof NbtCompound block)) {
            return false;
        }

        // Get block state element
        var blockStateElement = block.get("state");

        if (!(blockStateElement instanceof NbtInt blockState)) {
            return false;
        }

        // Get palette element
        var paletteElement = blockPalette.get(blockState.intValue());

        if (!(paletteElement instanceof NbtCompound paletteBlockState)) {
            return false;
        }

        // Get the block name element
        var blockNameElement = paletteBlockState.get("Name");

        if (!(blockNameElement instanceof NbtString blockName)) {
            return false;
        }

        block.put("nbt", litematicTileEntity);
        return true;
    }

    private static boolean addBlockList(NbtCompound nbt, NbtCompound litematicMainRegion, NbtCompound size, NbtList palette) {
        // Create block list
        var list = new NbtList();

        // Get size
        var sizeX = ((NbtInt)size.get("x")).intValue();
        var sizeY = ((NbtInt)size.get("y")).intValue();
        var sizeZ = ((NbtInt)size.get("z")).intValue();

        // Get block state array element
        var blockStateArrayElement = litematicMainRegion.get("BlockStates");

        // If is not long array
        if (!(blockStateArrayElement instanceof NbtLongArray blockStateArray)) {
            return false;
        }

        // Calculate number of bits
        var bits = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(palette.size() - 1));

        // Calculate max entry value
        var maxEntryValue = (1L << bits) - 1L;

        // Iterate over every possible block
        for (long x = 0; x < sizeX; x++) {
            for (long y = 0; y < sizeY; y++) {
                for (long z = 0; z < sizeZ; z++) {
                    // Get index in array
                    var index = (y * sizeX * sizeZ) + (z * sizeX) + x;

                    // Get the block state index in the palette
                    var paletteIndex = getBlockAt(blockStateArray, index, bits, maxEntryValue);

                    // Create block nbt compound
                    var block = new NbtCompound();

                    // Create pos list
                    var pos = new NbtList();
                    pos.add(NbtInt.of((int) x));
                    pos.add(NbtInt.of((int) y));
                    pos.add(NbtInt.of((int) z));

                    // Add pos to block
                    block.put("pos", pos);

                    // Add state to block
                    block.put("state", NbtInt.of(paletteIndex));

                    // Add block to list
                    list.add(block);
                }
            }
        }

        // Add block list to nbt
        nbt.put("blocks", list);

        return true;
    }

    private static int getBlockAt(NbtLongArray array, long index, int bitsPerEntry, long maxEntryValue) {
        var startOffset = index * (long) bitsPerEntry;
        var startArrIndex = (int) (startOffset >> 6); // startOffset / 64
        var endArrIndex = (int) (((index + 1L) * (long) bitsPerEntry - 1L) >> 6);
        var startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64

        var valAtStartIndex = array.get(startArrIndex).longValue();

        if (startArrIndex == endArrIndex)
        {
            return (int) (valAtStartIndex >>> startBitOffset & maxEntryValue);
        }
        else
        {
            var valAtEndIndex = array.get(endArrIndex).longValue();

            var endOffset = 64 - startBitOffset;
            return (int) ((valAtStartIndex >>> startBitOffset | valAtEndIndex << endOffset) & maxEntryValue);
        }
    }

    //endregion

    public static NbtList xyzCompoundToList(NbtCompound compound) {
        // Create list
        var list = new NbtList();

        // Add to list
        list.add(0, compound.get("x"));
        list.add(1, compound.get("y"));
        list.add(2, compound.get("z"));

        return list;
    }
}
