package mielon.thesift.world;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PortalTransitGuard {
   private static final Map blocked = new HashMap();

   public static void block(Entity e) {
      e.getSelfAndPassengers().forEach((p) -> blocked.put(p.getUUID(), new Arrival(p.level().dimension(), p.position())));
   }

   public static boolean isBlocked(Entity e) {
      Arrival arrival = (Arrival)blocked.get(e.getUUID());
      if (arrival == null) {
         return false;
      } else if (arrival.dimension.equals(e.level().dimension()) && arrival.pos.distanceToSqr(e.position()) < (double)0.0625F) {
         return true;
      } else {
         if (!SiftTeleportManager.isTouchingPortal(e) && !RiftManager.isTouchingRift(e)) {
            blocked.remove(e.getUUID());
         }

         return true;
      }
   }

   public static void clear() {
      blocked.clear();
   }

   public static void prune(MinecraftServer server) {
      blocked.keySet().removeIf((id) -> server.overworld().getEntityInAnyDimension((UUID) id) == null);
   }

   private static record Arrival(ResourceKey dimension, Vec3 pos) {
   }
}
