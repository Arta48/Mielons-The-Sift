package mielon.thesift.mixin;

import mielon.thesift.fluid.IchorWaterlogging;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FlowingFluid.class})
public abstract class IchorFlowAcceptanceMixin {
   @Inject(
      method = {"canHoldFluid", "canHoldSpecificFluid"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void theSift$acceptIchor(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid, CallbackInfoReturnable cir) {
      if (IchorWaterlogging.isIchor(fluid) && IchorWaterlogging.canIchorlog(state)) {
         cir.setReturnValue(IchorWaterlogging.canFill(state));
      }

   }
}
