package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftslatePlantPatchFeature implements Feature {
   public static final MapCodec CODEC = MapCodec.unit(SiftslatePlantPatchFeature::new);

   public MapCodec codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      int centerX = (origin.getX() & -16) + 8;
      int centerZ = (origin.getZ() & -16) + 8;
      int target = 4 + random.nextInt(4);
      int placed = 0;
      Block var10000;
      switch (random.nextInt(6)) {
         case 0 -> var10000 = ModBlocks.OVERGROWN_FRONDS;
         case 1 -> var10000 = ModBlocks.OVERGROWN_STALKS;
         case 2 -> var10000 = ModBlocks.OVERGROWN_CHARD;
         case 3 -> var10000 = ModBlocks.SIFTSLATE_STALKS;
         case 4 -> var10000 = ModBlocks.HEALTHY_SCULK_SPROUTS;
         default -> var10000 = ModBlocks.DRY_HEALTHY_SCULK_SPROUTS;
      }

      Block plant = var10000;

      for (int attempt = 0; attempt < 32 && placed < target; ++attempt) {
         int x = centerX + random.nextInt(9) - 4;
         int z = centerZ + random.nextInt(9) - 4;
         int y = level.getHeight(Types.WORLD_SURFACE_WG, x, z);
         BlockPos plantPos = new BlockPos(x, y, z);
         if (level.getBlockState(plantPos.below()).is(ModBlocks.SIFTSLATE) && level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
            BlockState state = plant.defaultBlockState();
            if (state.canSurvive(level, plantPos)) {
               level.setBlock(plantPos, state, 2);
               ++placed;
            }
         }
      }

      return placed > 0;
   }
}
