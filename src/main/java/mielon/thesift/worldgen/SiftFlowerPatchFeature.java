package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftFlowerPatchFeature implements Feature {
   public static final MapCodec CODEC = MapCodec.unit(SiftFlowerPatchFeature::new);

   public MapCodec codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      int centerX = (origin.getX() & -16) + 8;
      int centerZ = (origin.getZ() & -16) + 8;
      int target = 3 + random.nextInt(2);
      int placed = 0;
      int surfaceY = level.getHeight(Types.WORLD_SURFACE_WG, centerX, centerZ);
      boolean overgrown = TheSiftDimension.isOvergrownBiome(level.getBiome(new BlockPos(centerX, surfaceY, centerZ)));
      int selection = random.nextInt(overgrown ? 6 : 24);
      boolean wildflowers = selection < 3;
      Block var10000;
      switch (selection % 3) {
         case 0 -> var10000 = ModBlocks.OVERGROWN_LOTUS;
         case 1 -> var10000 = ModBlocks.SUNBURST_PLANT;
         default -> var10000 = ModBlocks.WHISPERBLOOM;
      }

      Block flower = var10000;

      for (int attempt = 0; attempt < 24 && placed < target; ++attempt) {
         int x = centerX + random.nextInt(7) - 3;
         int z = centerZ + random.nextInt(7) - 3;
         int y = level.getHeight(Types.WORLD_SURFACE_WG, x, z);
         BlockPos plantPos = new BlockPos(x, y, z);
         BlockState floor = level.getBlockState(plantPos.below());
         if (isFlowerSoil(floor) && level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
            BlockState flowerState = wildflowers ? wildflowersState(random) : flower.defaultBlockState();
            if (flowerState.canSurvive(level, plantPos)) {
               level.setBlock(plantPos, flowerState, 2);
               ++placed;
            }
         }
      }

      return placed > 0;
   }

   static boolean isFlowerSoil(BlockState floor) {
      return floor.is(ModBlocks.SIFTSLATE) || floor.is(ModBlocks.SIFTSLATE_GROWTH) || floor.is(ModBlocks.DRY_HEALTHY_SCULK) || floor.is(ModBlocks.HEALTHY_SCULK);
   }

   static boolean isLakeFlowerSoil(BlockState floor) {
      return isFlowerSoil(floor) || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH);
   }

   static BlockState wildflowersState(RandomSource random) {
      return (BlockState)((BlockState)Blocks.WILDFLOWERS.defaultBlockState().setValue(FlowerBedBlock.FACING, Direction.from2DDataValue(random.nextInt(4)))).setValue(FlowerBedBlock.AMOUNT, 1 + random.nextInt(4));
   }
}
