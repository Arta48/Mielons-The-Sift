package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftSurfaceSculkRegionFeature implements Feature {
   public static final MapCodec CODEC = MapCodec.unit(SiftSurfaceSculkRegionFeature::new);
   private static final int CELL_SIZE = 512;
   private static final int MIN_DIAMETER = 50;
   private static final int MAX_DIAMETER = 120;
   private static final int MAX_RADIUS = 60;
   private static final int CENTER_MARGIN = 76;
   private static final int MIN_SCULK_DEPTH = 6;
   private static final int SCULK_DEPTH_VARIATION = 4;
   private static final long REGION_SALT = 5999732914757190983L;
   private static final long DARK_SNIFFER_SALT = 4918302751538956614L;

   public MapCodec codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      int chunkMinX = origin.getX() & -16;
      int chunkMinZ = origin.getZ() & -16;
      int chunkMaxX = chunkMinX + 15;
      int chunkMaxZ = chunkMinZ + 15;
      int minCellX = Math.floorDiv(chunkMinX - 60, 512);
      int maxCellX = Math.floorDiv(chunkMaxX + 60, 512);
      int minCellZ = Math.floorDiv(chunkMinZ - 60, 512);
      int maxCellZ = Math.floorDiv(chunkMaxZ + 60, 512);
      boolean changed = false;

      for (int cellX = minCellX; cellX <= maxCellX; ++cellX) {
         for (int cellZ = minCellZ; cellZ <= maxCellZ; ++cellZ) {
            SculkRegion region = createRegion(level.getSeed(), cellX, cellZ);
            if (region.intersects(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ)) {
               List<DarkSpawnPlan> darkSpawnPlans = new ArrayList();
               int darkSnifferCount = 1 + (int)(mix(region.seed() ^ 4918302751538956614L) & 1L);
               int firstDarkBlob = Math.floorMod(mix(region.seed() ^ 4918302751538956614L ^ 1848679592227504671L), region.blobs().length);
               int secondDarkBlob = Math.floorMod(mix(region.seed() ^ 4918302751538956614L ^ 8312979105109307661L), region.blobs().length - 1);
               if (secondDarkBlob >= firstDarkBlob) {
                  ++secondDarkBlob;
               }

               for (int blobIndex = 0; blobIndex < region.blobs().length; ++blobIndex) {
                  SculkBlob blob = region.blobs()[blobIndex];
                  boolean ownerChunk = Math.floorDiv((int)Math.floor(blob.centerX()), 16) == Math.floorDiv(chunkMinX, 16) && Math.floorDiv((int)Math.floor(blob.centerZ()), 16) == Math.floorDiv(chunkMinZ, 16);
                  boolean selected = blobIndex == firstDarkBlob || darkSnifferCount == 2 && blobIndex == secondDarkBlob;
                  if (ownerChunk && selected) {
                     darkSpawnPlans.add(new DarkSpawnPlan(blob, new ArrayList(32)));
                  }
               }

               for (int x = chunkMinX; x <= chunkMaxX; ++x) {
                  for (int z = chunkMinZ; z <= chunkMaxZ; ++z) {
                     boolean core = region.contains(x, z);
                     boolean fringe = !core && region.isVeinFringe(x, z);
                     if (core || fringe) {
                        int surfaceY = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                        if (surfaceY >= level.getMinY() && surfaceY < level.getMaxY() - 2) {
                           BlockPos surfacePos = new BlockPos(x, surfaceY, z);
                           BlockState surfaceState = level.getBlockState(surfacePos);
                           if (isNaturalSiftTerrain(surfaceState)) {
                              BlockPos abovePos = surfacePos.above();
                              BlockState aboveState = level.getBlockState(abovePos);
                              if (isReplaceableSiftPlant(aboveState)) {
                                 clearUpperSnifferHalf(level, abovePos, aboveState);
                                 level.setBlock(abovePos, Blocks.AIR.defaultBlockState(), 3);
                                 aboveState = Blocks.AIR.defaultBlockState();
                              }

                              if (core) {
                                 boolean replaced = replaceSurface(level, surfacePos, region.seed(), x, z);
                                 changed |= replaced;
                                 if (aboveState.isAir()) {
                                    boolean decorated = placeSculkFamilyBlock(level, abovePos, region, x, z);
                                    changed |= decorated;
                                    if (replaced && !decorated && x > chunkMinX && x < chunkMaxX && z > chunkMinZ && z < chunkMaxZ) {
                                       for (DarkSpawnPlan plan : darkSpawnPlans) {
                                          if (plan.sites().size() < 64 && plan.blob().contains(x, z)) {
                                             plan.sites().add(surfacePos.immutable());
                                          }
                                       }
                                    }
                                 }
                              } else if (aboveState.isAir() && level.ensureCanWrite(abovePos)) {
                                 BlockState vein = (BlockState)Blocks.SCULK_VEIN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true);
                                 level.setBlock(abovePos, vein, 2);
                                 changed = true;
                              }
                           }
                        }
                     }
                  }
               }

               for (DarkSpawnPlan plan : darkSpawnPlans) {
                  if (!plan.sites().isEmpty()) {
                     SiftDeferredSnifferSpawner.enqueueDark(level, plan.sites(), plan.blob().seed());
                  }
               }
            }
         }
      }

      return changed;
   }

   private static boolean replaceSurface(WorldGenLevel level, BlockPos surfacePos, long regionSeed, int x, int z) {
      int depth = 6 + Math.floorMod(mix(regionSeed ^ positionSalt(x, z)), 4);
      boolean changed = false;

      for (int offset = 0; offset < depth; ++offset) {
         BlockPos target = surfacePos.below(offset);
         if (!level.ensureCanWrite(target) || !isNaturalSiftTerrain(level.getBlockState(target))) {
            break;
         }

         level.setBlock(target, Blocks.SCULK.defaultBlockState(), 2);
         changed = true;
      }

      return changed;
   }

   private static boolean placeSculkFamilyBlock(WorldGenLevel level, BlockPos pos, SculkRegion region, int x, int z) {
      double roll = unit(mix(region.seed() ^ positionSalt(x, z) ^ 4922508400677442629L));
      BlockState decoration;
      if (roll < 0.0015) {
         decoration = Blocks.SCULK_CATALYST.defaultBlockState();
      } else if (roll < 0.0055) {
         decoration = (BlockState)Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true);
      } else if (roll < 0.021) {
         decoration = Blocks.SCULK_SENSOR.defaultBlockState();
      } else {
         if (!(roll < 0.115) || !region.touchesBlobEdge(x, z)) {
            return false;
         }

         decoration = (BlockState)Blocks.SCULK_VEIN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true);
      }

      if (!level.ensureCanWrite(pos)) {
         return false;
      } else {
         level.setBlock(pos, decoration, 2);
         return true;
      }
   }

   private static boolean isNaturalSiftTerrain(BlockState state) {
      return state.is(ModBlocks.SIFTSLATE) || state.is(ModBlocks.SIFTSLATE_GROWTH) || state.is(ModBlocks.HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH) || state.is(Blocks.SCULK);
   }

   private static boolean isReplaceableSiftPlant(BlockState state) {
      return state.is(ModBlocks.OVERGROWN_CHARD) || state.is(ModBlocks.OVERGROWN_STALKS) || state.is(ModBlocks.OVERGROWN_FRONDS) || state.is(ModBlocks.OVERGROWN_LOTUS) || state.is(ModBlocks.SUNBURST_PLANT) || state.is(ModBlocks.WHISPERBLOOM) || state.is(ModBlocks.SIFTSLATE_STALKS) || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS) || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS) || state.is(Blocks.WILDFLOWERS) || state.is(Blocks.TORCHFLOWER) || state.is(Blocks.TORCHFLOWER_CROP) || state.is(Blocks.PITCHER_CROP) || state.is(Blocks.PITCHER_PLANT);
   }

   private static void clearUpperSnifferHalf(WorldGenLevel level, BlockPos lowerPos, BlockState lowerState) {
      if (lowerState.is(Blocks.PITCHER_CROP) || lowerState.is(Blocks.PITCHER_PLANT)) {
         BlockPos upperPos = lowerPos.above();
         BlockState upperState = level.getBlockState(upperPos);
         if ((upperState.is(Blocks.PITCHER_CROP) || upperState.is(Blocks.PITCHER_PLANT)) && level.ensureCanWrite(upperPos)) {
            level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 3);
         }

      }
   }

   private static SculkRegion createRegion(long worldSeed, int cellX, int cellZ) {
      long seed = mix(worldSeed ^ 5999732914757190983L ^ (long)cellX * -7046029254386353131L ^ (long)cellZ * -4417276706812531889L);
      RandomSource random = RandomSource.create(seed);
      int availableCenterRange = 360;
      double centerX = (double)cellX * (double)512.0F + (double)76.0F + (double)random.nextInt(availableCenterRange + 1);
      double centerZ = (double)cellZ * (double)512.0F + (double)76.0F + (double)random.nextInt(availableCenterRange + 1);
      int diameter = 50 + random.nextInt(71);
      double majorRadius = (double)diameter * (double)0.5F;
      double minorRadius = majorRadius * (0.72 + random.nextDouble() * 0.23);
      double angle = random.nextDouble() * Math.PI;
      int blobCount = 6 + random.nextInt(6);
      SculkBlob[] blobs = new SculkBlob[blobCount];

      for (int index = 0; index < blobCount; ++index) {
         double direction = random.nextDouble() * Math.PI * (double)2.0F;
         double distance = index == 0 ? (double)0.0F : Math.sqrt(random.nextDouble()) * majorRadius * 0.68;
         double localX = Math.cos(direction) * distance;
         double localZ = Math.sin(direction) * distance * (minorRadius / majorRadius);
         double cosRegion = Math.cos(angle);
         double sinRegion = Math.sin(angle);
         double blobCenterX = centerX + localX * cosRegion - localZ * sinRegion;
         double blobCenterZ = centerZ + localX * sinRegion + localZ * cosRegion;
         double blobRadius = majorRadius * (0.18 + random.nextDouble() * 0.18);
         double blobAspect = 0.62 + random.nextDouble() * 0.36;
         double blobAngle = random.nextDouble() * Math.PI;
         blobs[index] = new SculkBlob(blobCenterX, blobCenterZ, blobRadius, blobRadius * blobAspect, Math.cos(blobAngle), Math.sin(blobAngle), random.nextLong());
      }

      return new SculkRegion(centerX, centerZ, majorRadius, minorRadius, Math.cos(angle), Math.sin(angle), seed, blobs);
   }

   private static long positionSalt(int x, int z) {
      return (long)x * 7146057691288625177L ^ (long)z * -7046029288634856825L;
   }

   private static long mix(long value) {
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      value *= -7723592293110705685L;
      return value ^ value >>> 31;
   }

   private static double unit(long value) {
      return (double)(value >>> 11) * (double)1.110223E-16F;
   }

   private static record SculkRegion(double centerX, double centerZ, double majorRadius, double minorRadius, double cos, double sin, long seed, SculkBlob[] blobs) {
      boolean intersects(int minX, int minZ, int maxX, int maxZ) {
         return this.centerX + this.majorRadius >= (double)minX && this.centerX - this.majorRadius <= (double)maxX && this.centerZ + this.majorRadius >= (double)minZ && this.centerZ - this.majorRadius <= (double)maxZ;
      }

      boolean contains(int x, int z) {
         if (!this.insideEnvelope(x, z)) {
            return false;
         } else {
            for (SculkBlob blob : this.blobs) {
               if (blob.contains(x, z)) {
                  return true;
               }
            }

            return false;
         }
      }

      boolean isVeinFringe(int x, int z) {
         if (!this.insideEnvelope(x, z)) {
            return false;
         } else {
            return this.contains(x + 1, z) || this.contains(x - 1, z) || this.contains(x, z + 1) || this.contains(x, z - 1) || this.contains(x + 2, z) || this.contains(x - 2, z) || this.contains(x, z + 2) || this.contains(x, z - 2);
         }
      }

      boolean touchesBlobEdge(int x, int z) {
         return !this.contains(x + 1, z) || !this.contains(x - 1, z) || !this.contains(x, z + 1) || !this.contains(x, z - 1);
      }

      private boolean insideEnvelope(int x, int z) {
         double dx = (double)x + (double)0.5F - this.centerX;
         double dz = (double)z + (double)0.5F - this.centerZ;
         double localX = dx * this.cos + dz * this.sin;
         double localZ = -dx * this.sin + dz * this.cos;
         double ellipse = localX * localX / (this.majorRadius * this.majorRadius) + localZ * localZ / (this.minorRadius * this.minorRadius);
         return ellipse <= (double)1.0F;
      }
   }

   private static record SculkBlob(double centerX, double centerZ, double radiusX, double radiusZ, double cos, double sin, long seed) {
      boolean contains(int x, int z) {
         double dx = (double)x + (double)0.5F - this.centerX;
         double dz = (double)z + (double)0.5F - this.centerZ;
         double localX = dx * this.cos + dz * this.sin;
         double localZ = -dx * this.sin + dz * this.cos;
         double ellipse = localX * localX / (this.radiusX * this.radiusX) + localZ * localZ / (this.radiusZ * this.radiusZ);
         double edgeNoise = (SiftSurfaceSculkRegionFeature.unit(SiftSurfaceSculkRegionFeature.mix(this.seed ^ SiftSurfaceSculkRegionFeature.positionSalt(x, z))) - (double)0.5F) * 0.22;
         return ellipse <= (double)1.0F + edgeNoise;
      }
   }

   private static record DarkSpawnPlan(SculkBlob blob, List sites) {
   }
}
