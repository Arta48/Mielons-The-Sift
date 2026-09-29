package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftLavaFloorFeature implements Feature {
   public static final MapCodec CODEC = MapCodec.unit(SiftLavaFloorFeature::new);
   private static final int LAVA_SURFACE_Y = 11;

   public MapCodec codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      int chunkMinX = origin.getX() & -16;
      int chunkMinZ = origin.getZ() & -16;
      int maxY = Math.min(11, level.getMaxY() - 1);
      int placed = 0;
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (int x = chunkMinX; x < chunkMinX + 16; ++x) {
         for (int z = chunkMinZ; z < chunkMinZ + 16; ++z) {
            for (int y = level.getMinY(); y <= maxY; ++y) {
               cursor.set(x, y, z);
               if (level.getBlockState(cursor).isAir() && level.ensureCanWrite(cursor)) {
                  level.setBlock(cursor, Blocks.LAVA.defaultBlockState(), 2);
                  ++placed;
               }
            }
         }
      }

      return placed > 0;
   }
}
