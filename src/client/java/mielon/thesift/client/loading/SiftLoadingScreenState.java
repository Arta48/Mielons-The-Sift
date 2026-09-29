package mielon.thesift.client.loading;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class SiftLoadingScreenState {
   private static boolean active;
   private static boolean rift;
   private static ResourceKey sourceDimension;
   private static long startedAtNanos;

   public static boolean isRift() {
      return rift;
   }

   private SiftLoadingScreenState() {
   }

   public static boolean isActive() {
      return active;
   }

   public static void begin(ResourceKey source) {
      begin(source, false);
   }

   public static void begin(ResourceKey source, boolean fromRift) {
      rift = fromRift;
      active = true;
      sourceDimension = source;
      startedAtNanos = System.nanoTime();
   }

   public static void markVanillaTransition(ResourceKey oldDimension) {
      if (!active) {
         begin(oldDimension);
      } else if (sourceDimension == null) {
         sourceDimension = oldDimension;
      }

   }

   public static ResourceKey sourceDimension() {
      return sourceDimension;
   }

   public static boolean isOlderThanSeconds(double seconds) {
      return active && startedAtNanos != 0L && (double)(System.nanoTime() - startedAtNanos) > seconds * (double)1.0E9F;
   }

   public static void clear() {
      active = false;
      rift = false;
      sourceDimension = null;
      startedAtNanos = 0L;
   }

   public static void setActive(boolean active) {
      if (active) {
         if (!SiftLoadingScreenState.active) {
            SiftLoadingScreenState.active = true;
            startedAtNanos = System.nanoTime();
         }
      } else {
         clear();
      }

   }
}
