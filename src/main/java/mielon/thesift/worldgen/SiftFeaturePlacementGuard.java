package mielon.thesift.worldgen;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;

public final class SiftFeaturePlacementGuard {
   private static final int PORTAL_MARGIN = 15;
   private static final int MAX_RECENT_RESERVATIONS = 4096;
   private static volatile ProtectedRectangle portalRectangle;
   private static final Deque recentReservations = new ArrayDeque();

   private SiftFeaturePlacementGuard() {
   }

   public static void reservePortal(BlockPos structureOrigin, int sizeX, int sizeZ) {
      portalRectangle = new ProtectedRectangle(structureOrigin.getX() - 15, structureOrigin.getZ() - 15, structureOrigin.getX() + sizeX - 1 + 15, structureOrigin.getZ() + sizeZ - 1 + 15);
   }

   public static boolean intersectsPortal(int minX, int minZ, int maxX, int maxZ) {
      ProtectedRectangle rectangle = portalRectangle;
      return rectangle != null && maxX >= rectangle.minX && minX <= rectangle.maxX && maxZ >= rectangle.minZ && minZ <= rectangle.maxZ;
   }

   public static boolean intersectsPortal(BlockPos center, int horizontalRadius) {
      return intersectsPortal(center.getX() - horizontalRadius, center.getZ() - horizontalRadius, center.getX() + horizontalRadius, center.getZ() + horizontalRadius);
   }

   public static synchronized boolean tryReserveCarver(int minX, int minZ, int maxX, int maxZ) {
      return tryReserve(new ProtectedRectangle(minX, minZ, maxX, maxZ), SiftFeaturePlacementGuard.FeatureKind.CARVER);
   }

   public static synchronized boolean tryReserveSolid(int minX, int minZ, int maxX, int maxZ) {
      return tryReserve(new ProtectedRectangle(minX, minZ, maxX, maxZ), SiftFeaturePlacementGuard.FeatureKind.SOLID);
   }

   private static boolean tryReserve(ProtectedRectangle candidate, FeatureKind kind) {
      if (intersectsPortal(candidate.minX, candidate.minZ, candidate.maxX, candidate.maxZ)) {
         return false;
      } else {
         for (FeatureReservation reservation : (Iterable<FeatureReservation>) recentReservations) {
            if (candidate.intersects(reservation.rectangle())) {
               return false;
            }
         }

         recentReservations.addLast(new FeatureReservation(candidate, kind));

         while(recentReservations.size() > 4096) {
            recentReservations.removeFirst();
         }

         return true;
      }
   }

   public static void clear() {
      portalRectangle = null;
      synchronized(SiftFeaturePlacementGuard.class) {
         recentReservations.clear();
      }
   }

   private static enum FeatureKind {
      CARVER,
      SOLID;

      // $FF: synthetic method
      private static FeatureKind[] $values() {
         return new FeatureKind[]{CARVER, SOLID};
      }
   }

   private static record FeatureReservation(ProtectedRectangle rectangle, FeatureKind kind) {
   }

   private static record ProtectedRectangle(int minX, int minZ, int maxX, int maxZ) {
      boolean intersects(ProtectedRectangle other) {
         return this.maxX >= other.minX && this.minX <= other.maxX && this.maxZ >= other.minZ && this.minZ <= other.maxZ;
      }
   }
}
