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
      boolean infected = sniffer instanceof SnifferInfectionAccess access && access.theSift$isSculkInfected();
      ((SnifferInfectionRenderState) state).theSift$setInfected(infected);
   }
}
