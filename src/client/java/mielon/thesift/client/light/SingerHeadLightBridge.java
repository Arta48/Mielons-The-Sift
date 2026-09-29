package mielon.thesift.client.light;

import mielon.thesift.entity.SingerEntity;
import net.minecraft.world.phys.Vec3;

public final class SingerHeadLightBridge {
   private static final Handler NO_OP = (singer, position) -> {
   };
   private static volatile Handler handler;

   private SingerHeadLightBridge() {
   }

   public static void install(Handler newHandler) {
      handler = newHandler != null ? newHandler : NO_OP;
   }

   public static void update(SingerEntity singer, Vec3 position) {
      handler.update(singer, position);
   }

   static {
      handler = NO_OP;
   }

   @FunctionalInterface
   public interface Handler {
      void update(SingerEntity var1, Vec3 var2);
   }
}
