package mielon.thesift.portal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mielon.thesift.block.SonorousNoteBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public final class SonorousIgnition {
   private static final int TICKS_BEFORE_BURST = 10;
   private static final int TICKS_AFTER_BURST_BEFORE_AUTOPLAY = 20;
   private static final List PENDING = new ArrayList();

   private SonorousIgnition() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register((ServerTickEvents.EndTick)(server) -> tick());
      ServerLifecycleEvents.SERVER_STOPPED.register((ServerLifecycleEvents.ServerStopped)(server) -> PENDING.clear());
   }

   public static void start(ServerLevel level, SonorousConsoles.MatchedSequence matched) {
      if (!matched.notes().isEmpty()) {
         PENDING.add(new Pending(level, matched));
      }
   }

   public static void interrupt(ServerLevel level, BlockPos notePos) {
      PENDING.removeIf((p) -> ((Pending) p).level == level && ((Pending) p).involves(notePos));
   }

   private static void tick() {
      Iterator<Pending> iterator = PENDING.iterator();

      while(iterator.hasNext()) {
         if (!((Pending)iterator.next()).tick()) {
            iterator.remove();
         }
      }

   }

   private static class Pending {
      private final ServerLevel level;
      private final SonorousConsoles.MatchedSequence matched;
      private int ticksLeft = 10;
      private boolean burstDone = false;

      Pending(ServerLevel level, SonorousConsoles.MatchedSequence matched) {
         this.level = level;
         this.matched = matched;
      }

      boolean involves(BlockPos notePos) {
         return this.matched.notes().stream().anyMatch((n) -> ((SonorousConsoles.PlayedNote) n).notePos().equals(notePos));
      }

      boolean tick() {
         if (!this.isStillIntact()) {
            return false;
         } else if (this.ticksLeft > 0) {
            --this.ticksLeft;
            return true;
         } else if (this.burstDone) {
            SonorousAutoplay.start(this.level, this.matched);
            return false;
         } else {
            for (SonorousConsoles.PlayedNote note : (Iterable<SonorousConsoles.PlayedNote>) this.matched.notes()) {
               SonorousEffects.spawnColorBurst(this.level, note.notePos(), note.soundIndex1to8());
            }

            this.burstDone = true;
            this.ticksLeft = 20;
            return true;
         }
      }

      private boolean isStillIntact() {
         for (SonorousConsoles.PlayedNote note : (Iterable<SonorousConsoles.PlayedNote>) this.matched.notes()) {
            if (!this.level.getBlockState(note.notePos()).is(Blocks.NOTE_BLOCK) || !SonorousNoteBlocks.isSonorous(this.level, note.notePos())) {
               return false;
            }
         }

         return true;
      }
   }
}
