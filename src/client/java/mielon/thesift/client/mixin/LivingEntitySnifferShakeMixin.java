package mielon.thesift.client.mixin;

import mielon.thesift.client.entity.SnifferInfectionRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntitySnifferShakeMixin {
   @Inject(
      method = {"isShaking"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$shakeInfectedSniffer(LivingEntityRenderState state, CallbackInfoReturnable cir) {
      if (state instanceof SnifferInfectionRenderState infectionState) {
         if (infectionState.theSift$isInfected()) {
            cir.setReturnValue(true);
         }
      }

   }
}
