package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftslateGrowthBlock;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftIchorSnowFeature implements Feature {
   public static final MapCodec CODEC = MapCodec.unit(SiftIchorSnowFeature::new);
   private static final long SNOW_SALT = 5279142693729160783L;
   private static final long DETAIL_SALT = 6002822589557593153L;

   public MapCodec codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;
      long seed = level.getSeed();
      boolean changed = false;

      for (int x = minX; x < minX + 16; ++x) {
         for (int z = minZ; z < minZ + 16; ++z) {
            int groundY = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            if (groundY >= level.getMinY() && groundY < level.getMaxY() - 2) {
               BlockPos groundPos = new BlockPos(x, groundY, z);
               if (level.getBiome(groundPos).is(TheSiftDimension.ICHOR_SNOWY_PEAKS)) {
                  BlockState ground = level.getBlockState(groundPos);
                  if (isNaturalMountainSurface(ground) && ground.getFluidState().isEmpty()) {
                     changed |= placeSnowLayer(level, groundPos, seed, x, z, 5279142693729160783L, true);
                  }

                  int canopyY = level.getHeight(Types.MOTION_BLOCKING, x, z) - 1;
                  if (canopyY >= level.getMinY() && canopyY < level.getMaxY() - 1) {
                     BlockPos canopyPos = new BlockPos(x, canopyY, z);
                     if (level.getBiome(canopyPos).is(TheSiftDimension.ICHOR_SNOWY_PEAKS) && isWillowSnowSupport(level.getBlockState(canopyPos))) {
                        changed |= placeSnowLayer(level, canopyPos, seed, x, z, 2164546982323162371L, false);
                     }
                  }
               }
            }
         }
      }

      return changed;
   }

   private static boolean placeSnowLayer(WorldGenLevel level, BlockPos supportPos, long seed, int x, int z, long salt, boolean replaceVegetation) {
      BlockPos snowPos = supportPos.above();
      BlockState above = level.getBlockState(snowPos);
      if ((above.isAir() || replaceVegetation && isReplaceablePeakVegetation(above)) && level.ensureCanWrite(snowPos)) {
         double field = valueNoise(seed ^ salt, x, z, 26);
         double detail = valueNoise(seed ^ 6002822589557593153L ^ salt, x, z, 9);
         int layers = field + detail * 0.35 > 0.88 ? 2 : 1;
         BlockState snow = (BlockState)ModBlocks.ICHOR_SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
         if (!snow.canSurvive(level, snowPos)) {
            return false;
         } else {
            clearUpperSnifferHalf(level, snowPos, above);
            level.setBlock(snowPos, snow, 2);
            BlockState support = level.getBlockState(supportPos);
            if (support.is(ModBlocks.SIFTSLATE_GROWTH) && !(Boolean)support.getValue(SiftslateGrowthBlock.SNOWY)) {
               level.setBlock(supportPos, (BlockState)support.setValue(SiftslateGrowthBlock.SNOWY, true), 2);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean isNaturalMountainSurface(BlockState state) {
      return state.is(ModBlocks.SIFTSLATE) || state.is(ModBlocks.SIFTSLATE_GROWTH) || state.is(ModBlocks.HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH) || state.is(Blocks.SCULK);
   }

   private static boolean isWillowSnowSupport(BlockState state) {
      return state.is(ModBlocks.OVERGROWN_WILLOW_FOLIAGE) || state.is(ModBlocks.OVERGROWN_WILLOW_LOG) || state.is(ModBlocks.OVERGROWN_WILLOW_WOOD);
   }

   private static boolean isReplaceablePeakVegetation(BlockState state) {
      return state.is(ModBlocks.HEALTHY_SCULK_SPROUTS) || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS) || state.is(ModBlocks.SIFTSLATE_STALKS) || state.is(ModBlocks.OVERGROWN_FRONDS) || state.is(ModBlocks.OVERGROWN_CHARD) || state.is(ModBlocks.OVERGROWN_STALKS) || state.is(ModBlocks.OVERGROWN_LOTUS) || state.is(ModBlocks.SUNBURST_PLANT) || state.is(ModBlocks.WHISPERBLOOM) || state.is(Blocks.SCULK_VEIN) || state.is(Blocks.WILDFLOWERS) || state.is(Blocks.TORCHFLOWER) || state.is(Blocks.TORCHFLOWER_CROP) || state.is(Blocks.PITCHER_CROP) || state.is(Blocks.PITCHER_PLANT);
   }

   private static void clearUpperSnifferHalf(WorldGenLevel level, BlockPos lowerPos, BlockState lowerState) {
      if (lowerState.is(Blocks.PITCHER_CROP) || lowerState.is(Blocks.PITCHER_PLANT)) {
         BlockPos upperPos = lowerPos.above();
         BlockState upperState = level.getBlockState(upperPos);
         if ((upperState.is(Blocks.PITCHER_CROP) || upperState.is(Blocks.PITCHER_PLANT)) && level.ensureCanWrite(upperPos)) {
            level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 2);
         }

      }
   }

   private static double valueNoise(long seed, int x, int z, int scale) {
      int cellX = Math.floorDiv(x, scale);
      int cellZ = Math.floorDiv(z, scale);
      double localX = (double)Math.floorMod(x, scale) / (double)scale;
      double localZ = (double)Math.floorMod(z, scale) / (double)scale;
      double smoothX = localX * localX * ((double)3.0F - (double)2.0F * localX);
      double smoothZ = localZ * localZ * ((double)3.0F - (double)2.0F * localZ);
      double a = unit(seed, cellX, cellZ);
      double b = unit(seed, cellX + 1, cellZ);
      double c = unit(seed, cellX, cellZ + 1);
      double d = unit(seed, cellX + 1, cellZ + 1);
      return lerp(smoothZ, lerp(smoothX, a, b), lerp(smoothX, c, d));
   }

   private static double unit(long seed, int x, int z) {
      long value = seed ^ (long)x * -7046029254386353131L ^ (long)z * -4417276706812531889L;
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      value *= -7723592293110705685L;
      value ^= value >>> 31;
      return (double)(value >>> 11) * (double)1.110223E-16F;
   }

   private static double lerp(double delta, double start, double end) {
      return start + delta * (end - start);
   }
}
