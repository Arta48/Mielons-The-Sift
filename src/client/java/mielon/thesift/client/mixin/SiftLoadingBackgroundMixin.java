package mielon.thesift.client.mixin;

import mielon.thesift.client.loading.SiftLoadingBackgroundRenderer;
import mielon.thesift.client.loading.SiftLoadingScreenState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelLoadingScreen.class})
public abstract class SiftLoadingBackgroundMixin {
   @Inject(
      method = {"extractBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$extractPortalBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (SiftLoadingScreenState.isActive()) {
         LevelLoadingScreen screen = (LevelLoadingScreen)(Object)this;
         SiftLoadingBackgroundRenderer.draw(graphics, screen.width, screen.height);
         ci.cancel();
      }
   }

   @Inject(
      method = {"onClose"},
      at = {@At("TAIL")}
   )
   private void theSift$clearLoadingStateOnClose(CallbackInfo ci) {
      SiftLoadingScreenState.clear();
   }
}
