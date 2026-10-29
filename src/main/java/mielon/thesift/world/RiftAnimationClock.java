package mielon.thesift.world;

public final class RiftAnimationClock {
   public static final double DURATION_TICKS = 15.834;

   public static float progress(boolean closing, double elapsedTicks) {
      double t = Math.clamp(elapsedTicks / 15.834, 0.0F, 1.0F);
      return (float) (closing ? (double) 1.0F - t : t);
   }

   private RiftAnimationClock() {
   }
}
