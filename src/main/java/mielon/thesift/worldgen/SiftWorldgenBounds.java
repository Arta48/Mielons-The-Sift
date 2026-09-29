package mielon.thesift.worldgen;

import net.minecraft.core.BlockPos;

final class SiftWorldgenBounds {
   private final int minX;
   private final int maxX;
   private final int minZ;
   private final int maxZ;

   private SiftWorldgenBounds(int chunkX, int chunkZ, int horizontalMargin) {
      this.minX = (chunkX - 1 << 4) + horizontalMargin;
      this.maxX = (chunkX + 2 << 4) - 1 - horizontalMargin;
      this.minZ = (chunkZ - 1 << 4) + horizontalMargin;
      this.maxZ = (chunkZ + 2 << 4) - 1 - horizontalMargin;
   }

   static SiftWorldgenBounds around(BlockPos generatingOrigin) {
      return new SiftWorldgenBounds(Math.floorDiv(generatingOrigin.getX(), 16), Math.floorDiv(generatingOrigin.getZ(), 16), 0);
   }

   static SiftWorldgenBounds aroundWithNeighbourMargin(BlockPos generatingOrigin) {
      return new SiftWorldgenBounds(Math.floorDiv(generatingOrigin.getX(), 16), Math.floorDiv(generatingOrigin.getZ(), 16), 1);
   }

   boolean contains(BlockPos pos) {
      return this.contains(pos.getX(), pos.getZ());
   }

   boolean contains(int x, int z) {
      return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
   }
}
