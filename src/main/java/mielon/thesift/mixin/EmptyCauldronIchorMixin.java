package mielon.thesift.mixin;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CauldronBlock.class})
public abstract class EmptyCauldronIchorMixin {
   @Inject(
      method = {"canReceiveStalactiteDrip"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$acceptIchor(Fluid fluid, CallbackInfoReturnable cir) {
      if (fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR) {
         cir.setReturnValue(true);
      }

   }

   @Inject(
      method = {"receiveStalactiteDrip"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$receiveIchor(BlockState state, Level level, BlockPos pos, Fluid fluid, CallbackInfo ci) {
      if (fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR) {
         BlockState next = (BlockState)ModBlocks.ICHOR_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1);
         level.setBlockAndUpdate(pos, next);
         level.gameEvent(GameEvent.BLOCK_CHANGE, pos, Context.of(next));
         level.levelEvent(1047, pos, 0);
         ci.cancel();
      }
   }
}
