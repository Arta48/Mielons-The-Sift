package mielon.thesift.portal;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public final class PortalAutoActivation {
   private static final int SEARCH_RADIUS = 10;

   private PortalAutoActivation() {
   }

   public static void tryActivateNearNotes(ServerLevel level, List sequence) {
       if (!sequence.isEmpty()) {
           Set<BlockPos> checked = new HashSet<>();
           BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

           for (SonorousConsoles.PlayedNote note : (Iterable<SonorousConsoles.PlayedNote>) sequence) {
               BlockPos notePos = note.notePos();

               for (int dx = -10; dx <= 10; ++dx) {
                   for (int dy = -10; dy <= 10; ++dy) {
                       for (int dz = -10; dz <= 10; ++dz) {
                           cursor.set(notePos.getX() + dx, notePos.getY() + dy, notePos.getZ() + dz);
                           BlockPos candidate = cursor.immutable();
                           if (checked.add(candidate) && level.getBlockState(candidate).is(Blocks.REINFORCED_DEEPSLATE) && PortalIgnition.tryIgnite(level, candidate)) {
                               return;
                           }
                       }
                   }
               }
           }

       }
   }

   public static void tryCloseNearNotes(ServerLevel level, List sequence) {
       if (!sequence.isEmpty()) {
           Set<BlockPos> checked = new HashSet<>();
           BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

           for (SonorousConsoles.PlayedNote note : (Iterable<SonorousConsoles.PlayedNote>) sequence) {
               BlockPos notePos = note.notePos();

               for (int dx = -10; dx <= 10; ++dx) {
                   for (int dy = -10; dy <= 10; ++dy) {
                       for (int dz = -10; dz <= 10; ++dz) {
                           cursor.set(notePos.getX() + dx, notePos.getY() + dy, notePos.getZ() + dz);
                           BlockPos candidate = cursor.immutable();
                           if (checked.add(candidate) && level.getBlockState(candidate).is(Blocks.REINFORCED_DEEPSLATE)) {
                               Optional<PortalFrameScanner.Frame> frame = PortalFrameScanner.scanForClosing(level, candidate);
                               if (frame.isPresent()) {
                                   PortalGrowth.startClosing(level, frame.get().interior(), frame.get().axis());
                                   return;
                               }
                           }
                       }
                   }
               }
           }

       }
   }
}
