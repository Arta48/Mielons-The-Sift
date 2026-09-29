package mielon.thesift.mixin;

import mielon.thesift.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PointedDripstoneBlock.class})
public abstract class PointedDripstoneIchorMixin {
   @Inject(
      method = {"maybeTransferFluid"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void theSift$transferIchor(BlockState state, ServerLevel level, BlockPos pos, float chance, CallbackInfo ci) {
      if (theSift$hasIchorSourceAbove(level, pos)) {
         ci.cancel();
         if (!(chance >= 0.17578125F)) {
            BlockPos tip = SpeleothemInvoker.theSift$findTip(state, level, pos, 11, false);
            if (tip != null) {
               BlockPos cauldron = PointedDripstoneInvoker.theSift$findFillableCauldron(level, tip, ModFluids.ICHOR);
               if (cauldron != null) {
                  level.levelEvent(1504, tip, 0);
                  level.scheduleTick(cauldron, level.getBlockState(cauldron).getBlock(), 50 + tip.getY() - cauldron.getY());
               }
            }
         }
      }
   }

   @Inject(
      method = {"getCauldronFillFluidType"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private static void theSift$resolveIchorDrip(ServerLevel level, BlockPos tip, CallbackInfoReturnable cir) {
      if (cir.getReturnValue() == Fluids.EMPTY && theSift$hasIchorSourceAbove(level, tip)) {
         cir.setReturnValue(ModFluids.ICHOR);
      }

   }

   private static boolean theSift$hasIchorSourceAbove(ServerLevel level, BlockPos start) {
      for (int offset = 1; offset <= 13; ++offset) {
         Fluid fluid = level.getFluidState(start.above(offset)).getType();
         if (fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR) {
            return level.getFluidState(start.above(offset)).isSource();
         }
      }

      return false;
   }
}
