package mielon.thesift.client.loading;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.entity.RiftEntity;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class SiftClientPortalTransition {
   private static LocalPlayer lastPlayer;
   private static boolean initialized;
   private static boolean waitingForExit;
   private static boolean suppressPortalUntilExit;
   private static ResourceKey riftSourceDimension;

   private SiftClientPortalTransition() {
   }

   public static void beginRiftTransition(LocalPlayer player) {
      suppressPortalUntilExit = true;
      riftSourceDimension = player.level().dimension();
      waitingForExit = true;
   }

   public static void endRiftTransition(LocalPlayer player) {
      if (suppressPortalUntilExit && (riftSourceDimension == null || player.level().dimension().equals(riftSourceDimension))) {
         clearRiftSuppression();
      }

   }

   public static void tick(LocalPlayer player) {
      Minecraft minecraft = Minecraft.getInstance();
      if (SiftLoadingScreenState.isActive()) {
         Screen currentScreen = minecraft.gui.screen();
         boolean validTransitionScreen = currentScreen instanceof SiftTransitionScreen || currentScreen instanceof LevelLoadingScreen;
         if (!validTransitionScreen || SiftLoadingScreenState.isOlderThanSeconds((double)45.0F)) {
            SiftLoadingScreenState.clear();
         }
      }

      if (SiftLoadingScreenState.isActive() && minecraft.gui.screen() instanceof LevelLoadingScreen) {
         ResourceKey<Level> source = SiftLoadingScreenState.sourceDimension();
         if (source != null && !player.level().dimension().equals(source)) {
            SiftLoadingScreenState.clear();
            minecraft.gui.setScreen((Screen)null);
            return;
         }
      }

      if (player != lastPlayer) {
         lastPlayer = player;
         initialized = false;
         waitingForExit = suppressPortalUntilExit;
      }

      boolean supportedDimension = player.level().dimension().equals(Level.OVERWORLD) || player.level().dimension().equals(TheSiftDimension.LEVEL_KEY);
      boolean touchingPortal = supportedDimension && isTouchingPortal(player);
      boolean touchingAny = touchingPortal || !player.level().getEntitiesOfClass(RiftEntity.class, player.getBoundingBox().inflate((double)5.0F), (rift) -> rift.getPortalBounds().intersects(player.getBoundingBox())).isEmpty();
      if (suppressPortalUntilExit) {
         boolean changedDimension = riftSourceDimension != null && !player.level().dimension().equals(riftSourceDimension);
         if (!changedDimension || touchingAny) {
            waitingForExit = true;
            return;
         }

         clearRiftSuppression();
         waitingForExit = false;
      }

      if (player.isSpectator()) {
         if (touchingPortal) {
            waitingForExit = true;
         }

         if (minecraft.gui.screen() instanceof SiftTransitionScreen) {
            SiftLoadingScreenState.clear();
            minecraft.gui.setScreen((Screen)null);
         }

      } else if (!initialized) {
         initialized = true;
         waitingForExit = touchingAny;
      } else if (waitingForExit) {
         if (!touchingAny) {
            waitingForExit = false;
         }

      } else if (touchingPortal) {
         waitingForExit = true;
         SiftLoadingScreenState.begin(player.level().dimension());
         if (!(minecraft.gui.screen() instanceof SiftTransitionScreen)) {
            minecraft.gui.setScreen(new SiftTransitionScreen());
         }

      }
   }

   private static void clearRiftSuppression() {
      suppressPortalUntilExit = false;
      riftSourceDimension = null;
   }

   private static boolean isTouchingPortal(LocalPlayer player) {
      AABB box = player.getBoundingBox().deflate(1.0E-7);
      int minX = Mth.floor(box.minX);
      int minY = Mth.floor(box.minY);
      int minZ = Mth.floor(box.minZ);
      int maxX = Mth.floor(box.maxX);
      int maxY = Mth.floor(box.maxY);
      int maxZ = Mth.floor(box.maxZ);
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int x = minX; x <= maxX; ++x) {
         for (int y = minY; y <= maxY; ++y) {
            for (int z = minZ; z <= maxZ; ++z) {
               pos.set(x, y, z);
               if (player.level().getBlockState(pos).is(ModBlocks.SIFT_PORTAL)) {
                  return true;
               }
            }
         }
      }

      return false;
   }
}
