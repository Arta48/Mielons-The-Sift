package mielon.thesift.portal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SonorousDeepslateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class SonorousConsoles {
   public static final int GROUP_SEARCH_RADIUS = 16;
   public static final int[] TARGET_SEQUENCE = new int[]{1, 3, 7, 6, 5, 2, 4, 8};
   public static final int[] CLOSING_SEQUENCE;
   private static final Map BUFFERS;

   private SonorousConsoles() {
   }

   public static void clearTransientBuffers() {
      BUFFERS.clear();
   }

   public static Optional onSoundPlayed(ServerLevel level, BlockPos noteBlockPos, int soundIndex1to8) {
      List<BlockPos> group = findGroup(level, noteBlockPos);
      if (group.isEmpty()) {
         return Optional.empty();
      } else {
         ConsoleKey key = new ConsoleKey(level.dimension(), anchor(group));
         Deque<PlayedNote> buffer = (Deque)BUFFERS.computeIfAbsent(key, (k) -> new ArrayDeque(TARGET_SEQUENCE.length + 1));
         buffer.addLast(new PlayedNote(noteBlockPos.immutable(), soundIndex1to8));

         while(buffer.size() > TARGET_SEQUENCE.length) {
            buffer.removeFirst();
         }

         if (matches(buffer, TARGET_SEQUENCE)) {
            List<PlayedNote> winning = List.copyOf(buffer);
            BUFFERS.remove(key);
            return Optional.of(new MatchedSequence(winning, SonorousConsoles.SequenceType.OPENING));
         } else if (matches(buffer, CLOSING_SEQUENCE)) {
            List<PlayedNote> winning = List.copyOf(buffer);
            BUFFERS.remove(key);
            return Optional.of(new MatchedSequence(winning, SonorousConsoles.SequenceType.CLOSING));
         } else {
            return Optional.empty();
         }
      }
   }

   public static void reset(ServerLevel level, BlockPos anyPosNearGroup) {
      BlockPos anchorPos = findAnchor(level, anyPosNearGroup);
      if (anchorPos != null) {
         BUFFERS.remove(new ConsoleKey(level.dimension(), anchorPos));
      }

   }

   public static BlockPos findAnchor(ServerLevel level, BlockPos anyPosNearGroup) {
      List<BlockPos> group = findGroup(level, anyPosNearGroup);
      return group.isEmpty() ? null : anchor(group);
   }

   private static boolean matches(Deque buffer, int[] target) {
      if (buffer.size() != target.length) {
         return false;
      } else {
         int i = 0;

         for (PlayedNote played : (Iterable<PlayedNote>) buffer) {
            if (played.soundIndex1to8() != target[i]) {
               return false;
            }

            ++i;
         }

         return true;
      }
   }

   private static int[] reverse(int[] source) {
      int[] result = new int[source.length];

      for (int i = 0; i < source.length; ++i) {
         result[i] = source[source.length - 1 - i];
      }

      return result;
   }

   public static List findGroup(ServerLevel level, BlockPos origin) {
      BlockPos seed = resolveSeed(level, origin);
      if (seed == null) {
         return List.of();
      } else {
         Set<BlockPos> found = new HashSet();
         Deque<BlockPos> queue = new ArrayDeque();
         found.add(seed);
         queue.add(seed);
         BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

         while(!queue.isEmpty()) {
            BlockPos current = (BlockPos)queue.poll();

            for (int dx = -16; dx <= 16; ++dx) {
               for (int dy = -16; dy <= 16; ++dy) {
                  for (int dz = -16; dz <= 16; ++dz) {
                     cursor.set(current.getX() + dx, current.getY() + dy, current.getZ() + dz);
                     if (isNoteState(level, cursor)) {
                        BlockPos immutable = cursor.immutable();
                        if (found.add(immutable)) {
                           queue.add(immutable);
                        }
                     }
                  }
               }
            }
         }

         return new ArrayList(found);
      }
   }

   private static BlockPos resolveSeed(ServerLevel level, BlockPos origin) {
      BlockPos originImmutable = origin.immutable();
      if (isNoteState(level, originImmutable)) {
         return originImmutable;
      } else {
         BlockPos below = originImmutable.below();
         return isNoteState(level, below) ? below : null;
      }
   }

   private static boolean isNoteState(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (!state.is(ModBlocks.SONOROUS_DEEPSLATE)) {
         return false;
      } else {
         return state.getValue(SonorousDeepslateBlock.MODE) == SonorousDeepslateBlock.Mode.NOTE;
      }
   }

   private static BlockPos anchor(List group) {
      return (BlockPos) group.stream().min(Comparator.comparingInt((BlockPos p) -> p.getY()).thenComparingInt(Vec3i::getX).thenComparingInt(Vec3i::getZ)).orElseThrow();
   }

   static {
      CLOSING_SEQUENCE = reverse(TARGET_SEQUENCE);
      BUFFERS = new HashMap();
   }

   public static enum SequenceType {
      OPENING,
      CLOSING;

      // $FF: synthetic method
      private static SequenceType[] $values() {
         return new SequenceType[]{OPENING, CLOSING};
      }
   }

   public static record PlayedNote(BlockPos notePos, int soundIndex1to8) {
   }

   public static record MatchedSequence(List notes, SequenceType type) {
   }

   private static record ConsoleKey(ResourceKey dimension, BlockPos anchor) {
   }
}
