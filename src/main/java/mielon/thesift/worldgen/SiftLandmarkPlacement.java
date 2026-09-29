package mielon.thesift.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class SiftLandmarkPlacement {
   public static final int ABANDONED_PORTAL_CELL_CHUNKS = 32;
   public static final int SNIFFER_CAVE_OVERGROWN_CELL_CHUNKS = 16;
   public static final int SNIFFER_CAVE_WASTES_CELL_CHUNKS = 28;
   public static final int SNIFFER_CAVE_OVERGROWN_CANDIDATES = 4;
   public static final int SNIFFER_CAVE_WASTES_CANDIDATES = 3;
   public static final int SOUL_CANYON_CANDIDATES = 1;
   public static final int SOUL_CANYON_CELL_CHUNKS = 12;
   public static final long ABANDONED_PORTAL_SALT = 4702392765337521733L;
   public static final long SNIFFER_CAVE_OVERGROWN_SALT = 6002815919507592773L;
   public static final long SNIFFER_CAVE_WASTES_SALT = 6002815919508111699L;
   public static final long SOUL_CANYON_SALT = 6003110614342389337L;

   private SiftLandmarkPlacement() {
   }

   public static boolean isSelectedChunk(long worldSeed, BlockPos origin, int cellChunks, long salt) {
      return isSelectedChunk(worldSeed, origin, cellChunks, salt, 1);
   }

   public static boolean isSelectedChunk(long worldSeed, BlockPos origin, int cellChunks, long salt, int candidateCount) {
      int chunkX = Math.floorDiv(origin.getX(), 16);
      int chunkZ = Math.floorDiv(origin.getZ(), 16);
      int cellX = Math.floorDiv(chunkX, cellChunks);
      int cellZ = Math.floorDiv(chunkZ, cellChunks);

      for (int index = 0; index < candidateCount; ++index) {
         ChunkPos selected = candidateChunk(worldSeed, cellX, cellZ, cellChunks, salt, index);
         if (selected.x() == chunkX && selected.z() == chunkZ) {
            return true;
         }
      }

      return false;
   }

   public static ChunkPos candidateChunk(long worldSeed, int cellX, int cellZ, int cellChunks, long salt) {
      return candidateChunk(worldSeed, cellX, cellZ, cellChunks, salt, 0);
   }

   public static ChunkPos candidateChunk(long worldSeed, int cellX, int cellZ, int cellChunks, long salt, int candidateIndex) {
      long indexedSalt = salt ^ (long)candidateIndex * -2960836687051489901L;
      long first = mix(worldSeed, cellX, cellZ, indexedSalt);
      long second = mix(worldSeed, cellZ, cellX, indexedSalt ^ -7046029254386353131L);
      int offsetX = (int)Long.remainderUnsigned(first, (long)cellChunks);
      int offsetZ = (int)Long.remainderUnsigned(second, (long)cellChunks);
      return new ChunkPos(cellX * cellChunks + offsetX, cellZ * cellChunks + offsetZ);
   }

   private static long mix(long seed, int cellX, int cellZ, long salt) {
      long value = seed ^ salt;
      value ^= (long)cellX * -7046029254386353131L;
      value ^= (long)cellZ * -4417276706812531889L;
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      value *= -7723592293110705685L;
      return value ^ value >>> 31;
   }
}
