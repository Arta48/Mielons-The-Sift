package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SoulCanyonFeature implements Feature {
   public static final MapCodec CODEC = MapCodec.unit(SoulCanyonFeature::new);

   public MapCodec codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      if (!SiftLandmarkPlacement.isSelectedChunk(level.getSeed(), origin, 12, 6003110614342389337L, 1)) {
         return false;
      } else {
         int centerX = (origin.getX() & -16) + 8;
         int centerZ = (origin.getZ() & -16) + 8;
         if (SiftFeaturePlacementGuard.intersectsPortal(new BlockPos(centerX, 0, centerZ), 34)) {
            return false;
         } else {
            SiftWorldgenBounds bounds = SiftWorldgenBounds.around(origin);
            int length = 49 + random.nextInt(3);
            int baseRadius = 4 + random.nextInt(2);
            int maximumDepth = 22 + random.nextInt(5);
            double phase = random.nextDouble() * Math.PI * (double)2.0F;
            int firstOrientation = random.nextInt(4);
            CanyonPlan plan = null;
            List<BlockPos> soulBlocks = null;

            for (int attempt = 0; attempt < 8 && plan == null; ++attempt) {
               int orientation = firstOrientation + attempt & 3;
               double candidatePhase = phase + (double)(attempt / 4) * 1.941611038725466;
               CanyonPlan candidate = buildPlan(level, bounds, centerX, centerZ, length, baseRadius, maximumDepth, candidatePhase, orientation);
               if (candidate != null && canCarve(level, candidate.columns().values())) {
                  List<BlockPos> candidateSoulBlocks = selectSoulVeins(candidate, candidatePhase, maximumDepth);
                  if (candidateSoulBlocks.size() >= 8) {
                     plan = candidate;
                     soulBlocks = candidateSoulBlocks;
                  }
               }
            }

            if (plan != null && soulBlocks != null) {
               int minX = Integer.MAX_VALUE;
               int minZ = Integer.MAX_VALUE;
               int maxX = Integer.MIN_VALUE;
               int maxZ = Integer.MIN_VALUE;

               for (CanyonColumn column : (Iterable<CanyonColumn>) plan.columns().values()) {
                  minX = Math.min(minX, column.x());
                  minZ = Math.min(minZ, column.z());
                  maxX = Math.max(maxX, column.x());
                  maxZ = Math.max(maxZ, column.z());
               }

               if (!SiftFeaturePlacementGuard.tryReserveCarver(minX - 2, minZ - 2, maxX + 2, maxZ + 2)) {
                  return false;
               } else {
                  for (CanyonColumn column : (Iterable<CanyonColumn>) plan.columns().values()) {
                     BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

                     for (int y = column.floorY() + 1; y <= column.surfaceY() + 2; ++y) {
                        cursor.set(column.x(), y, column.z());
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                     }
                  }

                  for (BlockPos soul : soulBlocks) {
                     level.setBlock(soul, ModBlocks.SOUL_BLOCK.defaultBlockState(), 2);
                     SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SOUL_CANYON_SOUL_BLOCK, soul);
                  }

                  SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SOUL_CANYON_ENTRANCE, plan.entrance());
                  SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SOUL_CANYON_MIDPOINT, plan.midpoint());
                  SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SOUL_CANYON_CLUSTER, clusterCenter(soulBlocks));
                  return true;
               }
            } else {
               return false;
            }
         }
      }
   }

   private static CanyonPlan buildPlan(WorldGenLevel level, SiftWorldgenBounds bounds, int centerX, int centerZ, int length, int baseRadius, int maximumDepth, double phase, int orientation) {
      double directionX = (orientation & 1) == 0 ? 0.7071067811865476 : -0.7071067811865476;
      double directionZ = (orientation & 2) == 0 ? 0.7071067811865476 : -0.7071067811865476;
      double sideX = -directionZ;
      double sideZ = directionX;
      int halfLength = length / 2;
      Map<Long, CanyonColumn> columns = new HashMap();
      BlockPos entrance = null;
      BlockPos midpoint = null;
      int previousCenterFloor = Integer.MIN_VALUE;
      int previousCenterSurface = Integer.MIN_VALUE;

      for (int step = 0; step < length; ++step) {
         int offset = step - halfLength;
         double meander = Math.sin(phase + (double)step * 0.22) * 1.65 + Math.sin(phase * 1.73 + (double)step * 0.071) * 0.7;
         int radius = Math.max(3, baseRadius + (int)Math.round(Math.sin(phase * 0.63 + (double)step * 0.39)));
         double exactCenterX = (double)centerX + directionX * (double)offset + sideX * meander;
         double exactCenterZ = (double)centerZ + directionZ * (double)offset + sideZ * meander;
         int centerXAtStep = (int)Math.round(exactCenterX);
         int centerZAtStep = (int)Math.round(exactCenterZ);
         int centerSurface = findTerrainSurfaceBlockY(level, centerXAtStep, centerZAtStep);
         if (centerSurface < 0) {
            return null;
         }

         if (previousCenterSurface != Integer.MIN_VALUE && Math.abs(centerSurface - previousCenterSurface) > 6) {
            return null;
         }

         previousCenterSurface = centerSurface;
         double progress = (double)step / (double)Math.max(1, length - 1);
         int rampDepth = Math.min(maximumDepth, 1 + (int)Math.round((double)step * 0.67));
         double farTaper = smootherStep(clamp01(((double)1.0F - progress) / 0.34));
         int centerFloorDepth = 1 + (int)Math.round((double)(rampDepth - 1) * farTaper);
         int desiredCenterFloor = centerSurface - centerFloorDepth;
         int intendedCenterFloor = previousCenterFloor == Integer.MIN_VALUE ? desiredCenterFloor : Math.max(previousCenterFloor - 1, Math.min(previousCenterFloor + 1, desiredCenterFloor));
         if (centerSurface <= intendedCenterFloor) {
            return null;
         }

         if (centerSurface - intendedCenterFloor > maximumDepth + 14) {
            return null;
         }

         previousCenterFloor = intendedCenterFloor;
         BlockPos routeFeet = new BlockPos(centerXAtStep, intendedCenterFloor + 1, centerZAtStep);
         if (step == 0) {
            entrance = routeFeet;
         }

         if (step == Math.round((float)(length - 1) * 0.38F)) {
            midpoint = routeFeet;
         }

         int rasterRadius = radius + 2;
         int minX = (int)Math.floor(exactCenterX) - rasterRadius;
         int maxX = (int)Math.ceil(exactCenterX) + rasterRadius;
         int minZ = (int)Math.floor(exactCenterZ) - rasterRadius;
         int maxZ = (int)Math.ceil(exactCenterZ) + rasterRadius;

         for (int x = minX; x <= maxX; ++x) {
            for (int z = minZ; z <= maxZ; ++z) {
               double relativeX = (double)x - exactCenterX;
               double relativeZ = (double)z - exactCenterZ;
               double alongDistance = Math.abs(relativeX * directionX + relativeZ * directionZ);
               double signedSide = relativeX * sideX + relativeZ * sideZ;
               double sideDistance = Math.abs(signedSide);
               if (!(alongDistance > 0.82) && !(sideDistance > (double)radius + 0.35)) {
                  if (!bounds.contains(x, z)) {
                     return null;
                  }

                  double sideRatio = sideDistance / ((double)radius + 0.35);
                  double arch = Math.sqrt(Math.max((double)0.0F, (double)1.0F - sideRatio * sideRatio));
                  int surfaceY = findTerrainSurfaceBlockY(level, x, z);
                  if (surfaceY < 0) {
                     return null;
                  }

                  boolean routeCorridor = sideDistance <= 1.55;
                  if (routeCorridor && (surfaceY < intendedCenterFloor || Math.abs(surfaceY - centerSurface) > 7)) {
                     return null;
                  }

                  int centerDepth = surfaceY - intendedCenterFloor;
                  int depth = Math.max(1, (int)Math.round((double)centerDepth * arch));
                  if (!(sideDistance > (double)radius - 0.35) || !(hashUnit(x, z, maximumDepth) < 0.18)) {
                     int floorY = routeCorridor ? intendedCenterFloor : surfaceY - depth;
                     if (floorY <= level.getMinY() + 3) {
                        return null;
                     }

                     CanyonColumn column = new CanyonColumn(x, z, surfaceY, floorY, surfaceY - floorY, (double)step / (double)Math.max(1, length - 1), step, (int)Math.round(sideDistance), signedSide);
                     long key = BlockPos.asLong(x, 0, z);
                     CanyonColumn previous = (CanyonColumn)columns.get(key);
                     if (previous == null || column.floorY() < previous.floorY()) {
                        columns.put(key, column);
                     }
                  }
               }
            }
         }
      }

      BlockPos verifiedEntrance = routeFeet(columns, entrance);
      BlockPos verifiedMidpoint = routeFeet(columns, midpoint);
      return verifiedEntrance != null && verifiedMidpoint != null ? new CanyonPlan(columns, verifiedEntrance, verifiedMidpoint, length, baseRadius) : null;
   }

   private static BlockPos routeFeet(Map columns, BlockPos marker) {
      if (marker == null) {
         return null;
      } else {
         CanyonColumn column = (CanyonColumn)columns.get(BlockPos.asLong(marker.getX(), 0, marker.getZ()));
         return column == null ? null : new BlockPos(marker.getX(), column.floorY() + 1, marker.getZ());
      }
   }

   private static List selectSoulVeins(CanyonPlan plan, double phase, int maximumDepth) {
      List<BlockPos> result = new ArrayList();
      Set<Long> reachableFloor = reachableFloorFromRamp(plan);

      for (CanyonColumn column : (Iterable<CanyonColumn>) plan.columns().values()) {
         if (!((double)column.depth() < (double)maximumDepth * (double)0.5F) && !(column.progress() < 0.53) && !(column.progress() > 0.86) && column.sideDistance() <= plan.baseRadius() && reachableFloor.contains(BlockPos.asLong(column.x(), 0, column.z())) && hasGolemSizedApproach(plan, column)) {
            double along = (double)column.alongStep();
            double veinCenter = Math.sin(phase + along * 0.31) * 1.15 + Math.sin(phase * 0.71 + along * 0.13) * 0.48;
            double width = 0.82 + (Math.sin(phase * 1.37 + along * 0.43) + (double)1.0F) * 0.24;
            double edgeRatio = Math.abs(column.signedSide() - veinCenter) / width;
            double grain = hashUnit(column.x(), column.z(), maximumDepth * 31);
            boolean windingVein = edgeRatio <= (double)1.0F && (edgeRatio < 0.48 || grain > 0.2 + edgeRatio * 0.42);
            double pocketOne = square((column.progress() - 0.63) / 0.055) + square((column.signedSide() + 1.65 + Math.sin(phase) * 0.55) / 1.55);
            double pocketTwo = square((column.progress() - 0.77) / 0.048) + square((column.signedSide() - 1.35 - Math.cos(phase) * 0.45) / 1.35);
            boolean pocket = Math.min(pocketOne, pocketTwo) <= (double)1.0F && grain > 0.16;
            if (windingVein || pocket) {
               result.add(new BlockPos(column.x(), column.floorY(), column.z()));
            }
         }
      }

      return result;
   }

   private static Set reachableFloorFromRamp(CanyonPlan plan) {
      Set<Long> visited = new HashSet();
      ArrayDeque<CanyonColumn> queue = new ArrayDeque();
      CanyonColumn start = (CanyonColumn)plan.columns().get(BlockPos.asLong(plan.midpoint().getX(), 0, plan.midpoint().getZ()));
      if (start == null) {
         return visited;
      } else {
         visited.add(BlockPos.asLong(start.x(), 0, start.z()));
         queue.add(start);

         while(!queue.isEmpty()) {
            CanyonColumn current = (CanyonColumn)queue.removeFirst();

            for (Direction direction : Plane.HORIZONTAL) {
               int x = current.x() + direction.getStepX();
               int z = current.z() + direction.getStepZ();
               long key = BlockPos.asLong(x, 0, z);
               CanyonColumn next = (CanyonColumn)plan.columns().get(key);
               if (next != null && !visited.contains(key) && Math.abs(next.floorY() - current.floorY()) <= 1) {
                  visited.add(key);
                  queue.addLast(next);
               }
            }
         }

         return visited;
      }
   }

   private static boolean hasGolemSizedApproach(CanyonPlan plan, CanyonColumn target) {
      for (int startX : new int[]{target.x() - 1, target.x()}) {
         for (int startZ : new int[]{target.z() - 1, target.z()}) {
            boolean valid = true;
            boolean hasStep = false;

            for (int dx = 0; dx <= 1 && valid; ++dx) {
               for (int dz = 0; dz <= 1; ++dz) {
                  CanyonColumn column = (CanyonColumn)plan.columns().get(BlockPos.asLong(startX + dx, 0, startZ + dz));
                  if (column == null || column.floorY() > target.floorY() || column.floorY() < target.floorY() - 1) {
                     valid = false;
                     break;
                  }

                  if (column.x() != target.x() || column.z() != target.z()) {
                     hasStep = true;
                  }
               }
            }

            if (valid && hasStep) {
               return true;
            }
         }
      }

      return false;
   }

   private static BlockPos clusterCenter(List positions) {
      long x = 0L;
      long y = 0L;
      long z = 0L;

      for (BlockPos pos : (Iterable<BlockPos>) positions) {
         x += (long)pos.getX();
         y += (long)pos.getY();
         z += (long)pos.getZ();
      }

      int size = positions.size();
      return new BlockPos((int)(x / (long)size), (int)(y / (long)size), (int)(z / (long)size));
   }

   private static boolean canCarve(WorldGenLevel level, Iterable columns) {
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (CanyonColumn column : (Iterable<CanyonColumn>) columns) {
         cursor.set(column.x(), column.floorY(), column.z());
         if (!isSiftTerrain(level.getBlockState(cursor))) {
            return false;
         }

         for (int y = column.floorY() + 1; y <= column.surfaceY() + 2; ++y) {
            cursor.set(column.x(), y, column.z());
            if (!isCarvable(level.getBlockState(cursor)) || !level.ensureCanWrite(cursor)) {
               return false;
            }
         }
      }

      return true;
   }

   private static int findTerrainSurfaceBlockY(WorldGenLevel level, int x, int z) {
      int top = level.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1;
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, top, z);
      int minimum = Math.max(level.getMinY() + 1, top - 64);

      for (int y = top; y >= minimum; --y) {
         cursor.setY(y);
         if (isSiftTerrain(level.getBlockState(cursor))) {
            return y;
         }
      }

      return -1;
   }

   private static boolean isCarvable(BlockState state) {
      return state.isAir() || isSiftTerrain(state) || state.is(ModBlocks.ICHOR_SNOW) || state.is(ModBlocks.ICHOR_SNOW_BLOCK) || state.is(Blocks.SCULK_VEIN) || state.is(Blocks.SCULK_SENSOR) || state.is(Blocks.SCULK_SHRIEKER) || state.is(Blocks.SCULK_CATALYST) || state.is(ModBlocks.OVERGROWN_FRONDS) || state.is(ModBlocks.OVERGROWN_CHARD) || state.is(ModBlocks.OVERGROWN_STALKS) || state.is(ModBlocks.SIFTSLATE_STALKS) || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS) || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS) || state.is(ModBlocks.SIFTSLATE_HANGING_ROOTS) || state.is(ModBlocks.OVERGROWN_HANGING_ROOTS);
   }

   private static boolean isSiftTerrain(BlockState state) {
      return state.is(ModBlocks.SIFTSLATE) || state.is(ModBlocks.SIFTSLATE_GROWTH) || state.is(ModBlocks.HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH) || state.is(ModBlocks.SIFTSLATE_COAL_ORE) || state.is(ModBlocks.SIFTSLATE_DIAMOND_ORE) || state.is(ModBlocks.SIFTSLATE_EMERALD_ORE) || state.is(ModBlocks.SIFTSLATE_CHAROITE_ORE) || state.is(ModBlocks.SIFTSLATE_SIFTITE_ORE) || state.is(Blocks.SCULK) || state.is(ModBlocks.SOUL_BLOCK);
   }

   private static double square(double value) {
      return value * value;
   }

   private static double clamp01(double value) {
      return Math.max((double)0.0F, Math.min((double)1.0F, value));
   }

   private static double smootherStep(double value) {
      return value * value * value * (value * (value * (double)6.0F - (double)15.0F) + (double)10.0F);
   }

   private static double hashUnit(int x, int z, int salt) {
      long value = (long)x * -7046029254386353131L ^ (long)z * -4417276706812531889L ^ (long)salt * -7723592293110705685L;
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      return (double)((value ^ value >>> 31) >>> 11) * (double)1.110223E-16F;
   }

   private static record CanyonPlan(Map columns, BlockPos entrance, BlockPos midpoint, int length, int baseRadius) {
   }

   private static record CanyonColumn(int x, int z, int surfaceY, int floorY, int depth, double progress, int alongStep, int sideDistance, double signedSide) {
   }
}
