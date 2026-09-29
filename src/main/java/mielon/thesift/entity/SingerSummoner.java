package mielon.thesift.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.portal.PortalFrameScanner;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SingerSummoner {
   private static final int FRAME_RADIUS = 40;
   private static final int SONOROUS_RADIUS = 20;
   private static final double EXISTING_SINGER_RADIUS = (double)2.0F;
   private static final int SUMMON_DELAY_TICKS = 60;
   private static final List PENDING = new ArrayList();

   private SingerSummoner() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register((ServerTickEvents.EndTick)(server) -> tick());
      ServerLifecycleEvents.SERVER_STOPPED.register((ServerLifecycleEvents.ServerStopped)(server) -> PENDING.clear());
   }

   public static void onGoatHornFinished(ServerLevel level, Player player) {
      PENDING.add(new PendingSummon(level, player.position()));
   }

   public static boolean isNearAncientCityCenter(ServerLevel level, Vec3 position) {
      return findNearestFrame(level, position).isPresent();
   }

   private static void tick() {
      Iterator<PendingSummon> iterator = PENDING.iterator();

      while(iterator.hasNext()) {
         PendingSummon pending = (PendingSummon)iterator.next();
         if (pending.tick()) {
            iterator.remove();
         }
      }

   }

   private static void trySummon(ServerLevel level, Vec3 origin) {
      Optional<PortalFrameScanner.Frame> frameOptional = findNearestFrame(level, origin);
      if (!frameOptional.isEmpty()) {
         PortalFrameScanner.Frame frame = (PortalFrameScanner.Frame)frameOptional.get();
         Vec3 spawnPos = frame.bottomCenter();
         BlockPos portalCenter = BlockPos.containing(spawnPos);
         if (!hasSingerForPortal(level, portalCenter, spawnPos)) {
            BlockPos sonorous = findNearestSonorous(level, spawnPos);
            Direction facing = getCardinalDirection(spawnPos, sonorous);
            SingerEntity singer = new SingerEntity(ModEntities.SINGER, level);
            singer.setPortalCenter(portalCenter);
            singer.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            float yaw = facing.toYRot();
            singer.setYRot(yaw);
            singer.setYBodyRot(yaw);
            singer.yBodyRotO = yaw;
            singer.setYHeadRot(yaw);
            singer.yHeadRotO = yaw;
            singer.beginSequence();
            level.addFreshEntity(singer);
         }
      }
   }

   private static boolean hasSingerForPortal(ServerLevel level, BlockPos portalCenter, Vec3 spawnPos) {
      double radius = (double)2.0F;
      AABB box = new AABB(spawnPos.x - radius, spawnPos.y - radius, spawnPos.z - radius, spawnPos.x + radius, spawnPos.y + radius, spawnPos.z + radius);

      for (SingerEntity singer : level.getEntitiesOfClass(SingerEntity.class, box)) {
         BlockPos singerPortal = singer.getPortalCenter();
         if (portalCenter.equals(singerPortal)) {
            return true;
         }
      }

      return false;
   }

   private static Optional findNearestFrame(ServerLevel level, Vec3 origin) {
      double bestDistance = Double.MAX_VALUE;
      PortalFrameScanner.Frame best = null;
      Set<Vec3> seenCenters = new HashSet();
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
      int originX = Mth.floor(origin.x);
      int originY = Mth.floor(origin.y);
      int originZ = Mth.floor(origin.z);

      for (int dx = -40; dx <= 40; ++dx) {
         for (int dy = -40; dy <= 40; ++dy) {
            for (int dz = -40; dz <= 40; ++dz) {
               double distanceSquared = (double)dx * (double)dx + (double)dy * (double)dy + (double)dz * (double)dz;
               if (!(distanceSquared > (double)1600.0F)) {
                  cursor.set(originX + dx, originY + dy, originZ + dz);
                  if (level.getBlockState(cursor).is(Blocks.REINFORCED_DEEPSLATE)) {
                     Optional<PortalFrameScanner.Frame> candidate = PortalFrameScanner.scan(level, cursor);
                     if (!candidate.isEmpty()) {
                        PortalFrameScanner.Frame frame = (PortalFrameScanner.Frame)candidate.get();
                        if (seenCenters.add(frame.bottomCenter())) {
                           double distance = frame.bottomCenter().distanceToSqr(origin);
                           if (distance <= (double)1600.0F && distance < bestDistance) {
                              bestDistance = distance;
                              best = frame;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return Optional.ofNullable(best);
   }

   private static BlockPos findNearestSonorous(ServerLevel level, Vec3 origin) {
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;
      int originX = Mth.floor(origin.x);
      int originY = Mth.floor(origin.y);
      int originZ = Mth.floor(origin.z);

      for (int dx = -20; dx <= 20; ++dx) {
         for (int dy = -20; dy <= 20; ++dy) {
            for (int dz = -20; dz <= 20; ++dz) {
               double distanceSquared = (double)dx * (double)dx + (double)dy * (double)dy + (double)dz * (double)dz;
               if (!(distanceSquared > (double)400.0F)) {
                  cursor.set(originX + dx, originY + dy, originZ + dz);
                  if (level.getBlockState(cursor).is(ModBlocks.SONOROUS_DEEPSLATE)) {
                     Vec3 blockCenter = Vec3.atCenterOf(cursor);
                     double distance = blockCenter.distanceToSqr(origin);
                     if (distance < bestDistance) {
                        bestDistance = distance;
                        best = cursor.immutable();
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private static Direction getCardinalDirection(Vec3 origin, BlockPos target) {
      if (target == null) {
         return Direction.SOUTH;
      } else {
         double dx = (double)target.getX() + (double)0.5F - origin.x;
         double dz = (double)target.getZ() + (double)0.5F - origin.z;
         if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= (double)0.0F ? Direction.EAST : Direction.WEST;
         } else {
            return dz >= (double)0.0F ? Direction.SOUTH : Direction.NORTH;
         }
      }
   }

   private static final class PendingSummon {
      private final ServerLevel level;
      private final Vec3 origin;
      private int ticksLeft = 60;

      PendingSummon(ServerLevel level, Vec3 origin) {
         this.level = level;
         this.origin = origin;
      }

      boolean tick() {
         if (this.ticksLeft > 0) {
            --this.ticksLeft;
            return false;
         } else {
            SingerSummoner.trySummon(this.level, this.origin);
            return true;
         }
      }
   }
}
