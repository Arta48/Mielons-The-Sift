package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftArchFeature implements Feature {
   public static final MapCodec CODEC = MapCodec.unit(SiftArchFeature::new);
   private static final int[][] AXES = new int[][]{{1, 0}, {0, 1}, {1, 1}, {1, -1}};

   public MapCodec codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(origin);
      int centerX = (origin.getX() & -16) + 8 + random.nextInt(3) - 1;
      int centerZ = (origin.getZ() & -16) + 8 + random.nextInt(3) - 1;
      BlockPos featureCenter = new BlockPos(centerX, origin.getY(), centerZ);
      if (SiftFeaturePlacementGuard.intersectsPortal(featureCenter, 23)) {
         return false;
      } else {
         int centerY = SiftMonolithFeature.findTerrainSurface(level, centerX, centerZ);
         if (centerY < 0) {
            return false;
         } else {
            int halfSpan = 13 + random.nextInt(3);
            Candidate best = findBestSpan(level, bounds, centerX, centerZ, centerY, halfSpan, random);
            if (best == null) {
               return false;
            } else {
               int baselineMiddle = (best.leftY + best.rightY) / 2;
               int archRise = 7 + random.nextInt(6) + Math.min(5, best.valleyDepth / 5);
               int topAtMiddle = baselineMiddle + archRise + 5;
               if (topAtMiddle >= level.getMaxY()) {
                  archRise -= topAtMiddle - level.getMaxY() + 1;
               }

               if (archRise < 6) {
                  return false;
               } else {
                  BlockState leftFloor = level.getBlockState(best.leftFloor(centerX, centerZ));
                  BlockState rightFloor = level.getBlockState(best.rightFloor(centerX, centerZ));
                  boolean healthyGround = isHealthy(leftFloor) || isHealthy(rightFloor);
                  boolean growthGround = leftFloor.is(ModBlocks.SIFTSLATE_GROWTH) || rightFloor.is(ModBlocks.SIFTSLATE_GROWTH);
                  float healthyChance = healthyGround ? 0.78F : (growthGround ? 0.22F : 0.43F);
                  boolean healthy = random.nextFloat() < healthyChance;
                  BlockState body = (healthy ? ModBlocks.HEALTHY_SCULK : ModBlocks.SIFTSLATE).defaultBlockState();
                  boolean growthVariant = !healthy && growthGround;
                  double baseThickness = 2.45 + random.nextDouble() * 0.9;
                  int span = best.halfSpan * 2;
                  int steps = span * 2;
                  long shapeSalt = random.nextLong();
                  int blocksPlaced = 0;
                  SiftPackedLongSet archBlocks = new SiftPackedLongSet(4096);
                  List<BlockPos> crownPath = new ArrayList();

                  for (int step = 0; step <= steps; ++step) {
                     double t = (double)step / (double)steps;
                     CurvePoint point = curvePoint(best, centerX, centerZ, t, archRise, shapeSalt);
                     double endStrength = Math.pow(Math.abs(t * (double)2.0F - (double)1.0F), (double)4.0F);
                     double thicknessWave = 0.93 + 0.09 * Math.sin(t * Math.PI * (double)5.0F + phase(shapeSalt, 11)) + 0.045 * Math.sin(t * Math.PI * (double)9.0F + phase(shapeSalt, 29));
                     double localThickness = baseThickness * thicknessWave + endStrength * 1.15;
                     blocksPlaced += fillOrganicSphere(level, bounds, point, localThickness, body, healthy, step, shapeSalt, archBlocks);
                     if (step % 5 == 0) {
                        crownPath.add(new BlockPos((int)Math.round(point.x), (int)Math.round(point.y + localThickness), (int)Math.round(point.z)));
                     }
                  }

                  blocksPlaced += blendAnchorIntoTerrain(level, bounds, best, centerX, centerZ, true, baseThickness, body, healthy, shapeSalt ^ 1279608404L, archBlocks);
                  blocksPlaced += blendAnchorIntoTerrain(level, bounds, best, centerX, centerZ, false, baseThickness, body, healthy, shapeSalt ^ 353416726612L, archBlocks);
                  carveGouges(level, random, best, centerX, centerZ, archRise, baseThickness, shapeSalt, archBlocks);
                  if (growthVariant) {
                     coatExposedCrownWithGrowth(level, bounds, archBlocks);
                  }

                  for (BlockPos crownPos : crownPath) {
                     SiftSurfaceDecorator.decorate(level, random, crownPos, healthy ? 0.82F : 0.72F);
                  }

                  return blocksPlaced > 0;
               }
            }
         }
      }
   }

   private static int blendAnchorIntoTerrain(WorldGenLevel level, SiftWorldgenBounds bounds, Candidate candidate, int centerX, int centerZ, boolean left, double baseThickness, BlockState body, boolean healthy, long salt, SiftPackedLongSet archBlocks) {
      double t = left ? (double)0.0F : (double)1.0F;
      CurvePoint endpoint = curvePoint(candidate, centerX, centerZ, t, 0, salt);
      double outward = left ? (double)-1.0F : (double)1.0F;
      double perpendicularX = -candidate.axisZ;
      double perpendicularZ = candidate.axisX;
      int previousSurface = left ? candidate.leftY : candidate.rightY;
      int placed = 0;
      int samples = 5;

      for (int sample = 0; sample < samples; ++sample) {
         double progress = ((double)sample + (double)1.0F) / (double)samples;
         double distance = 0.85 + (double)sample * 0.78;
         double sideWave = Math.sin(progress * Math.PI * 2.4 + phase(salt, 13)) * 0.42 * ((double)1.0F - progress);
         double x = endpoint.x + candidate.axisX * outward * distance + perpendicularX * sideWave;
         double z = endpoint.z + candidate.axisZ * outward * distance + perpendicularZ * sideWave;
         int sampleX = (int)Math.round(x);
         int sampleZ = (int)Math.round(z);
         if (!bounds.contains(sampleX, sampleZ)) {
            break;
         }

         int surface = SiftMonolithFeature.findTerrainSurface(level, sampleX, sampleZ);
         if (surface < 0 || Math.abs(surface - previousSurface) > 9) {
            break;
         }

         previousSurface = surface;
         double eased = smootherstep(progress);
         double centerY = endpoint.y + ((double)surface - 0.35 - endpoint.y) * eased;
         double radius = baseThickness + 0.9 - progress * (baseThickness - 0.55) + Math.sin(progress * Math.PI * (double)3.0F + phase(salt, 31)) * 0.16;
         placed += fillOrganicSphere(level, bounds, new CurvePoint(x, centerY, z), Math.max((double)1.25F, radius), body, healthy, left ? -sample : sample, salt, archBlocks);
      }

      return placed;
   }

   private static void coatExposedCrownWithGrowth(WorldGenLevel level, SiftWorldgenBounds bounds, SiftPackedLongSet archBlocks) {
      archBlocks.forEach((packed) -> {
         BlockPos pos = BlockPos.of(packed);
         BlockPos abovePos = pos.above();
         if (bounds.contains(pos) && level.ensureCanWrite(pos) && !archBlocks.contains(abovePos.asLong()) && isOpenCrownSpace(level.getBlockState(abovePos)) && level.getBlockState(pos).is(ModBlocks.SIFTSLATE)) {
            level.setBlock(pos, ModBlocks.SIFTSLATE_GROWTH.defaultBlockState(), 2);
         }
      });
   }

   private static boolean isOpenCrownSpace(BlockState state) {
      return state.isAir() || state.is(ModBlocks.OVERGROWN_CHARD) || state.is(ModBlocks.OVERGROWN_STALKS) || state.is(ModBlocks.OVERGROWN_FRONDS) || state.is(ModBlocks.SIFTSLATE_STALKS) || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS) || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS);
   }

   private static Candidate findBestSpan(WorldGenLevel level, SiftWorldgenBounds bounds, int centerX, int centerZ, int centerY, int halfSpan, RandomSource random) {
      Candidate best = null;
      double bestScore = -Double.MAX_VALUE;

      for (int[] direction : AXES) {
         double normalization = direction[0] != 0 && direction[1] != 0 ? 0.70710678118 : (double)1.0F;
         double axisX = (double)direction[0] * normalization;
         double axisZ = (double)direction[1] * normalization;
         int leftX = centerX - (int)Math.round(axisX * (double)halfSpan);
         int leftZ = centerZ - (int)Math.round(axisZ * (double)halfSpan);
         int rightX = centerX + (int)Math.round(axisX * (double)halfSpan);
         int rightZ = centerZ + (int)Math.round(axisZ * (double)halfSpan);
         if (bounds.contains(leftX, leftZ) && bounds.contains(rightX, rightZ)) {
            int leftY = SiftMonolithFeature.findTerrainSurface(level, leftX, leftZ);
            int rightY = SiftMonolithFeature.findTerrainSurface(level, rightX, rightZ);
            if (leftY >= 0 && rightY >= 0) {
               int innerDistance = Math.max(7, halfSpan / 2);
               int leftInnerX = centerX - (int)Math.round(axisX * (double)(halfSpan - innerDistance));
               int leftInnerZ = centerZ - (int)Math.round(axisZ * (double)(halfSpan - innerDistance));
               int rightInnerX = centerX + (int)Math.round(axisX * (double)(halfSpan - innerDistance));
               int rightInnerZ = centerZ + (int)Math.round(axisZ * (double)(halfSpan - innerDistance));
               if (bounds.contains(leftInnerX, leftInnerZ) && bounds.contains(rightInnerX, rightInnerZ)) {
                  int leftInnerY = SiftMonolithFeature.findTerrainSurface(level, leftInnerX, leftInnerZ);
                  int rightInnerY = SiftMonolithFeature.findTerrainSurface(level, rightInnerX, rightInnerZ);
                  if (leftInnerY >= 0 && rightInnerY >= 0) {
                     int heightDifference = Math.abs(leftY - rightY);
                     int valleyDepth = Math.min(leftY, rightY) - centerY;
                     int leftDrop = leftY - leftInnerY;
                     int rightDrop = rightY - rightInnerY;
                     if (heightDifference <= 20 && valleyDepth >= 6 && leftDrop >= 3 && rightDrop >= 3 && leftDrop + rightDrop >= 8) {
                        double score = (double)valleyDepth * (double)5.5F + (double)(leftDrop + rightDrop) * 2.2 - (double)heightDifference * (double)0.75F + random.nextDouble();
                        if (score > bestScore) {
                           bestScore = score;
                           best = new Candidate(axisX, axisZ, halfSpan, leftY, rightY, valleyDepth);
                        }
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private static CurvePoint curvePoint(Candidate candidate, int centerX, int centerZ, double t, int archRise, long salt) {
      double signedDistance = (t * (double)2.0F - (double)1.0F) * (double)candidate.halfSpan;
      double archMask = Math.sin(Math.PI * t);
      double lateralWave = (Math.sin(t * Math.PI * (double)2.0F + phase(salt, 7)) * 0.68 + Math.sin(t * Math.PI * (double)5.0F + phase(salt, 23)) * 0.32) * 1.45 * archMask;
      double verticalWave = (Math.sin(t * Math.PI * (double)3.0F + phase(salt, 17)) * 0.72 + Math.sin(t * Math.PI * (double)7.0F + phase(salt, 37)) * 0.28) * 1.05 * archMask;
      double perpendicularX = -candidate.axisZ;
      double perpendicularZ = candidate.axisX;
      double baseY = (double)candidate.leftY + (double)(candidate.rightY - candidate.leftY) * t;
      return new CurvePoint((double)centerX + candidate.axisX * signedDistance + perpendicularX * lateralWave, baseY + (double)archRise * (double)4.0F * t * ((double)1.0F - t) + verticalWave, (double)centerZ + candidate.axisZ * signedDistance + perpendicularZ * lateralWave);
   }

   private static int fillOrganicSphere(WorldGenLevel level, SiftWorldgenBounds bounds, CurvePoint center, double radius, BlockState mainState, boolean healthy, int stripeSeed, long salt, SiftPackedLongSet placedBlocks) {
      int placed = 0;
      int limit = (int)Math.ceil(radius + (double)0.5F);
      int roundedX = (int)Math.round(center.x);
      int roundedY = (int)Math.round(center.y);
      int roundedZ = (int)Math.round(center.z);
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (int dx = -limit; dx <= limit; ++dx) {
         for (int dy = -limit; dy <= limit; ++dy) {
            for (int dz = -limit; dz <= limit; ++dz) {
               double roughness = (hash01(roundedX + dx, roundedY + dy, roundedZ + dz, salt) - (double)0.5F) * 0.62;
               double edgeRadius = radius + roughness;
               if (!((double)(dx * dx + dy * dy + dz * dz) > edgeRadius * edgeRadius)) {
                  cursor.set(roundedX + dx, roundedY + dy, roundedZ + dz);
                  if (bounds.contains(cursor) && level.ensureCanWrite(cursor) && SiftMonolithFeature.canReplace(level.getBlockState(cursor))) {
                     boolean dryVein = healthy && dy < 0 && stripeSeed / 8 % 7 == 3 && hash01(cursor.getX(), cursor.getY(), cursor.getZ(), salt ^ -7046029254386353131L) < 0.58;
                     level.setBlock(cursor, dryVein ? ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState() : mainState, 2);
                     placedBlocks.add(cursor.asLong());
                     ++placed;
                  }
               }
            }
         }
      }

      return placed;
   }

   private static void carveGouges(WorldGenLevel level, RandomSource random, Candidate candidate, int centerX, int centerZ, int archRise, double thickness, long salt, SiftPackedLongSet placedBlocks) {
      int gouges = 3 + random.nextInt(5);
      double perpendicularX = -candidate.axisZ;
      double perpendicularZ = candidate.axisX;

      for (int i = 0; i < gouges; ++i) {
         double t = 0.16 + random.nextDouble() * 0.68;
         CurvePoint curve = curvePoint(candidate, centerX, centerZ, t, archRise, salt);
         double angle = -1.0995574287564276 + random.nextDouble() * Math.PI * 1.7;
         double surfaceOffset = thickness * (0.68 + random.nextDouble() * 0.22);
         double gougeX = curve.x + perpendicularX * Math.cos(angle) * surfaceOffset;
         double gougeY = curve.y + Math.sin(angle) * surfaceOffset;
         double gougeZ = curve.z + perpendicularZ * Math.cos(angle) * surfaceOffset;
         double radius = 0.82 + random.nextDouble() * 0.78;
         int limit = (int)Math.ceil(radius);
         int roundedX = (int)Math.round(gougeX);
         int roundedY = (int)Math.round(gougeY);
         int roundedZ = (int)Math.round(gougeZ);
         BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

         for (int dx = -limit; dx <= limit; ++dx) {
            for (int dy = -limit; dy <= limit; ++dy) {
               for (int dz = -limit; dz <= limit; ++dz) {
                  if (!((double)(dx * dx + dy * dy + dz * dz) > radius * radius)) {
                     cursor.set(roundedX + dx, roundedY + dy, roundedZ + dz);
                     if (placedBlocks.remove(cursor.asLong()) && level.ensureCanWrite(cursor)) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                     }
                  }
               }
            }
         }
      }

   }

   private static boolean isHealthy(BlockState state) {
      return state.is(ModBlocks.HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK);
   }

   private static double phase(long salt, int shift) {
      return (double)(salt >>> shift & 65535L) / (double)65535.0F * Math.PI * (double)2.0F;
   }

   private static double hash01(int x, int y, int z, long salt) {
      long value = salt ^ (long)x * -7046029254386353131L;
      value ^= (long)y * -4417276706812531889L;
      value ^= (long)z * 1609587929392839161L;
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      value *= -7723592293110705685L;
      value ^= value >>> 31;
      return (double)(value >>> 11) * (double)1.110223E-16F;
   }

   private static double smootherstep(double value) {
      double t = Math.max((double)0.0F, Math.min((double)1.0F, value));
      return t * t * t * (t * (t * (double)6.0F - (double)15.0F) + (double)10.0F);
   }

   private static record CurvePoint(double x, double y, double z) {
   }

   private static record Candidate(double axisX, double axisZ, int halfSpan, int leftY, int rightY, int valleyDepth) {
      private BlockPos leftFloor(int centerX, int centerZ) {
         return new BlockPos(centerX - (int)Math.round(this.axisX * (double)this.halfSpan), this.leftY - 1, centerZ - (int)Math.round(this.axisZ * (double)this.halfSpan));
      }

      private BlockPos rightFloor(int centerX, int centerZ) {
         return new BlockPos(centerX + (int)Math.round(this.axisX * (double)this.halfSpan), this.rightY - 1, centerZ + (int)Math.round(this.axisZ * (double)this.halfSpan));
      }
   }
}
