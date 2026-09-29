package mielon.thesift.world;

public final class RiftAnimationClock {
   public static final double DURATION_TICKS = 15.834;

   public static float progress(boolean closing, double elapsedTicks) {
      double t = Math.max((double)0.0F, Math.min((double)1.0F, elapsedTicks / 15.834));
      return (float)(closing ? (double)1.0F - t : t);
   }

   private RiftAnimationClock() {
   }
}
