package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SiftSurfaceDecorator {
   private SiftSurfaceDecorator() {
   }

   public static boolean decorate(WorldGenLevel level, RandomSource random, BlockPos floorPos, float densityScale) {
      return decorate(level, random, floorPos, densityScale, 2);
   }

   public static boolean decorate(WorldGenLevel level, RandomSource random, BlockPos floorPos, float densityScale, int updateFlags) {
      BlockState floor = level.getBlockState(floorPos);
      BlockPos plantPos = floorPos.above();
      if (level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
         float roll = random.nextFloat();
         float chance;
         Block plant;
         if (floor.is(ModBlocks.SIFTSLATE_GROWTH)) {
            chance = 0.255F;
            plant = roll < 0.43F ? ModBlocks.OVERGROWN_FRONDS : (roll < 0.68F ? ModBlocks.OVERGROWN_STALKS : (roll < 0.86F ? ModBlocks.OVERGROWN_CHARD : ModBlocks.SIFTSLATE_STALKS));
         } else if (floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) {
            chance = 0.235F;
            plant = roll < 0.34F ? ModBlocks.DRY_HEALTHY_SCULK_SPROUTS : (roll < 0.59F ? ModBlocks.OVERGROWN_FRONDS : (roll < 0.8F ? ModBlocks.OVERGROWN_STALKS : (roll < 0.93F ? ModBlocks.OVERGROWN_CHARD : ModBlocks.SIFTSLATE_STALKS)));
         } else if (floor.is(ModBlocks.HEALTHY_SCULK)) {
            chance = 0.225F;
            plant = roll < 0.76F ? ModBlocks.HEALTHY_SCULK_SPROUTS : (roll < 0.92F ? ModBlocks.SIFTSLATE_STALKS : ModBlocks.OVERGROWN_FRONDS);
         } else if (floor.is(ModBlocks.DRY_HEALTHY_SCULK)) {
            chance = 0.145F;
            plant = roll < 0.74F ? ModBlocks.DRY_HEALTHY_SCULK_SPROUTS : (roll < 0.94F ? ModBlocks.SIFTSLATE_STALKS : ModBlocks.OVERGROWN_STALKS);
         } else {
            if (!floor.is(ModBlocks.SIFTSLATE)) {
               return false;
            }

            chance = 0.045F;
            plant = roll < 0.72F ? ModBlocks.SIFTSLATE_STALKS : ModBlocks.OVERGROWN_FRONDS;
         }

         if (random.nextFloat() >= chance * densityScale) {
            return false;
         } else {
            BlockState plantState = plant.defaultBlockState();
            if (!plantState.canSurvive(level, plantPos)) {
               return false;
            } else {
               level.setBlock(plantPos, plantState, updateFlags);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public static void decorateLakeShore(WorldGenLevel level, RandomSource random, List shore, int updateFlags) {
      if (!shore.isEmpty()) {
         List<BlockPos> flowerFloors = new ArrayList();

         for (BlockPos floorPos : (Iterable<BlockPos>) shore) {
            if (SiftFlowerPatchFeature.isLakeFlowerSoil(level.getBlockState(floorPos))) {
               flowerFloors.add(floorPos);
            }
         }

         int batches = Math.max(2, Math.min(12, 2 + flowerFloors.size() / 30));
         int firstSpecies = random.nextInt(6);
         List<BlockPos> anchors = new ArrayList();

         for (int batch = 0; batch < batches && !flowerFloors.isEmpty(); ++batch) {
            BlockPos anchor = chooseSeparatedAnchor(flowerFloors, anchors, random);
            if (anchor == null) {
               break;
            }

            anchors.add(anchor);
            placeFlowerBatch(level, random, flowerFloors, anchor, (firstSpecies + batch) % 6, updateFlags);
         }

         for (BlockPos floorPos : (Iterable<BlockPos>) shore) {
            BlockState floor = level.getBlockState(floorPos);
            float chance = !floor.is(ModBlocks.SIFTSLATE_GROWTH) && !floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH) ? (floor.is(ModBlocks.HEALTHY_SCULK) ? 0.6F : (floor.is(ModBlocks.DRY_HEALTHY_SCULK) ? 0.5F : (floor.is(ModBlocks.SIFTSLATE) ? 0.45F : 0.0F))) : 0.68F;
            if (!(random.nextFloat() >= chance)) {
               BlockPos plantPos = floorPos.above();
               if (level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
                  float roll = random.nextFloat();
                  Block plant;
                  if (!floor.is(ModBlocks.SIFTSLATE_GROWTH) && !floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) {
                     if (floor.is(ModBlocks.SIFTSLATE)) {
                        plant = roll < 0.82F ? ModBlocks.SIFTSLATE_STALKS : ModBlocks.HEALTHY_SCULK_SPROUTS;
                     } else if (floor.is(ModBlocks.HEALTHY_SCULK)) {
                        plant = roll < 0.82F ? ModBlocks.HEALTHY_SCULK_SPROUTS : ModBlocks.SIFTSLATE_STALKS;
                     } else {
                        if (!floor.is(ModBlocks.DRY_HEALTHY_SCULK) && !floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) {
                           continue;
                        }

                        plant = ModBlocks.DRY_HEALTHY_SCULK_SPROUTS;
                     }
                  } else {
                     plant = roll < 0.38F ? ModBlocks.OVERGROWN_FRONDS : (roll < 0.72F ? ModBlocks.OVERGROWN_STALKS : ModBlocks.OVERGROWN_CHARD);
                  }

                  BlockState plantState = plant.defaultBlockState();
                  if (plantState.canSurvive(level, plantPos)) {
                     level.setBlock(plantPos, plantState, updateFlags);
                  }
               }
            }
         }

      }
   }

   private static BlockPos chooseSeparatedAnchor(List candidates, List used, RandomSource random) {
      for (int attempt = 0; attempt < 32; ++attempt) {
         BlockPos candidate = (BlockPos)candidates.get(random.nextInt(candidates.size()));
         boolean separated = true;

         for (BlockPos anchor : (Iterable<BlockPos>) used) {
            int dx = candidate.getX() - anchor.getX();
            int dz = candidate.getZ() - anchor.getZ();
            if (dx * dx + dz * dz < 16) {
               separated = false;
               break;
            }
         }

         if (separated) {
            return candidate;
         }
      }

      return null;
   }

   private static void placeFlowerBatch(WorldGenLevel level, RandomSource random, List candidates, BlockPos anchor, int species, int updateFlags) {
      int target = 3 + random.nextInt(2);
      List<BlockPos> nearby = new ArrayList();

      for (BlockPos candidate : (Iterable<BlockPos>) candidates) {
         if (Math.abs(candidate.getX() - anchor.getX()) <= 2 && Math.abs(candidate.getZ() - anchor.getZ()) <= 2 && Math.abs(candidate.getY() - anchor.getY()) <= 1) {
            nearby.add(candidate);
         }
      }

      int placed = 0;

      while(placed < target && !nearby.isEmpty()) {
         BlockPos floorPos = (BlockPos)nearby.remove(random.nextInt(nearby.size()));
         if (placeOasisFlower(level, random, floorPos.above(), species, updateFlags)) {
            ++placed;
         }
      }

   }

   private static boolean placeOasisFlower(WorldGenLevel level, RandomSource random, BlockPos plantPos, int species, int updateFlags) {
      if (SiftFlowerPatchFeature.isLakeFlowerSoil(level.getBlockState(plantPos.below())) && level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
         if (species == 4) {
            if (level.isEmptyBlock(plantPos.above()) && level.ensureCanWrite(plantPos.above())) {
               BlockState pitcher = (BlockState)Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE, 3 + random.nextInt(2));
               if (!pitcher.canSurvive(level, plantPos)) {
                  return false;
               } else {
                  DoublePlantBlock.placeAt(level, pitcher, plantPos, updateFlags);
                  return true;
               }
            } else {
               return false;
            }
         } else if (species == 5) {
            BlockState wildflowers = SiftFlowerPatchFeature.wildflowersState(random);
            if (!wildflowers.canSurvive(level, plantPos)) {
               return false;
            } else {
               level.setBlock(plantPos, wildflowers, updateFlags);
               return true;
            }
         } else {
            Block var10000;
            switch (species) {
               case 0 -> var10000 = ModBlocks.OVERGROWN_LOTUS;
               case 1 -> var10000 = ModBlocks.SUNBURST_PLANT;
               case 2 -> var10000 = ModBlocks.WHISPERBLOOM;
               default -> var10000 = Blocks.TORCHFLOWER;
            }

            Block flower = var10000;
            BlockState state = flower.defaultBlockState();
            if (!state.canSurvive(level, plantPos)) {
               return false;
            } else {
               level.setBlock(plantPos, state, updateFlags);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean decorateStructureSurface(WorldGenLevel level, RandomSource random, BlockPos floorPos, float densityScale, int updateFlags) {
      BlockState floor = level.getBlockState(floorPos);
      BlockPos plantPos = floorPos.above();
      if (level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
         float roll = random.nextFloat();
         float chance;
         Block plant;
         if (!floor.is(ModBlocks.SIFTSLATE_GROWTH) && !floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) {
            if (floor.is(ModBlocks.SIFTSLATE)) {
               chance = 0.36F;
               plant = ModBlocks.SIFTSLATE_STALKS;
            } else if (floor.is(ModBlocks.HEALTHY_SCULK)) {
               chance = 0.42F;
               plant = ModBlocks.HEALTHY_SCULK_SPROUTS;
            } else {
               if (!floor.is(ModBlocks.DRY_HEALTHY_SCULK)) {
                  return false;
               }

               chance = 0.3F;
               plant = ModBlocks.DRY_HEALTHY_SCULK_SPROUTS;
            }
         } else {
            chance = 0.72F;
            plant = roll < 0.4F ? ModBlocks.OVERGROWN_FRONDS : (roll < 0.72F ? ModBlocks.OVERGROWN_STALKS : ModBlocks.OVERGROWN_CHARD);
         }

         if (random.nextFloat() >= chance * densityScale) {
            return false;
         } else {
            BlockState plantState = plant.defaultBlockState();
            if (!plantState.canSurvive(level, plantPos)) {
               return false;
            } else {
               level.setBlock(plantPos, plantState, updateFlags);
               return true;
            }
         }
      } else {
         return false;
      }
   }
}
