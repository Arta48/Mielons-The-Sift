package mielon.thesift.client.mixin;

import mielon.thesift.client.loading.SiftClientPortalTransition;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class SiftClientPortalTransitionMixin {
   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void theSift$showTransitionImmediately(CallbackInfo ci) {
      SiftClientPortalTransition.tick((LocalPlayer)(Object)this);
   }
}
