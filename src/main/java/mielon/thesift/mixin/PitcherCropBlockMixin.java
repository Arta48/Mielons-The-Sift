package mielon.thesift.mixin;

import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PitcherCropBlock.class})
public abstract class PitcherCropBlockMixin {
   @Inject(
      method = {"mayPlaceOn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$allowSiftSoil(BlockState floor, BlockGetter level, BlockPos floorPos, CallbackInfoReturnable cir) {
      if (theSift$isSiftSoil(floor)) {
         cir.setReturnValue(true);
      }

   }

   @Inject(
      method = {"canSurvive"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$allowGeneratedBulbsInCaves(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable cir) {
      if (theSift$isSiftSoil(level.getBlockState(pos.below()))) {
         cir.setReturnValue(true);
      }

   }

   private static boolean theSift$isSiftSoil(BlockState floor) {
      return floor.is(ModBlocks.SIFTSLATE) || floor.is(ModBlocks.HEALTHY_SCULK) || floor.is(ModBlocks.DRY_HEALTHY_SCULK) || floor.is(ModBlocks.SIFTSLATE_GROWTH) || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH) || floor.is(ModBlocks.OVERGROWN_WILLOW_FOLIAGE);
   }
}
