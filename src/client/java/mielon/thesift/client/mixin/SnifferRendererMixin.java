package mielon.thesift.client.mixin;

import mielon.thesift.client.entity.SnifferInfectionRenderState;
import mielon.thesift.entity.SnifferInfectionAccess;
import net.minecraft.client.renderer.entity.SnifferRenderer;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SnifferRenderer.class})
public abstract class SnifferRendererMixin {
   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/animal/sniffer/Sniffer;Lnet/minecraft/client/renderer/entity/state/SnifferRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void theSift$copyInfectionState(Sniffer sniffer, SnifferRenderState state, float partialTick, CallbackInfo ci) {
      SnifferInfectionRenderState var10000;
      boolean var10001;
      label12: {
         var10000 = (SnifferInfectionRenderState)state;
         if (sniffer instanceof SnifferInfectionAccess access) {
            if (access.theSift$isSculkInfected()) {
               var10001 = true;
               break label12;
            }
         }

         var10001 = false;
      }

      var10000.theSift$setInfected(var10001);
   }
}
