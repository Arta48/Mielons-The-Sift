package mielon.thesift.client.mixin;

import mielon.thesift.client.loading.SiftLoadingScreenState;
import mielon.thesift.client.loading.SiftTransitionScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Screen.class})
public abstract class SiftScreenLifecycleMixin {
   @Inject(
      method = {"removed"},
      at = {@At("TAIL")}
   )
   private void theSift$clearAfterLoadingScreenIsRemoved(CallbackInfo ci) {
      if ((Object)this instanceof LevelLoadingScreen && !((Object)this instanceof SiftTransitionScreen) && SiftLoadingScreenState.isActive()) {
         SiftLoadingScreenState.clear();
      }

   }
}
