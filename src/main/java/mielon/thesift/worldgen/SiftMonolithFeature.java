package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftMonolithFeature implements Feature {
   public static final MapCodec LUSH_CODEC = MapCodec.unit(() -> new SiftMonolithFeature(true));
   public static final MapCodec BARE_CODEC = MapCodec.unit(() -> new SiftMonolithFeature(false));
   private final boolean lush;

   public SiftMonolithFeature(boolean lush) {
      this.lush = lush;
   }

   public MapCodec codec() {
      return this.lush ? LUSH_CODEC : BARE_CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos placementOrigin) {
      int centerX = (placementOrigin.getX() & -16) + 8 + random.nextInt(5) - 2;
      int centerZ = (placementOrigin.getZ() & -16) + 8 + random.nextInt(5) - 2;
      int centerY = findTerrainSurface(level, centerX, centerZ);
      if (centerY < 0) {
         return false;
      } else {
         BlockPos origin = new BlockPos(centerX, centerY, centerZ);
         if (!isDrySupportedSurface(level, centerX, centerY, centerZ, 6)) {
            return false;
         } else {
            int radius = this.lush ? 5 + random.nextInt(3) : 4 + random.nextInt(3);
            if (!SiftFeaturePlacementGuard.intersectsPortal(origin, 22) && hasStableFootprint(level, origin, radius, this.lush ? 14 : 12)) {
               int requestedHeight = this.lush ? 31 + random.nextInt(25) : 24 + random.nextInt(21);
               int height = Math.min(requestedHeight, level.getMaxY() - origin.getY() - 3);
               if (height < 18) {
                  return false;
               } else if (!SiftFeaturePlacementGuard.tryReserveSolid(centerX - 22, centerZ - 22, centerX + 22, centerZ + 22)) {
                  return false;
               } else {
                  int placed = this.placePillar(level, random, origin, radius, height, this.lush, true);
                  int satellites = this.lush ? 1 + random.nextInt(3) : random.nextInt(2);

                  for (int i = 0; i < satellites; ++i) {
                     double angle = random.nextDouble() * Math.PI * (double)2.0F;
                     int distance = radius + 4 + random.nextInt(4);
                     int x = origin.getX() + (int)Math.round(Math.cos(angle) * (double)distance);
                     int z = origin.getZ() + (int)Math.round(Math.sin(angle) * (double)distance);
                     int y = findTerrainSurface(level, x, z);
                     if (y >= 0) {
                        BlockPos satelliteOrigin = new BlockPos(x, y, z);
                        int satelliteRadius = 2 + random.nextInt(2);
                        if (isDrySupportedSurface(level, x, y, z, 6) && hasStableFootprint(level, satelliteOrigin, satelliteRadius, 10)) {
                           int satelliteHeight = Math.min(13 + random.nextInt(Math.max(7, height / 2)), level.getMaxY() - y - 3);
                           if (satelliteHeight >= 10) {
                              placed += this.placePillar(level, random, satelliteOrigin, satelliteRadius, satelliteHeight, this.lush && random.nextFloat() < 0.72F, false);
                           }
                        }
                     }
                  }

                  return placed > 0;
               }
            } else {
               return false;
            }
         }
      }
   }

   private int placePillar(WorldGenLevel level, RandomSource random, BlockPos origin, int baseRadius, int height, boolean withGrowth, boolean mainPillar) {
      int placed = 0;
      int leanX = random.nextInt(5) - 2;
      int leanZ = random.nextInt(5) - 2;
      int baseY = origin.getY() - 2;
      long salt = random.nextLong();
      placed += placeRootFlare(level, origin, baseRadius, salt);
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (int dy = 0; dy <= height; ++dy) {
         double progress = (double)dy / (double)height;
         int centerX = origin.getX() + (int)Math.round((double)leanX * progress * progress);
         int centerZ = origin.getZ() + (int)Math.round((double)leanZ * progress * progress);
         double taper = (double)baseRadius - ((double)baseRadius - (double)2.25F) * progress;
         double ledge = terraceBoost(progress, mainPillar);
         double radius = taper + ledge;
         int limit = (int)Math.ceil(radius + (double)1.0F);

         for (int dx = -limit; dx <= limit; ++dx) {
            for (int dz = -limit; dz <= limit; ++dz) {
               double edgeNoise = (hash01(centerX + dx, baseY + dy, centerZ + dz, salt) - (double)0.5F) * 0.82;
               double edgeRadius = radius + edgeNoise;
               if (!((double)(dx * dx + dz * dz) > edgeRadius * edgeRadius)) {
                  cursor.set(centerX + dx, baseY + dy, centerZ + dz);
                  if (canReplace(level.getBlockState(cursor)) && level.ensureCanWrite(cursor)) {
                     level.setBlock(cursor, ModBlocks.SIFTSLATE.defaultBlockState(), 2);
                     ++placed;
                  }
               }
            }
         }
      }

      int topX = origin.getX() + leanX;
      int topZ = origin.getZ() + leanZ;
      int topY = baseY + height;
      int topRadius = Math.max(2, baseRadius - 2) + (mainPillar ? 1 : 0);
      BlockState cap = (withGrowth ? ModBlocks.SIFTSLATE_GROWTH : ModBlocks.SIFTSLATE).defaultBlockState();
      BlockPos.MutableBlockPos capPos = new BlockPos.MutableBlockPos();

      for (int dx = -topRadius; dx <= topRadius; ++dx) {
         for (int dz = -topRadius; dz <= topRadius; ++dz) {
            double edgeRadius = (double)topRadius + (hash01(topX + dx, topY, topZ + dz, salt ^ 1779033703L) - (double)0.5F) * 0.7;
            if (!((double)(dx * dx + dz * dz) > edgeRadius * edgeRadius)) {
               capPos.set(topX + dx, topY, topZ + dz);
               if (canReplace(level.getBlockState(capPos)) && level.ensureCanWrite(capPos)) {
                  level.setBlock(capPos, cap, 2);
                  ++placed;
               }

               if (withGrowth) {
                  SiftSurfaceDecorator.decorate(level, random, capPos, 1.08F);
               }
            }
         }
      }

      return placed;
   }

   private static int placeRootFlare(WorldGenLevel level, BlockPos origin, int baseRadius, long salt) {
      int placed = 0;
      int rootRadius = baseRadius + 2;
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (int dx = -rootRadius; dx <= rootRadius; ++dx) {
         for (int dz = -rootRadius; dz <= rootRadius; ++dz) {
            double irregularEdge = (hash01(origin.getX() + dx, origin.getY(), origin.getZ() + dz, salt ^ -4126379630918251389L) - (double)0.5F) * 0.9;
            double edgeRadius = (double)rootRadius + irregularEdge;
            int distanceSquared = dx * dx + dz * dz;
            if (!((double)distanceSquared > edgeRadius * edgeRadius)) {
               int x = origin.getX() + dx;
               int z = origin.getZ() + dz;
               int localSurface = findTerrainSurface(level, x, z);
               if (localSurface >= 0 && Math.abs(localSurface - origin.getY()) <= 8) {
                  double distance = Math.sqrt((double)distanceSquared);
                  double centerStrength = (double)1.0F - Math.min((double)1.0F, distance / (double)rootRadius);
                  int targetTop = origin.getY() + (int)Math.round(centerStrength * (double)2.0F);
                  if (localSurface <= targetTop + 1) {
                     for (int y = localSurface - 2; y <= targetTop; ++y) {
                        cursor.set(x, y, z);
                        if (level.ensureCanWrite(cursor) && canReplace(level.getBlockState(cursor))) {
                           level.setBlock(cursor, ModBlocks.SIFTSLATE.defaultBlockState(), 2);
                           ++placed;
                        }
                     }
                  }
               }
            }
         }
      }

      return placed;
   }

   private static boolean hasStableFootprint(WorldGenLevel level, BlockPos origin, int radius, int maximumRelief) {
      int minY = Integer.MAX_VALUE;
      int maxY = Integer.MIN_VALUE;
      int samples = 0;
      int valid = 0;
      int sampleRadius = radius + 2;

      for (int dx = -sampleRadius; dx <= sampleRadius; ++dx) {
         for (int dz = -sampleRadius; dz <= sampleRadius; ++dz) {
            if (dx * dx + dz * dz <= sampleRadius * sampleRadius) {
               ++samples;
               int y = findTerrainSurface(level, origin.getX() + dx, origin.getZ() + dz);
               if (y >= 0 && isDrySupportedSurface(level, origin.getX() + dx, y, origin.getZ() + dz, 6)) {
                  ++valid;
                  minY = Math.min(minY, y);
                  maxY = Math.max(maxY, y);
               }
            }
         }
      }

      return (double)valid >= Math.ceil((double)samples * 0.84) && maxY - minY <= maximumRelief;
   }

   private static double terraceBoost(double progress, boolean mainPillar) {
      double boost = (double)0.0F;
      if (Math.abs(progress - 0.28) < 0.035) {
         boost += mainPillar ? 1.35 : (double)0.75F;
      }

      if (Math.abs(progress - 0.58) < 0.03) {
         boost += mainPillar ? 1.05 : 0.55;
      }

      if (Math.abs(progress - 0.81) < 0.025) {
         boost += mainPillar ? (double)0.75F : 0.35;
      }

      return boost;
   }

   static boolean isSiftTerrain(BlockState state) {
      return state.is(ModBlocks.SIFTSLATE) || state.is(ModBlocks.SIFTSLATE_GROWTH) || state.is(ModBlocks.HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH);
   }

   static boolean isDrySupportedSurface(WorldGenLevel level, int x, int surfaceY, int z, int depth) {
      BlockPos surface = new BlockPos(x, surfaceY, z);
      if (!level.getFluidState(surface).isEmpty()) {
         return false;
      } else {
         for (int offset = 1; offset <= depth; ++offset) {
            if (!isSiftTerrain(level.getBlockState(surface.below(offset)))) {
               return false;
            }
         }

         return true;
      }
   }

   static boolean canReplace(BlockState state) {
      return state.isAir() || isSiftTerrain(state) || state.is(ModBlocks.OVERGROWN_CHARD) || state.is(ModBlocks.OVERGROWN_STALKS) || state.is(ModBlocks.OVERGROWN_FRONDS) || state.is(ModBlocks.SIFTSLATE_STALKS) || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS) || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS);
   }

   static int findTerrainSurface(WorldGenLevel level, int x, int z) {
      int y = level.getHeight(Types.WORLD_SURFACE_WG, x, z);
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (int step = 0; step < 8 && y > level.getMinY(); --y) {
         cursor.set(x, y - 1, z);
         if (isSiftTerrain(level.getBlockState(cursor))) {
            return y;
         }

         ++step;
      }

      return -1;
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
}
