package mielon.thesift.entity;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

public final class SoulExchangeManager {
   private SoulExchangeManager() {
   }

   public static SingerEntity requestSinger(ServerLevel level, EchoGolemEntity golem) {
      if (golem.hasSoulBlock() && golem.canInteractWithSinger()) {
         AABB search = golem.getBoundingBox().inflate((double)52.0F, (double)32.0F, (double)52.0F);
         Stream var10000 = level.getEntitiesOfClass(SingerEntity.class, search, SingerEntity::isSoulEvent).stream();
         Objects.requireNonNull(golem);
         SingerEntity existing = (SingerEntity)var10000.min(Comparator.comparingDouble((SingerEntity s) -> golem.distanceToSqr(s))).orElse(null);
         if (existing != null) {
            if (!golem.tryAcquirePathBudget(level, 6)) {
               return null;
            }

            Path path = golem.getNavigation().createPath(existing.blockPosition(), 2);
            if (path == null || !path.canReach()) {
               existing = null;
            }
         }

         if (existing != null) {
            existing.offerSoulGolem(golem);
            return existing;
         } else {
            BlockPos spawn = findNaturalSpawn(level, golem.blockPosition());
            if (spawn == null) {
               return null;
            } else {
               SingerEntity singer = (SingerEntity)ModEntities.SINGER.create(level, EntitySpawnReason.EVENT);
               if (singer == null) {
                  return null;
               } else {
                  singer.snapTo((double)spawn.getX() + (double)0.5F, (double)spawn.getY(), (double)spawn.getZ() + (double)0.5F, level.getRandom().nextFloat() * 360.0F, 0.0F);
                  singer.beginSoulEvent(golem.getUUID());
                  singer.setPersistenceRequired();
                  if (!level.addFreshEntity(singer)) {
                     singer.discard();
                     return null;
                  } else {
                     return singer;
                  }
               }
            }
         }
      } else {
         return null;
      }
   }

   private static BlockPos findNaturalSpawn(ServerLevel level, BlockPos around) {
      BlockPos activeCanyon = (BlockPos)SiftLandmarkTracker.nearestActiveSoulCanyon(level, around, (double)48.0F).orElse(null);
      double phase = level.getRandom().nextDouble() * Math.PI * (double)2.0F;

      for (int attempt = 0; attempt < 16; ++attempt) {
         double angle = phase + (double)attempt * (Math.PI / 8D);
         int radius = 7 + level.getRandom().nextInt(6);
         int x = around.getX() + (int)Math.round(Math.cos(angle) * (double)radius);
         int z = around.getZ() + (int)Math.round(Math.sin(angle) * (double)radius);
         int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         BlockPos feet = new BlockPos(x, y, z);
         if ((activeCanyon == null || !(horizontalDistanceSqr(feet, activeCanyon) < (double)576.0F)) && level.isLoaded(feet) && level.getBlockState(feet.below()).isCollisionShapeFullBlock(level, feet.below())) {
            boolean clear = true;

            for (int dy = 0; dy <= 4; ++dy) {
               if (!level.getBlockState(feet.above(dy)).isAir()) {
                  clear = false;
                  break;
               }
            }

            if (clear) {
               return feet;
            }
         }
      }

      return null;
   }

   private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
      double dx = (double)(first.getX() - second.getX());
      double dz = (double)(first.getZ() - second.getZ());
      return dx * dx + dz * dz;
   }
}
