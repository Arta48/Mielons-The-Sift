package mielon.thesift.portal;

import mielon.thesift.block.entity.SonorousDeepslateBlockEntity;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SonorousAutoplay {
   private SonorousAutoplay() {}

   public static void register() {}

   public static void start(ServerLevel level, SonorousConsoles.MatchedSequence matched) {
      if (!matched.notes().isEmpty()) {
         BlockPos anchor = SonorousConsoles.findAnchor(level, ((SonorousConsoles.PlayedNote)matched.notes().getFirst()).notePos());
         if (anchor != null) {
            BlockEntity var4 = level.getBlockEntity(anchor);
            if (var4 instanceof SonorousDeepslateBlockEntity blockEntity) {
                blockEntity.startAutoplay(matched);
            }

         }
      }
   }

   public static void interrupt(ServerLevel level, BlockPos anyBlockInGroup) {
      BlockPos anchor = SonorousConsoles.findAnchor(level, anyBlockInGroup);
       if (anchor != null) {
           BlockEntity var4 = level.getBlockEntity(anchor);
           if (var4 instanceof SonorousDeepslateBlockEntity blockEntity) {
               if (blockEntity.stopAutoplay()) {
                   SonorousBeams.clearGroup(level, anchor);
                   level.playSound(null, anyBlockInGroup, ModSounds.SONOROUS_AUTOPLAY_GLITCH, SoundSource.RECORDS, 3.0F, 1.0F);
               }
           }

       }
   }
}
