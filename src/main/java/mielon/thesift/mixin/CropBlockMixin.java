package mielon.thesift.mixin;

import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CropBlock.class})
public abstract class CropBlockMixin {
   @Inject(
      method = {"mayPlaceOn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$allowOnlyTorchflowerOnSiftSoil(BlockState floor, BlockGetter level, BlockPos floorPos, CallbackInfoReturnable cir) {
      if ((Object)this == Blocks.TORCHFLOWER_CROP && theSift$isSiftSoil(floor)) {
         cir.setReturnValue(true);
      }

   }

   private static boolean theSift$isSiftSoil(BlockState floor) {
      return floor.is(ModBlocks.SIFTSLATE) || floor.is(ModBlocks.SIFTSLATE_GROWTH) || floor.is(ModBlocks.HEALTHY_SCULK) || floor.is(ModBlocks.DRY_HEALTHY_SCULK) || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH) || floor.is(ModBlocks.OVERGROWN_WILLOW_FOLIAGE);
   }
}
