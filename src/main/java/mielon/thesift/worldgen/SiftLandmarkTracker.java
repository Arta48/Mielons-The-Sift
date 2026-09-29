package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class SiftLandmarkTracker {
   private static final int SAVE_INTERVAL_TICKS = 600;
   private static final Identifier STORAGE_ID = Identifier.fromNamespaceAndPath("the_sift", "generated_landmarks");
   private static final Map POSITIONS = new EnumMap(Kind.class);
   private static final Map POSITIONS_BY_CHUNK = new EnumMap(Kind.class);
   private static final Map LEGACY_CANYON_ACTIVITY = new ConcurrentHashMap();
   private static volatile boolean dirty;
   private static boolean loaded;
   private static int saveCooldown;

   private SiftLandmarkTracker() {
   }

   public static void record(Kind kind, BlockPos pos) {
      long packed = pos.asLong();
      if (((Set)POSITIONS.get(kind)).add(packed)) {
         index(kind, packed);
         dirty = true;
      }

   }

   public static void forget(Kind kind, BlockPos pos) {
      long packed = pos.asLong();
      if (((Set)POSITIONS.get(kind)).remove(packed)) {
         unindex(kind, packed);
         dirty = true;
      }

   }

   private static void index(Kind kind, long packed) {
      BlockPos pos = BlockPos.of(packed);
      long chunk = chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
      ((Set)((Map)POSITIONS_BY_CHUNK.get(kind)).computeIfAbsent(chunk, (ignored) -> ConcurrentHashMap.newKeySet())).add(packed);
   }

   private static void unindex(Kind kind, long packed) {
      BlockPos pos = BlockPos.of(packed);
      long chunk = chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
      Set<Long> positions = (Set)((Map)POSITIONS_BY_CHUNK.get(kind)).get(chunk);
      if (positions != null) {
         positions.remove(packed);
         if (positions.isEmpty()) {
            ((Map)POSITIONS_BY_CHUNK.get(kind)).remove(chunk, positions);
         }

      }
   }

   private static long chunkKey(int chunkX, int chunkZ) {
      return (long)chunkX << 32 ^ (long)chunkZ & 4294967295L;
   }

   public static Optional nearest(Kind kind, BlockPos origin) {
      BlockPos best = null;
      double bestDistance = Double.POSITIVE_INFINITY;

      for (long packed : (Set<Long>) POSITIONS.get(kind)) {
         BlockPos candidate = BlockPos.of(packed);
         double dx = (double)(candidate.getX() - origin.getX());
         double dz = (double)(candidate.getZ() - origin.getZ());
         double distance = dx * dx + dz * dz;
         if (distance < bestDistance) {
            bestDistance = distance;
            best = candidate;
         }
      }

      return Optional.ofNullable(best);
   }

   public static Optional nearestOther(Kind kind, BlockPos origin, double maximumDistance, BlockPos excluded) {
      BlockPos best = null;
      double bestDistance = maximumDistance * maximumDistance;
      long excludedPacked = excluded == null ? Long.MIN_VALUE : excluded.asLong();
      int originChunkX = origin.getX() >> 4;
      int originChunkZ = origin.getZ() >> 4;
      int maximumRing = ceilChunks(maximumDistance);
      Map<Long, Set<Long>> index = (Map)POSITIONS_BY_CHUNK.get(kind);

      for (int ring = 0; ring <= maximumRing; ++ring) {
         for (int chunkX = originChunkX - ring; chunkX <= originChunkX + ring; ++chunkX) {
            for (int chunkZ = originChunkZ - ring; chunkZ <= originChunkZ + ring; ++chunkZ) {
               if (ring <= 0 || Math.max(Math.abs(chunkX - originChunkX), Math.abs(chunkZ - originChunkZ)) == ring) {
                  Set<Long> positions = (Set)index.get(chunkKey(chunkX, chunkZ));
                  if (positions != null) {
                     for (long packed : positions) {
                        if (packed != excludedPacked) {
                           BlockPos candidate = BlockPos.of(packed);
                           double distance = horizontalDistanceSqr(candidate, origin);
                           if (distance < bestDistance) {
                              bestDistance = distance;
                              best = candidate;
                           }
                        }
                     }
                  }
               }
            }
         }

         if (best != null && outsideRingDistanceSqr(origin, originChunkX, originChunkZ, ring) >= bestDistance) {
            break;
         }
      }

      return Optional.ofNullable(best);
   }

   public static List snapshot(Kind kind) {
      return ((Set<Long>) POSITIONS.get(kind)).stream().map(BlockPos::of).toList();
   }

   public static Optional nearestActiveSoulCanyon(ServerLevel level, BlockPos origin, double maximumDistance) {
      return nearestActiveSoulCanyon(level, origin, maximumDistance, Set.of());
   }

   public static Optional nearestActiveSoulCanyon(ServerLevel level, BlockPos origin, double maximumDistance, Collection excluded) {
      BlockPos best = null;
      double bestDistance = maximumDistance * maximumDistance;
      int originChunkX = origin.getX() >> 4;
      int originChunkZ = origin.getZ() >> 4;
      int maximumRing = ceilChunks(maximumDistance);
      Map<Long, Set<Long>> index = (Map)POSITIONS_BY_CHUNK.get(SiftLandmarkTracker.Kind.SOUL_CANYON_CLUSTER);

      for (int ring = 0; ring <= maximumRing; ++ring) {
         for (int chunkX = originChunkX - ring; chunkX <= originChunkX + ring; ++chunkX) {
            for (int chunkZ = originChunkZ - ring; chunkZ <= originChunkZ + ring; ++chunkZ) {
               if (ring <= 0 || Math.max(Math.abs(chunkX - originChunkX), Math.abs(chunkZ - originChunkZ)) == ring) {
                  Set<Long> positions = (Set)index.get(chunkKey(chunkX, chunkZ));
                  if (positions != null) {
                     for (long packed : positions) {
                        if (!excluded.contains(packed)) {
                           BlockPos candidate = BlockPos.of(packed);
                           double distance = horizontalDistanceSqr(candidate, origin);
                           if (distance < bestDistance && hasLoadedSoulBlockNear(level, candidate)) {
                              bestDistance = distance;
                              best = candidate;
                           }
                        }
                     }
                  }
               }
            }
         }

         if (best != null && outsideRingDistanceSqr(origin, originChunkX, originChunkZ, ring) >= bestDistance) {
            break;
         }
      }

      return Optional.ofNullable(best);
   }

   public static List activeSoulBlocksNear(ServerLevel level, BlockPos center, int horizontal, int vertical) {
      double horizontalSqr = (double)horizontal * (double)horizontal;
      int minimumChunkX = center.getX() - horizontal >> 4;
      int maximumChunkX = center.getX() + horizontal >> 4;
      int minimumChunkZ = center.getZ() - horizontal >> 4;
      int maximumChunkZ = center.getZ() + horizontal >> 4;
      List<BlockPos> result = new ArrayList();
      Map<Long, Set<Long>> index = (Map)POSITIONS_BY_CHUNK.get(SiftLandmarkTracker.Kind.SOUL_CANYON_SOUL_BLOCK);

      for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; ++chunkX) {
         for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; ++chunkZ) {
            Set<Long> positions = (Set)index.get(chunkKey(chunkX, chunkZ));
            if (positions != null) {
               for (long packed : positions) {
                  BlockPos candidate = BlockPos.of(packed);
                  if (horizontalDistanceSqr(candidate, center) <= horizontalSqr && Math.abs(candidate.getY() - center.getY()) <= vertical && level.isLoaded(candidate) && level.getBlockState(candidate).is(ModBlocks.SOUL_BLOCK)) {
                     result.add(candidate);
                  }
               }
            }
         }
      }

      return result;
   }

   public static boolean hasIndexedSoulPositionsNear(BlockPos center, int horizontal, int vertical) {
      double horizontalSqr = (double)horizontal * (double)horizontal;
      int minimumChunkX = center.getX() - horizontal >> 4;
      int maximumChunkX = center.getX() + horizontal >> 4;
      int minimumChunkZ = center.getZ() - horizontal >> 4;
      int maximumChunkZ = center.getZ() + horizontal >> 4;
      Map<Long, Set<Long>> index = (Map)POSITIONS_BY_CHUNK.get(SiftLandmarkTracker.Kind.SOUL_CANYON_SOUL_BLOCK);

      for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; ++chunkX) {
         for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; ++chunkZ) {
            Set<Long> positions = (Set)index.get(chunkKey(chunkX, chunkZ));
            if (positions != null) {
               for (long packed : positions) {
                  BlockPos candidate = BlockPos.of(packed);
                  if (horizontalDistanceSqr(candidate, center) <= horizontalSqr && Math.abs(candidate.getY() - center.getY()) <= vertical) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean hasLoadedSoulBlockNear(ServerLevel level, BlockPos center) {
      List<BlockPos> recorded = activeSoulBlocksNear(level, center, 36, 28);
      if (!recorded.isEmpty()) {
         return true;
      } else if (hasIndexedSoulPositionsNear(center, 36, 28)) {
         return false;
      } else {
         long now = level.getGameTime();
         CanyonActivity cached = (CanyonActivity)LEGACY_CANYON_ACTIVITY.get(center.asLong());
         if (cached != null && cached.validUntil() > now) {
            return cached.active();
         } else {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

            for (int dy = -20; dy <= 20; ++dy) {
               for (int dx = -24; dx <= 24; ++dx) {
                  for (int dz = -24; dz <= 24; ++dz) {
                     if (dx * dx + dz * dz <= 576) {
                        cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        if (level.isLoaded(cursor) && level.getBlockState(cursor).is(ModBlocks.SOUL_BLOCK)) {
                           LEGACY_CANYON_ACTIVITY.put(center.asLong(), new CanyonActivity(true, now + 100L));
                           return true;
                        }
                     }
                  }
               }
            }

            LEGACY_CANYON_ACTIVITY.put(center.asLong(), new CanyonActivity(false, now + 1200L));
            return false;
         }
      }
   }

   private static int ceilChunks(double distance) {
      return Math.max(0, (int)Math.ceil(distance / (double)16.0F) + 1);
   }

   private static double outsideRingDistanceSqr(BlockPos origin, int originChunkX, int originChunkZ, int ring) {
      int minimumX = originChunkX - ring << 4;
      int maximumX = (originChunkX + ring << 4) + 15;
      int minimumZ = originChunkZ - ring << 4;
      int maximumZ = (originChunkZ + ring << 4) + 15;
      int distance = Math.min(Math.min(origin.getX() - (minimumX - 1), maximumX + 1 - origin.getX()), Math.min(origin.getZ() - (minimumZ - 1), maximumZ + 1 - origin.getZ()));
      return (double)distance * (double)distance;
   }

   private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
      double dx = (double)(first.getX() - second.getX());
      double dz = (double)(first.getZ() - second.getZ());
      return dx * dx + dz * dz;
   }

   public static void tick(MinecraftServer server) {
      ensureLoaded(server);
      if (dirty && --saveCooldown <= 0) {
         saveCooldown = 600;
         writeStorage(server);
      }
   }

   public static void flush(MinecraftServer server) {
      ensureLoaded(server);
      if (dirty) {
         writeStorage(server);
      }

   }

   private static void ensureLoaded(MinecraftServer server) {
      if (!loaded) {
         loaded = true;
         CompoundTag stored = server.getCommandStorage().get(STORAGE_ID);

         for (Kind kind : SiftLandmarkTracker.Kind.values()) {
            long[] positions = (long[])stored.getLongArray(kind.storageKey()).orElse(new long[0]);

            for (long position : positions) {
               if (((Set)POSITIONS.get(kind)).add(position)) {
                  index(kind, position);
               }
            }
         }

      }
   }

   private static void writeStorage(MinecraftServer server) {
      CompoundTag stored = new CompoundTag();

      for (Kind kind : SiftLandmarkTracker.Kind.values()) {
         long[] positions = ((Set<Long>) POSITIONS.get(kind)).stream().mapToLong(Long::longValue).sorted().toArray();
         stored.putLongArray(kind.storageKey(), positions);
      }

      server.getCommandStorage().set(STORAGE_ID, stored);
      dirty = false;
   }

   public static void clearTransientState() {
      for (Set positions : (Iterable<Set>) POSITIONS.values()) {
         positions.clear();
      }

      for (Map positions : (Iterable<Map>) POSITIONS_BY_CHUNK.values()) {
         positions.clear();
      }

      LEGACY_CANYON_ACTIVITY.clear();
      dirty = false;
      loaded = false;
      saveCooldown = 0;
   }

   static {
      for (Kind kind : SiftLandmarkTracker.Kind.values()) {
         POSITIONS.put(kind, ConcurrentHashMap.newKeySet());
         POSITIONS_BY_CHUNK.put(kind, new ConcurrentHashMap());
      }

   }

   public static enum Kind {
      ABANDONED_MAIN_PORTAL("abandoned_main_portals"),
      SNIFFER_CAVE("sniffer_caves"),
      SOUL_CANYON_ENTRANCE("soul_canyon_entrances"),
      SOUL_CANYON_CLUSTER("soul_canyon_clusters"),
      SOUL_CANYON_MIDPOINT("soul_canyon_midpoints"),
      SOUL_CANYON_SOUL_BLOCK("soul_canyon_soul_blocks");

      private final String storageKey;

      private Kind(String storageKey) {
         this.storageKey = storageKey;
      }

      private String storageKey() {
         return this.storageKey;
      }

      // $FF: synthetic method
      private static Kind[] $values() {
         return new Kind[]{ABANDONED_MAIN_PORTAL, SNIFFER_CAVE, SOUL_CANYON_ENTRANCE, SOUL_CANYON_CLUSTER, SOUL_CANYON_MIDPOINT, SOUL_CANYON_SOUL_BLOCK};
      }
   }

   private static record CanyonActivity(boolean active, long validUntil) {
   }
}
