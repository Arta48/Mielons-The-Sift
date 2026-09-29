package mielon.thesift.client.mixin;

import mielon.thesift.client.loading.SiftLoadingScreenState;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen.Reason;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPacketListener.class})
public abstract class SiftLevelLoadingReasonMixin {
   @Inject(
      method = {"determineLevelLoadingReason"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void theSift$usePortalTransitionForSift(boolean playerDied, ResourceKey dimensionKey, ResourceKey oldDimensionKey, CallbackInfoReturnable cir) {
      boolean enteringSift = dimensionKey.equals(TheSiftDimension.LEVEL_KEY);
      boolean leavingSift = oldDimensionKey.equals(TheSiftDimension.LEVEL_KEY);
      boolean siftTransition = enteringSift != leavingSift;
      if (siftTransition) {
         SiftLoadingScreenState.markVanillaTransition(oldDimensionKey);
         cir.setReturnValue(Reason.END_PORTAL);
      }

   }
}
