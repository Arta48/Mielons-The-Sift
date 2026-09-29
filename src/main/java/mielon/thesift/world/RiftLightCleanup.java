package mielon.thesift.world;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

public final class RiftLightCleanup {
   private static final Set pending = new HashSet();

   public static void enqueue(ServerLevel level, Collection positions) {
      for (BlockPos pos : (Iterable<BlockPos>) positions) {
         pending.add(new Entry(level.dimension(), pos.immutable()));
      }

   }

   public static void tick(MinecraftServer server) {
      Iterator<Entry> it = pending.iterator();

      while(it.hasNext()) {
         Entry e = (Entry)it.next();
         ServerLevel level = server.getLevel(e.dimension);
         if (level == null) {
            it.remove();
         } else {
            LevelChunk chunk = level.getChunkSource().getChunkNow(e.pos.getX() >> 4, e.pos.getZ() >> 4);
            if (chunk != null) {
               if (chunk.getBlockState(e.pos).is(Blocks.LIGHT)) {
                  level.setBlock(e.pos, Blocks.AIR.defaultBlockState(), 2);
               }

               it.remove();
            }
         }
      }

   }

   public static void clear() {
      pending.clear();
   }

   private RiftLightCleanup() {
   }

   private static record Entry(ResourceKey dimension, BlockPos pos) {
   }
}
