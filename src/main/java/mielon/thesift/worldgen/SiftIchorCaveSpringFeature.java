package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftIchorCaveSpringFeature implements Feature {
   public static final MapCodec CODEC = MapCodec.unit(SiftIchorCaveSpringFeature::new);
   private static final Direction[] POSSIBLE_OPENINGS;

   public MapCodec codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      for (Direction direction : POSSIBLE_OPENINGS) {
         BlockPos opening = origin.relative(direction);
         if (level.isEmptyBlock(opening)) {
            int surfaceY = level.getHeight(Types.WORLD_SURFACE_WG, opening.getX(), opening.getZ());
            if (opening.getY() >= surfaceY - 1) {
               return false;
            }
         }
      }

      if (isValidSpringBlock(level.getBlockState(origin.above())) && isValidSpringBlock(level.getBlockState(origin.below()))) {
         BlockState current = level.getBlockState(origin);
         if (!current.isAir() && !isValidSpringBlock(current)) {
            return false;
         } else {
            int rockCount = 0;
            int holeCount = 0;

            for (Direction direction : POSSIBLE_OPENINGS) {
               BlockPos adjacent = origin.relative(direction);
               BlockState adjacentState = level.getBlockState(adjacent);
               if (isValidSpringBlock(adjacentState)) {
                  ++rockCount;
               }

               if (level.isEmptyBlock(adjacent)) {
                  ++holeCount;
               }
            }

            if (rockCount == 4 && holeCount == 1) {
               level.setBlock(origin, ModFluids.ICHOR.getSource(true).createLegacyBlock(), 2);
               level.scheduleTick(origin, ModFluids.ICHOR, 0);
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean isValidSpringBlock(BlockState state) {
      return state.is(ModBlocks.SIFTSLATE) || state.is(ModBlocks.SIFTSLATE_GROWTH) || state.is(ModBlocks.HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH);
   }

   static {
      POSSIBLE_OPENINGS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN};
   }
}
