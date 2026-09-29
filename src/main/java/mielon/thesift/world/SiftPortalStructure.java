package mielon.thesift.world;

import java.util.Arrays;
import java.util.Optional;
import mielon.thesift.TheSiftMod;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.worldgen.SiftFeaturePlacementGuard;
import mielon.thesift.worldgen.SiftSurfaceDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class SiftPortalStructure {
   private static final Identifier TEMPLATE_ID = Identifier.fromNamespaceAndPath("the_sift", "main_portal");
   private static final int SIZE_X = 9;
   private static final int SIZE_Y = 19;
   private static final int SIZE_Z = 40;
   private static final BlockPos MARKER_RELATIVE = new BlockPos(3, 4, 19);
   private static final BlockPos PLAYER_RELATIVE = new BlockPos(3, 5, 19);
   private static final int STRUCTURE_GROUND_RELATIVE_Y;
   private static final int[][] GROUND_PROFILE;
   private static final int[][] CLIFF_PROFILE;
   private static final ProfilePoint[] GROUND_POINTS;
   private static final ProfilePoint[] CLIFF_POINTS;
   private static final int BLEND_RADIUS = 52;
   private static final int FOUNDATION_BLEND_RADIUS = 15;
   private static final int SEARCH_RADIUS = 96;
   private static final int COARSE_STEP = 16;
   private static final int REFINE_RADIUS = 16;
   private static final int REFINE_STEP = 4;
   private static final int OUTER_SAMPLE_MARGIN = 12;
   private static final int FEATURE_CLEAR_MARGIN = 15;
   private static final int PORTAL_BUILD_FLAGS = 48;

   private SiftPortalStructure() {
   }

   public static Optional ensurePortal(MinecraftServer server, ServerLevel sift) {
      Optional<BlockPos> stored = SiftWorldStorage.getPortalAnchor(server);
      if (stored.isPresent()) {
         BlockPos storedOrigin = ((BlockPos)stored.get()).offset(-PLAYER_RELATIVE.getX(), -PLAYER_RELATIVE.getY(), -PLAYER_RELATIVE.getZ());
         SiftFeaturePlacementGuard.reservePortal(storedOrigin, 9, 40);
         return stored;
      } else {
         Optional<StructureTemplate> templateOptional = server.getStructureTemplateManager().get(TEMPLATE_ID);
         if (templateOptional.isEmpty()) {
            TheSiftMod.LOGGER.error("Missing portal structure template: {}", TEMPLATE_ID);
            return Optional.empty();
         } else {
            TheSiftMod.LOGGER.info("Selecting a location for the main Sift portal.");
            StructureTemplate template = (StructureTemplate)templateOptional.get();
            PlacementCandidate candidate = findBestCandidate(sift);
            int naturalGroundY = candidate.groundY();
            int originY = naturalGroundY - STRUCTURE_GROUND_RELATIVE_Y;
            originY = Math.max(0, Math.min(237, originY));
            BlockPos origin = new BlockPos(candidate.anchorX() - MARKER_RELATIVE.getX(), originY, candidate.anchorZ() - MARKER_RELATIVE.getZ());
            SiftFeaturePlacementGuard.reservePortal(origin, 9, 40);
            clearLargeFeaturesAroundPortal(sift, origin);
            blendTerrainIntoPortal(sift, origin);
            sealPortalFoundation(sift, origin);
            StructurePlaceSettings settings = (new StructurePlaceSettings()).setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(true).setKnownShape(true).setRandom(RandomSource.create(sift.getSeed() ^ 1397311060L));
            boolean placed = template.placeInWorld(sift, origin, origin, settings, RandomSource.create(sift.getSeed() ^ 88301613891916L), 48);
            if (!placed) {
               TheSiftMod.LOGGER.error("Failed to place the main Sift portal at {}", origin);
               return Optional.empty();
            } else {
               BlockPos marker = origin.offset(MARKER_RELATIVE.getX(), MARKER_RELATIVE.getY(), MARKER_RELATIVE.getZ());
               setDuringPortalBuild(sift, marker, ModBlocks.REINFORCED_SIFTSLATE.defaultBlockState());
               normalizeCoveredGrowth(sift, origin);
               BlockPos playerAnchor = origin.offset(PLAYER_RELATIVE.getX(), PLAYER_RELATIVE.getY(), PLAYER_RELATIVE.getZ());
               decoratePortalAndBlend(sift, origin, playerAnchor);
               SiftWorldStorage.setPortalAnchor(server, playerAnchor);
               TheSiftMod.LOGGER.info("Main Sift portal ready at {}", playerAnchor);
               return Optional.of(playerAnchor);
            }
         }
      }
   }

   private static void normalizeCoveredGrowth(ServerLevel level, BlockPos origin) {
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();

      for (int x = 0; x < 9; ++x) {
         for (int z = 0; z < 40; ++z) {
            for (int y = 0; y < 18; ++y) {
               cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
               if (level.getBlockState(cursor).is(ModBlocks.SIFTSLATE_GROWTH)) {
                  above.set(cursor.getX(), cursor.getY() + 1, cursor.getZ());
                  if (level.getBlockState(above).isCollisionShapeFullBlock(level, above)) {
                     setDuringPortalBuild(level, cursor, ModBlocks.SIFTSLATE.defaultBlockState());
                  }
               }
            }
         }
      }

   }

   private static void clearLargeFeaturesAroundPortal(ServerLevel level, BlockPos origin) {
      int minX = origin.getX() - 15;
      int maxX = origin.getX() + 9 - 1 + 15;
      int minZ = origin.getZ() - 15;
      int maxZ = origin.getZ() + 40 - 1 + 15;
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (int x = minX; x <= maxX; ++x) {
         for (int z = minZ; z <= maxZ; ++z) {
            int generatedTop = generatedSurfaceBlockY(level, x, z);
            int actualTop = level.getHeight(Types.WORLD_SURFACE, x, z) - 1;

            for (int y = actualTop; y > generatedTop; --y) {
               cursor.set(x, y, z);
               BlockState state = level.getBlockState(cursor);
               if (!state.isAir()) {
                  if (!isSiftTerrain(state) && !isSiftPlant(state)) {
                     break;
                  }

                  setDuringPortalBuild(level, cursor, Blocks.AIR.defaultBlockState());
               }
            }
         }
      }

   }

   private static PlacementCandidate findBestCandidate(ServerLevel level) {
      PlacementCandidate coarse = searchArea(level, -96, 96, -96, 96, 16);
      if (coarse == null) {
         int height = generatedSurfaceBlockY(level, 0, 0);
         return new PlacementCandidate(0, 0, height, (double)999.0F);
      } else {
         PlacementCandidate refined = searchArea(level, coarse.anchorX() - 16, coarse.anchorX() + 16, coarse.anchorZ() - 16, coarse.anchorZ() + 16, 4);
         return refined != null ? refined : coarse;
      }
   }

   private static PlacementCandidate searchArea(ServerLevel level, int minSearchX, int maxSearchX, int minSearchZ, int maxSearchZ, int step) {
      PlacementCandidate best = null;
      double bestScore = Double.POSITIVE_INFINITY;

      for (int x = minSearchX; x <= maxSearchX; x += step) {
         for (int z = minSearchZ; z <= maxSearchZ; z += step) {
            TerrainSample sample = sampleFootprintWithoutLoadingChunks(level, x, z);
            PlacementCandidate candidate = new PlacementCandidate(x, z, sample.medianHeight(), sample.roughness());
            double candidateScore = score(candidate, x, z) + (double)sample.range() * (double)135.0F + (double)sample.outerRange() * (double)75.0F;
            if (sample.medianHeight() < 12 || sample.medianHeight() > 232) {
               candidateScore += (double)10000.0F;
            }

            if (candidateScore < bestScore) {
               bestScore = candidateScore;
               best = candidate;
            }
         }
      }

      return best;
   }

   private static double score(PlacementCandidate candidate, int x, int z) {
      double distancePenalty = Math.sqrt((double)x * (double)x + (double)z * (double)z) * 0.8;
      return candidate.roughness() * (double)18.0F + distancePenalty;
   }

   private static TerrainSample sampleFootprintWithoutLoadingChunks(ServerLevel level, int anchorX, int anchorZ) {
      int minX = anchorX - MARKER_RELATIVE.getX();
      int minZ = anchorZ - MARKER_RELATIVE.getZ();
      int maxX = minX + 9 - 1;
      int maxZ = minZ + 40 - 1;
      int midX = (minX + maxX) / 2;
      int midZ = (minZ + maxZ) / 2;
      int[] core = new int[]{generatedSurfaceBlockY(level, minX, minZ), generatedSurfaceBlockY(level, midX, minZ), generatedSurfaceBlockY(level, maxX, minZ), generatedSurfaceBlockY(level, minX, midZ), generatedSurfaceBlockY(level, midX, midZ), generatedSurfaceBlockY(level, maxX, midZ), generatedSurfaceBlockY(level, minX, maxZ), generatedSurfaceBlockY(level, midX, maxZ), generatedSurfaceBlockY(level, maxX, maxZ)};
      int outerMinX = minX - 12;
      int outerMaxX = maxX + 12;
      int outerMinZ = minZ - 12;
      int outerMaxZ = maxZ + 12;
      int outerMidX = (outerMinX + outerMaxX) / 2;
      int outerMidZ = (outerMinZ + outerMaxZ) / 2;
      int[] outer = new int[]{generatedSurfaceBlockY(level, outerMinX, outerMinZ), generatedSurfaceBlockY(level, outerMidX, outerMinZ), generatedSurfaceBlockY(level, outerMaxX, outerMinZ), generatedSurfaceBlockY(level, outerMinX, outerMidZ), generatedSurfaceBlockY(level, outerMaxX, outerMidZ), generatedSurfaceBlockY(level, outerMinX, outerMaxZ), generatedSurfaceBlockY(level, outerMidX, outerMaxZ), generatedSurfaceBlockY(level, outerMaxX, outerMaxZ)};
      int[] sorted = ((int[]) core).clone();
      Arrays.sort(sorted);
      int median = sorted[sorted.length / 2];
      int coreMin = Arrays.stream(core).min().orElse(median);
      int coreMax = Arrays.stream(core).max().orElse(median);
      int outerMin = Arrays.stream(outer).min().orElse(median);
      int outerMax = Arrays.stream(outer).max().orElse(median);
      double roughness = (double)0.0F;

      for (int height : core) {
         roughness += (double)Math.abs(height - median);
      }

      for (int height : outer) {
         roughness += (double)Math.abs(height - median) * 0.45;
      }

      roughness /= (double)core.length + (double)outer.length * 0.45;
      return new TerrainSample(median, coreMax - coreMin, Math.max(coreMax, outerMax) - Math.min(coreMin, outerMin), roughness);
   }

   private static void blendTerrainIntoPortal(ServerLevel level, BlockPos origin) {
      int structureMinX = origin.getX();
      int structureMaxX = origin.getX() + 9 - 1;
      int structureMinZ = origin.getZ();
      int structureMaxZ = origin.getZ() + 40 - 1;
      int minX = structureMinX - 52;
      int maxX = structureMaxX + 52;
      int minZ = structureMinZ - 52;
      int maxZ = structureMaxZ + 52;
      long seed = level.getSeed();
      buildNaturalPortalFoundation(level, origin, seed);

      for (int x = minX; x <= maxX; ++x) {
         for (int z = minZ; z <= maxZ; ++z) {
            int localX = x - structureMinX;
            int localZ = z - structureMinZ;
            boolean inside = localX >= 0 && localX < 9 && localZ >= 0 && localZ < 40;
            int naturalY = findTerrainSurfaceBlockY(level, x, z);
            if (naturalY > 1 && naturalY < 254) {
               BlockState originalTop = level.getBlockState(new BlockPos(x, naturalY, z));
               if (isSiftTerrain(originalTop)) {
                  if (inside) {
                     int exactRelativeY = GROUND_PROFILE[localX][localZ];
                     if (exactRelativeY >= 0) {
                        int desiredY = Math.max(2, Math.min(253, origin.getY() + exactRelativeY));
                        if (naturalY > desiredY) {
                           lowerTerrainColumn(level, x, z, naturalY, desiredY, originalTop, false);
                        }
                     }
                  } else {
                     double rectangleDistance = distanceOutsideRectangle(x, z, structureMinX, structureMaxX, structureMinZ, structureMaxZ);
                     if (!(rectangleDistance > (double)52.0F)) {
                        double envelopeRelativeY = portalTerrainEnvelope(localX, localZ, seed);
                        double approach = portalApproachMask(localX, localZ);
                        if (approach > (double)0.0F) {
                           double lowForecourt = 3.6 + terrainBlendNoise(seed ^ -3335678366873096957L, localX, localZ) * 0.55;
                           envelopeRelativeY = envelopeRelativeY * ((double)1.0F - approach) + Math.min(envelopeRelativeY, lowForecourt) * approach;
                        }

                        if (!(envelopeRelativeY <= (double)0.0F)) {
                           int desiredY = origin.getY() + (int)Math.floor(envelopeRelativeY);
                           desiredY = Math.max(2, Math.min(253, desiredY));
                           if (desiredY > naturalY) {
                              raiseTerrainColumn(level, x, z, naturalY, desiredY, originalTop, true);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private static void sealPortalFoundation(ServerLevel level, BlockPos origin) {
      int foundationTopY = Math.max(2, origin.getY() - 1);

      for (int localX = 0; localX < 9; ++localX) {
         for (int localZ = 0; localZ < 40; ++localZ) {
            if (isPortalBaseColumn(localX, localZ)) {
               int x = origin.getX() + localX;
               int z = origin.getZ() + localZ;
               int terrainY = findTerrainSurfaceBlockY(level, x, z);
               if (terrainY < 1) {
                  terrainY = Math.max(1, generatedSurfaceBlockY(level, x, z));
               }

               BlockState surface = level.getBlockState(new BlockPos(x, terrainY, z));
               if (terrainY < foundationTopY) {
                  raiseTerrainColumn(level, x, z, terrainY, foundationTopY, surface, false);
               }

               BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

               for (int y = foundationTopY; y > terrainY; --y) {
                  cursor.set(x, y, z);
                  BlockState state = level.getBlockState(cursor);
                  if (state.isAir() || isSiftPlant(state)) {
                     setDuringPortalBuild(level, cursor, ModBlocks.SIFTSLATE.defaultBlockState());
                  }
               }
            }
         }
      }

   }

   private static void buildNaturalPortalFoundation(ServerLevel level, BlockPos origin, long seed) {
      int foundationTopY = Math.max(2, origin.getY() - 1);
      int minX = origin.getX() - 15;
      int maxX = origin.getX() + 9 - 1 + 15;
      int minZ = origin.getZ() - 15;
      int maxZ = origin.getZ() + 40 - 1 + 15;

      for (int x = minX; x <= maxX; ++x) {
         for (int z = minZ; z <= maxZ; ++z) {
            int localX = x - origin.getX();
            int localZ = z - origin.getZ();
            boolean core = isPortalBaseColumn(localX, localZ);
            double broadNoise = terrainBlendNoise(seed ^ 7640891576956012809L, localX * 3 + 11, localZ * 3 - 17);
            double fineNoise = terrainBlendNoise(seed ^ -4942790177534073029L, localX * 9 - 23, localZ * 9 + 31);
            double distance = core ? (double)0.0F : distanceToPortalBase((double)localX + broadNoise * 1.45, (double)localZ - broadNoise * 1.15);
            if (!(distance > (double)15.0F)) {
               double erosion = distance * (0.7 + fineNoise * 0.08) + distance * distance * 0.016;
               int targetY = core ? foundationTopY : foundationTopY - Math.max(1, (int)Math.floor(erosion));
               int naturalY = findTerrainSurfaceBlockY(level, x, z);
               if (naturalY > 1 && naturalY < targetY) {
                  BlockState originalTop = level.getBlockState(new BlockPos(x, naturalY, z));
                  if (isSiftTerrain(originalTop)) {
                     boolean preserveBiomeSurface = !core && distance > (double)2.25F;
                     raiseTerrainColumn(level, x, z, naturalY, targetY, originalTop, preserveBiomeSurface);
                  }
               }
            }
         }
      }

   }

   private static boolean isPortalBaseColumn(int localX, int localZ) {
      return localX >= 0 && localX < 9 && localZ >= 0 && localZ < 40 && GROUND_PROFILE[localX][localZ] >= 0;
   }

   private static double distanceToPortalBase(double localX, double localZ) {
      double bestSquared = Double.POSITIVE_INFINITY;

      for (ProfilePoint point : GROUND_POINTS) {
         double dx = localX - (double)point.x();
         double dz = localZ - (double)point.z();
         bestSquared = Math.min(bestSquared, dx * dx + dz * dz);
      }

      return Math.sqrt(bestSquared);
   }

   private static double portalTerrainEnvelope(int localX, int localZ, long seed) {
      double broadWarp = terrainBlendNoise(seed ^ 1375183133L, localX * 3 + 17, localZ * 3 - 29);
      double fineWarp = terrainBlendNoise(seed ^ 2082071971L, localX * 7 - 41, localZ * 7 + 13);
      double warpedX = (double)localX + broadWarp * 1.65 + fineWarp * 0.35;
      double warpedZ = (double)localZ - broadWarp * 1.1 + fineWarp * 0.45;
      double columnNoise = terrainBlendNoise(seed ^ -6752110988234923001L, localX * 5 + 31, localZ * 5 - 47);
      double erosionPerBlock = 0.405 + columnNoise * 0.022;
      double best = Double.NEGATIVE_INFINITY;

      for (ProfilePoint point : CLIFF_POINTS) {
         double dx = (warpedX - (double)point.x()) * 0.76;
         double dz = (warpedZ - (double)point.z()) * 1.08;
         double distance = Math.sqrt(dx * dx + dz * dz);
         double candidate = (double)point.height() - distance * erosionPerBlock;
         if (candidate > best) {
            best = candidate;
         }
      }

      best += columnNoise * 0.55;
      if (!Double.isFinite(best)) {
         return (double)-1.0F;
      } else {
         return best;
      }
   }

   private static double portalApproachMask(int localX, int localZ) {
      int distanceZ;
      if (localZ < 9) {
         distanceZ = 9 - localZ;
      } else if (localZ > 30) {
         distanceZ = localZ - 30;
      } else {
         distanceZ = 0;
      }

      int distanceX;
      if (localX < 0) {
         distanceX = -localX;
      } else {
         if (localX < 9) {
            return (double)0.0F;
         }

         distanceX = localX - 8;
      }

      double zMask = (double)1.0F - smootherstep((double)distanceZ / (double)6.0F);
      double xMask = (double)1.0F - smootherstep(Math.max((double)0.0F, (double)distanceX - (double)2.0F) / (double)18.0F);
      return clamp01(zMask * xMask);
   }

   private static void lowerTerrainColumn(ServerLevel level, int x, int z, int naturalY, int desiredY, BlockState originalTop, boolean preserveSurface) {
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (int y = naturalY; y > desiredY; --y) {
         cursor.set(x, y, z);
         BlockState state = level.getBlockState(cursor);
         if (!isSiftTerrain(state)) {
            break;
         }

         setDuringPortalBuild(level, cursor, Blocks.AIR.defaultBlockState());
      }

      cursor.set(x, desiredY, z);
      BlockState exposed = level.getBlockState(cursor);
      if (!preserveSurface && isSiftTerrain(exposed)) {
         setDuringPortalBuild(level, cursor, ModBlocks.SIFTSLATE.defaultBlockState());
      } else if (exposed.is(ModBlocks.SIFTSLATE)) {
         if (originalTop.is(ModBlocks.SIFTSLATE_GROWTH)) {
            setDuringPortalBuild(level, cursor, ModBlocks.SIFTSLATE_GROWTH.defaultBlockState());
         } else if (originalTop.is(ModBlocks.HEALTHY_SCULK)) {
            setDuringPortalBuild(level, cursor, ModBlocks.HEALTHY_SCULK.defaultBlockState());
         }
      }

   }

   private static void raiseTerrainColumn(ServerLevel level, int x, int z, int naturalY, int desiredY, BlockState originalTop, boolean preserveSurface) {
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (int y = naturalY + 1; y <= desiredY; ++y) {
         cursor.set(x, y, z);
         setDuringPortalBuild(level, cursor, ModBlocks.SIFTSLATE.defaultBlockState());
      }

      cursor.set(x, desiredY, z);
      if (!preserveSurface) {
         setDuringPortalBuild(level, cursor, ModBlocks.SIFTSLATE.defaultBlockState());
      } else if (originalTop.is(ModBlocks.SIFTSLATE_GROWTH)) {
         setDuringPortalBuild(level, cursor, ModBlocks.SIFTSLATE_GROWTH.defaultBlockState());
      } else if (originalTop.is(ModBlocks.HEALTHY_SCULK)) {
         setDuringPortalBuild(level, cursor, ModBlocks.HEALTHY_SCULK.defaultBlockState());
      }

   }

   private static void decoratePortalAndBlend(ServerLevel level, BlockPos origin, BlockPos playerAnchor) {
      RandomSource random = RandomSource.create(level.getSeed() ^ 22601841507586892L);
      int structureMinX = origin.getX();
      int structureMaxX = origin.getX() + 9 - 1;
      int structureMinZ = origin.getZ();
      int structureMaxZ = origin.getZ() + 40 - 1;

      for (int x = structureMinX; x <= structureMaxX; ++x) {
         for (int z = structureMinZ; z <= structureMaxZ; ++z) {
            for (int y = origin.getY(); y < origin.getY() + 19; ++y) {
               BlockPos floor = new BlockPos(x, y, z);
               if (!isPortalApproach(floor, playerAnchor)) {
                  SiftSurfaceDecorator.decorateStructureSurface(level, random, floor, 0.92F, 48);
               }
            }
         }
      }

   }

   private static boolean setDuringPortalBuild(ServerLevel level, BlockPos pos, BlockState state) {
      return level.getBlockState(pos) == state ? false : level.setBlock(pos, state, 48);
   }

   private static ProfilePoint[] collectProfilePoints(int[][] profile) {
      ProfilePoint[] temporary = new ProfilePoint[360];
      int count = 0;

      for (int x = 0; x < 9; ++x) {
         for (int z = 0; z < 40; ++z) {
            int height = profile[x][z];
            if (height >= 0) {
               temporary[count++] = new ProfilePoint(x, z, height);
            }
         }
      }

      return (ProfilePoint[])Arrays.copyOf(temporary, count);
   }

   private static boolean isPortalApproach(BlockPos pos, BlockPos playerAnchor) {
      int dx = Math.abs(pos.getX() - playerAnchor.getX());
      int dz = Math.abs(pos.getZ() - playerAnchor.getZ());
      return dx <= 4 && dz <= 5;
   }

   private static boolean isSiftTerrain(BlockState state) {
      return state.is(ModBlocks.SIFTSLATE) || state.is(ModBlocks.SIFTSLATE_GROWTH) || state.is(ModBlocks.HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK);
   }

   private static boolean isSiftPlant(BlockState state) {
      return state.is(ModBlocks.OVERGROWN_CHARD) || state.is(ModBlocks.OVERGROWN_STALKS) || state.is(ModBlocks.OVERGROWN_FRONDS) || state.is(ModBlocks.SIFTSLATE_STALKS) || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS) || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS) || state.is(ModBlocks.OVERGROWN_WILLOW_FOLIAGE) || state.is(ModBlocks.OVERGROWN_WILLOW_VINES) || state.is(ModBlocks.OVERGROWN_WILLOW_VINES_PLANT) || state.is(ModBlocks.OVERGROWN_WILLOW_LOG) || state.is(ModBlocks.OVERGROWN_WILLOW_WOOD);
   }

   private static int findTerrainSurfaceBlockY(ServerLevel level, int x, int z) {
      int top = level.getHeight(Types.WORLD_SURFACE, x, z) - 1;
      int minimum = Math.max(level.getMinY() + 1, top - 48);
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (int y = top; y >= minimum; --y) {
         cursor.set(x, y, z);
         if (isSiftTerrain(level.getBlockState(cursor))) {
            return y;
         }
      }

      return -1;
   }

   private static double distanceOutsideRectangle(int x, int z, int minX, int maxX, int minZ, int maxZ) {
      int dx = x < minX ? minX - x : Math.max(0, x - maxX);
      int dz = z < minZ ? minZ - z : Math.max(0, z - maxZ);
      return Math.sqrt((double)dx * (double)dx + (double)dz * (double)dz);
   }

   private static double terrainBlendNoise(long seed, int x, int z) {
      double phase = (double)(seed & 65535L) * 1.73E-4;
      double a = Math.sin((double)x * 0.083 + (double)z * 0.031 + phase);
      double b = Math.cos((double)x * 0.037 - (double)z * 0.071 - phase * 0.73);
      double c = Math.sin((double)(x + z) * 0.021 + phase * 1.91);
      return a * (double)0.5F + b * 0.33 + c * 0.17;
   }

   private static double smootherstep(double value) {
      double t = clamp01(value);
      return t * t * t * (t * (t * (double)6.0F - (double)15.0F) + (double)10.0F);
   }

   private static double clamp01(double value) {
      return Math.max((double)0.0F, Math.min((double)1.0F, value));
   }

   private static int generatedSurfaceBlockY(ServerLevel level, int x, int z) {
      ServerChunkCache chunkSource = level.getChunkSource();
      int firstFreeY = chunkSource.getGenerator().getBaseHeight(x, z, Types.WORLD_SURFACE_WG, level, chunkSource.randomState());
      return firstFreeY - 1;
   }

   static {
      STRUCTURE_GROUND_RELATIVE_Y = MARKER_RELATIVE.getY() - 4;
      GROUND_PROFILE = new int[][]{{-1, -1, -1, -1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1}, {-1, -1, -1, -1, 1, 7, 14, 1, 6, 4, 3, 3, 3, 2, -1, -1, 3, 2, 2, 3, 3, 2, 2, 3, 3, 2, 2, 3, 3, 4, -1, 1, 12, 16, 14, 2, -1, -1, -1, -1}, {-1, -1, -1, -1, 0, 17, 17, 12, 18, 6, 5, 4, 3, 3, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 6, 7, 17, 17, 17, 14, 14, -1, -1, -1, -1}, {-1, -1, -1, -1, 0, 17, 18, 18, 18, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 16, 17, 17, 15, 14, -1, -1, -1, -1}, {-1, -1, -1, -1, 0, 17, 17, 18, 18, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 5, 5, 6, 16, 17, 17, 15, 15, -1, -1, -1, -1}, {-1, -1, -1, -1, 0, 17, 17, 2, 6, 4, 3, 4, 3, 2, 2, 3, 3, 2, 2, 3, 3, 2, 2, 3, 3, 2, 2, 3, 3, 2, 4, 6, 17, 16, 15, 15, -1, -1, -1, -1}, {-1, -1, -1, -1, 0, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 1, 1, 1, 2, 1, 0, 2, 2, 2, 3, 3, 5, 5, 5, 4, -1, -1, -1, -1}, {-1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, -1, 1, 2, 2, -1, -1, -1, -1, -1, -1, -1, -1}, {-1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}};
      CLIFF_PROFILE = new int[][]{{-1, -1, -1, -1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1}, {-1, -1, -1, -1, 15, 17, 17, 17, 17, 17, 17, 17, 17, 16, 16, 16, 17, 16, 16, 17, 17, 17, 17, 17, 17, 17, 16, 16, 17, 17, 17, 17, 17, 16, 14, 14, -1, -1, -1, -1}, {9, 10, 10, 9, 16, 17, 17, 17, 18, 18, 17, 17, 17, 16, 16, 16, 17, 16, 16, 17, 17, 17, 18, 17, 17, 17, 17, 17, 17, 17, 16, 17, 17, 17, 14, 14, 12, 12, 12, 11}, {10, 10, 10, 10, 17, 17, 18, 18, 18, 18, 18, 17, 17, 16, 17, 16, 17, 17, 16, 17, 17, 18, 18, 18, 18, 18, 17, 17, 17, 16, 16, 16, 17, 17, 15, 14, 12, 12, 12, 12}, {10, 10, 10, 10, 17, 17, 17, 18, 18, 18, 17, 17, 17, 16, 16, 16, 17, 17, 17, 17, 17, 18, 17, 17, 18, 18, 17, 17, 17, 17, 17, 16, 17, 17, 15, 15, 12, 12, 12, 12}, {-1, -1, -1, -1, 17, 17, 17, 17, 17, 17, 17, 17, 16, 16, 16, 17, 17, 16, 16, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 16, 15, 15, 3, -1, -1, -1}, {-1, -1, -1, -1, 0, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 1, 1, 1, 2, 1, 0, 2, 2, 2, 3, 3, 5, 5, 5, 4, -1, -1, -1, -1}, {-1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, -1, 1, 2, 2, -1, -1, -1, -1, -1, -1, -1, -1}, {-1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}};
      GROUND_POINTS = collectProfilePoints(GROUND_PROFILE);
      CLIFF_POINTS = collectProfilePoints(CLIFF_PROFILE);
   }

   private static record TerrainSample(int medianHeight, int range, int outerRange, double roughness) {
   }

   private static record PlacementCandidate(int anchorX, int anchorZ, int groundY, double roughness) {
   }

   private static record ProfilePoint(int x, int z, int height) {
   }
}
