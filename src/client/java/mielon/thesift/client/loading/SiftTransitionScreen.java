package mielon.thesift.client.loading;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.LevelLoadingScreen.Reason;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class SiftTransitionScreen extends LevelLoadingScreen {
   private static final double FAILSAFE_SECONDS = (double)45.0F;
   private boolean realTracker;

   public boolean hasRealTracker() {
      return this.realTracker;
   }

   public void update(LevelLoadTracker tracker, LevelLoadingScreen.Reason reason) {
      super.update(tracker, reason);
      this.realTracker = true;
   }

   public SiftTransitionScreen() {
      super(new LevelLoadTracker(), Reason.END_PORTAL);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      SiftLoadingBackgroundRenderer.draw(graphics, this.width, this.height);
   }

   public void tick() {
      if (SiftLoadingScreenState.isActive() && this.minecraft.player != null) {
         ResourceKey<Level> source = SiftLoadingScreenState.sourceDimension();
         if (source != null && !this.minecraft.player.level().dimension().equals(source)) {
            SiftLoadingScreenState.clear();
            if (this.minecraft.gui.screen() == this) {
               this.minecraft.gui.setScreen((Screen)null);
            }

            return;
         }
      }

      if (this.realTracker) {
         super.tick();
      } else {
         if (!SiftLoadingScreenState.isActive() || SiftLoadingScreenState.isOlderThanSeconds((double)45.0F)) {
            SiftLoadingScreenState.clear();
            if (this.minecraft.gui.screen() == this) {
               this.minecraft.gui.setScreen((Screen)null);
            }
         }

      }
   }
}
