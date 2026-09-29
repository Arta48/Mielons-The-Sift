package mielon.thesift.mixin;

import java.util.Optional;
import mielon.thesift.block.SonorousNoteBlocks;
import mielon.thesift.portal.SonorousAutoplay;
import mielon.thesift.portal.SonorousConsoles;
import mielon.thesift.portal.SonorousIgnition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({NoteBlock.class})
public abstract class NoteBlockMixin {
   private static final double THE_SIFT_NOTE_MARKER = (double)100.0F;

   @Inject(
      method = {"useWithoutItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$cycleSonorousSound(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable cir) {
      if (SonorousNoteBlocks.isHorn(level, pos)) {
         if (!level.isClientSide()) {
            SonorousNoteBlocks.playHornGlitch(level, pos);
         }

         cir.setReturnValue(InteractionResult.SUCCESS);
      } else if (SonorousNoteBlocks.isSonorous(level, pos)) {
         if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel)level;
            SonorousIgnition.interrupt(serverLevel, pos);
            SonorousAutoplay.interrupt(serverLevel, pos);
            int current = SonorousNoteBlocks.getIndex(state);
            int nextStored = (current + 1) % 8;
            BlockState newState = (BlockState)state.setValue(NoteBlock.NOTE, nextStored);
            level.setBlock(pos, newState, 3);
            level.blockEvent(pos, newState.getBlock(), 0, 0);
            player.awardStat(Stats.TUNE_NOTEBLOCK);
         }

         cir.setReturnValue(InteractionResult.SUCCESS);
      }
   }

   @Inject(
      method = {"attack"},
      at = {@At("HEAD")}
   )
   private void theSift$trackSonorousAttack(BlockState state, Level level, BlockPos pos, Player player, CallbackInfo ci) {
      if (SonorousNoteBlocks.isSonorous(level, pos) && !level.isClientSide()) {
         ServerLevel serverLevel = (ServerLevel)level;
         SonorousIgnition.interrupt(serverLevel, pos);
         SonorousAutoplay.interrupt(serverLevel, pos);
         int current = SonorousNoteBlocks.getIndex(state);
         Optional<SonorousConsoles.MatchedSequence> winning = SonorousConsoles.onSoundPlayed(serverLevel, pos, current + 1);
         winning.ifPresent((matched) -> SonorousIgnition.start(serverLevel, matched));
      }
   }

   @Inject(
      method = {"triggerEvent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void theSift$playSonorousSound(BlockState state, Level level, BlockPos pos, int id, int param, CallbackInfoReturnable cir) {
      if (SonorousNoteBlocks.isHorn(level, pos)) {
         SonorousNoteBlocks.playHornGlitch(level, pos);
         cir.setReturnValue(true);
      } else if (SonorousNoteBlocks.isSonorous(level, pos)) {
         int index = SonorousNoteBlocks.getIndex(state);
         double encodedColor = (double)100.0F + (double)(index * 3);
         level.addParticle(ParticleTypes.NOTE, (double)pos.getX() + (double)0.5F, (double)pos.getY() + 1.2, (double)pos.getZ() + (double)0.5F, encodedColor, (double)0.0F, (double)0.0F);
         SonorousNoteBlocks.playSound(level, pos, index);
         cir.setReturnValue(true);
      }
   }
}
