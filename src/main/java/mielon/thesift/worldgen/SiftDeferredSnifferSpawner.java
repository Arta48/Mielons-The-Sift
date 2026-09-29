package mielon.thesift.worldgen;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.material.Fluid;

public final class SiftDeferredSnifferSpawner {
   private static final int MAX_SPAWNS_PER_TICK = 4;
   private static final int MAX_RETRIES = 80;
   private static final ConcurrentLinkedQueue PENDING = new ConcurrentLinkedQueue();

   private SiftDeferredSnifferSpawner() {
   }

   public static void enqueueFamily(WorldGenLevel level, RandomSource random, List nest) {
      if (!nest.isEmpty()) {
         long[] positions = new long[nest.size()];

         for (int index = 0; index < nest.size(); ++index) {
            positions[index] = ((BlockPos)nest.get(index)).asLong();
         }

         long familySeed = level.getSeed() ^ positions[0] ^ 6002815919506936396L;
         PENDING.add(new SpawnRequest(positions, false, false, mix(familySeed), 0));
         PENDING.add(new SpawnRequest(positions, false, false, mix(familySeed + 1L), 0));
         if (random.nextFloat() < 0.33F) {
            PENDING.add(new SpawnRequest(positions, true, false, mix(familySeed + 2L), 0));
         }

      }
   }

   public static void enqueueDark(WorldGenLevel level, List surfaceCandidates, long regionSeed) {
      if (!surfaceCandidates.isEmpty()) {
         long[] positions = new long[surfaceCandidates.size()];

         for (int index = 0; index < surfaceCandidates.size(); ++index) {
            positions[index] = ((BlockPos)surfaceCandidates.get(index)).asLong();
         }

         PENDING.add(new SpawnRequest(positions, false, true, mix(level.getSeed() ^ regionSeed ^ 4918302751538956614L), 0));
      }
   }

   public static void tick(MinecraftServer server) {
      ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (level != null && !PENDING.isEmpty()) {
         int availableAtTickStart = PENDING.size();
         int spawned = 0;

         for (int inspected = 0; inspected < availableAtTickStart && spawned < 4; ++inspected) {
            SpawnRequest request = (SpawnRequest)PENDING.poll();
            if (request == null) {
               break;
            }

            if (!allCandidateChunksReady(level, request.nestPositions())) {
               requeue(request);
            } else if (spawnOne(level, request)) {
               ++spawned;
            } else {
               requeue(request);
            }
         }

      }
   }

   public static void clear() {
      PENDING.clear();
   }

   private static boolean allCandidateChunksReady(ServerLevel level, long[] positions) {
      int previousChunkX = Integer.MIN_VALUE;
      int previousChunkZ = Integer.MIN_VALUE;

      for (long packed : positions) {
         BlockPos pos = BlockPos.of(packed);
         int chunkX = Math.floorDiv(pos.getX(), 16);
         int chunkZ = Math.floorDiv(pos.getZ(), 16);
         if (chunkX != previousChunkX || chunkZ != previousChunkZ) {
            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
               return false;
            }

            previousChunkX = chunkX;
            previousChunkZ = chunkZ;
         }
      }

      return true;
   }

   private static boolean spawnOne(ServerLevel level, SpawnRequest request) {
      RandomSource random = RandomSource.create(request.seed());
      long[] nest = request.nestPositions();
      int start = random.nextInt(nest.length);

      for (int offset = 0; offset < nest.length; ++offset) {
         BlockPos pos = BlockPos.of(nest[(start + offset) % nest.length]).above();
         if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()) && !isIchor(level, pos) && !isIchor(level, pos.below())) {
            EntitySpawnReason reason = request.dark() ? EntitySpawnReason.CHUNK_GENERATION : EntitySpawnReason.STRUCTURE;
            Sniffer sniffer = request.dark() ? (Sniffer)ModEntities.DARK_SNIFFER.create(level, reason) : (Sniffer)EntityTypes.SNIFFER.create(level, reason);
            if (sniffer == null) {
               return true;
            }

            sniffer.snapTo((double)pos.getX() + (double)0.5F, (double)pos.getY(), (double)pos.getZ() + (double)0.5F, random.nextFloat() * 360.0F, 0.0F);
            sniffer.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), reason, (SpawnGroupData)null);
            if (!request.dark()) {
               sniffer.setBaby(request.baby());
            } else {
               sniffer.setPersistenceRequired();
            }

            if (level.noCollision(sniffer) && level.addFreshEntity(sniffer)) {
               return true;
            }

            sniffer.discard();
         }
      }

      return false;
   }

   private static boolean isIchor(ServerLevel level, BlockPos pos) {
      Fluid fluid = level.getFluidState(pos).getType();
      return fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR;
   }

   private static void requeue(SpawnRequest request) {
      int attempts = request.attempts() + 1;
      if (attempts <= 80) {
         PENDING.add(new SpawnRequest(request.nestPositions(), request.baby(), request.dark(), request.seed(), attempts));
      }

   }

   private static long mix(long value) {
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      value *= -7723592293110705685L;
      return value ^ value >>> 31;
   }

   private static record SpawnRequest(long[] nestPositions, boolean baby, boolean dark, long seed, int attempts) {
   }
}
