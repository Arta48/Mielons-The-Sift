package mielon.thesift.worldgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class SiftSoulCanyonParticles {
   private static List clusters = List.of();
   private static int refreshCooldown;
   private static int cursor;
   private static final Map SOURCE_CACHE = new HashMap();

   private SiftSoulCanyonParticles() {
   }

   public static void tick(MinecraftServer server) {
      ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (level != null) {
         if (--refreshCooldown <= 0) {
            refreshCooldown = 400;
            clusters = SiftLandmarkTracker.snapshot(SiftLandmarkTracker.Kind.SOUL_CANYON_CLUSTER);
            if (cursor >= clusters.size()) {
               cursor = 0;
            }
         }

         if (!clusters.isEmpty()) {
            if ((level.getGameTime() & 1L) == 0L) {
               BlockPos center = (BlockPos)clusters.get(cursor++);
               if (cursor >= clusters.size()) {
                  cursor = 0;
               }

               if (level.getChunkSource().getChunkNow(Math.floorDiv(center.getX(), 16), Math.floorDiv(center.getZ(), 16)) != null) {
                  BlockPos source = findOpenSoulBlock(level, center);
                  if (source != null) {
                     level.sendParticles(ModParticles.CANYON_SOUL, (double)source.getX() - (double)1.75F + level.getRandom().nextDouble() * (double)4.5F, (double)source.getY() + 0.95, (double)source.getZ() - (double)1.75F + level.getRandom().nextDouble() * (double)4.5F, 1, 0.04, 0.01, 0.04, (double)0.0F);
                  }
               }
            }
         }
      }
   }

   private static BlockPos findOpenSoulBlock(ServerLevel level, BlockPos center) {
      long now = level.getGameTime();
      CachedSource cached = (CachedSource)SOURCE_CACHE.get(center.asLong());
      if (cached != null && cached.validUntil() > now) {
         BlockPos source = cached.source();
         if (source == null || level.getBlockState(source).is(ModBlocks.SOUL_BLOCK)) {
            return source;
         }
      }

      List<BlockPos> indexed = SiftLandmarkTracker.activeSoulBlocksNear(level, center, 10, 8);
      if (!indexed.isEmpty()) {
         int start = level.getRandom().nextInt(indexed.size());
         int maximum = Math.min(indexed.size(), 12);

         for (int offset = 0; offset < maximum; ++offset) {
            BlockPos candidate = (BlockPos)indexed.get((start + offset) % indexed.size());
            if (hasFiftyOpenBlocks(level, candidate)) {
               SOURCE_CACHE.put(center.asLong(), new CachedSource(candidate, now + 6L));
               return candidate;
            }
         }

         SOURCE_CACHE.put(center.asLong(), new CachedSource((BlockPos)null, now + 20L));
         return null;
      } else if (SiftLandmarkTracker.hasIndexedSoulPositionsNear(center, 12, 10)) {
         SOURCE_CACHE.put(center.asLong(), new CachedSource((BlockPos)null, now + 40L));
         return null;
      } else {
         BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

         for (int attempt = 0; attempt < 48; ++attempt) {
            int dx = level.getRandom().nextInt(17) - 8;
            int dz = level.getRandom().nextInt(17) - 8;
            int dy = level.getRandom().nextInt(11) - 5;
            cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
            if (level.getBlockState(cursor).is(ModBlocks.SOUL_BLOCK) && hasFiftyOpenBlocks(level, cursor)) {
               BlockPos source = cursor.immutable();
               SOURCE_CACHE.put(center.asLong(), new CachedSource(source, now + 40L));
               return source;
            }
         }

         SOURCE_CACHE.put(center.asLong(), new CachedSource((BlockPos)null, now + 20L));
         return null;
      }
   }

   private static boolean hasFiftyOpenBlocks(ServerLevel level, BlockPos source) {
      for (int above = 1; above <= 50; ++above) {
         if (!level.getBlockState(source.above(above)).isAir()) {
            return false;
         }
      }

      return true;
   }

   public static void clear() {
      clusters = List.of();
      refreshCooldown = 0;
      cursor = 0;
      SOURCE_CACHE.clear();
   }

   private static record CachedSource(BlockPos source, long validUntil) {
   }
}
