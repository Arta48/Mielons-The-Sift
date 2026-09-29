package mielon.thesift.client.mixin;

import mielon.thesift.client.entity.WardenPettingRenderState;
import mielon.thesift.entity.WardenPettingAccess;
import net.minecraft.client.renderer.entity.WardenRenderer;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WardenRenderer.class})
public abstract class WardenRendererMixin {
   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/monster/warden/Warden;Lnet/minecraft/client/renderer/entity/state/WardenRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void theSift$copyPettingState(Warden warden, WardenRenderState state, float partialTick, CallbackInfo ci) {
      ((WardenPettingRenderState)state).theSift$setPettingDarkSniffer(((WardenPettingAccess)warden).theSift$isPettingDarkSniffer());
   }
}
