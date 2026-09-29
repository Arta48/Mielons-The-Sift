package mielon.thesift.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;

final class SiftSnifferPlantPlacement {
   private SiftSnifferPlantPlacement() {
   }

   static PlantType chooseBlobType(RandomSource random, float torchflowerChance) {
      return random.nextFloat() < torchflowerChance ? SiftSnifferPlantPlacement.PlantType.TORCHFLOWER : SiftSnifferPlantPlacement.PlantType.PITCHER_CROP;
   }

   static boolean place(WorldGenLevel level, BlockPos plantPos, PlantType type, RandomSource random) {
      if (level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
         if (type == SiftSnifferPlantPlacement.PlantType.TORCHFLOWER) {
            BlockState torchflower = Blocks.TORCHFLOWER.defaultBlockState();
            if (!torchflower.canSurvive(level, plantPos)) {
               return false;
            } else {
               level.setBlock(plantPos, torchflower, 2);
               return true;
            }
         } else if (level.isEmptyBlock(plantPos.above()) && level.ensureCanWrite(plantPos.above())) {
            BlockState bulb = (BlockState)Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE, 3 + random.nextInt(2));
            if (!bulb.canSurvive(level, plantPos)) {
               return false;
            } else {
               DoublePlantBlock.placeAt(level, bulb, plantPos, 2);
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   static enum PlantType {
      TORCHFLOWER,
      PITCHER_CROP;

      // $FF: synthetic method
      private static PlantType[] $values() {
         return new PlantType[]{TORCHFLOWER, PITCHER_CROP};
      }
   }
}
