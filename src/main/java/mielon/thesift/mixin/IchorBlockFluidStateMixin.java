package mielon.thesift.mixin;

import mielon.thesift.fluid.IchorState;
import mielon.thesift.fluid.IchorWaterlogging;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockBehaviour.BlockStateBase.class})
public abstract class IchorBlockFluidStateMixin {
   @Inject(
      method = {"getFluidState"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$actualFluid(CallbackInfoReturnable cir) {
      if (IchorState.source != null && IchorWaterlogging.isIchorlogged((BlockState)(Object)this)) {
         cir.setReturnValue(IchorState.source);
      }

   }
}
