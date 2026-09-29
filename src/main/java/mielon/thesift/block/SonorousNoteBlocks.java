package mielon.thesift.block;

import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SonorousNoteBlocks {
   public static final int SOUND_COUNT = 8;

   private SonorousNoteBlocks() {
   }

   public static boolean isSonorous(Level level, BlockPos noteBlockPos) {
      BlockState below = level.getBlockState(noteBlockPos.below());
      return below.is(ModBlocks.SONOROUS_DEEPSLATE) && below.getValue(SonorousDeepslateBlock.MODE) == SonorousDeepslateBlock.Mode.NOTE;
   }

   public static boolean isHorn(Level level, BlockPos noteBlockPos) {
      BlockState below = level.getBlockState(noteBlockPos.below());
      return below.is(ModBlocks.SONOROUS_DEEPSLATE) && below.getValue(SonorousDeepslateBlock.MODE) == SonorousDeepslateBlock.Mode.HORN;
   }

   public static int getIndex(BlockState noteBlockState) {
      return (Integer)noteBlockState.getValue(NoteBlock.NOTE) % 8;
   }

   public static void playSound(Level level, BlockPos pos, int index) {
      level.playSound((Entity)null, pos, ModSounds.BY_INDEX[index], SoundSource.RECORDS, 3.0F, 1.0F);
   }

   public static void playHornGlitch(Level level, BlockPos pos) {
      level.playSound((Entity)null, pos, ModSounds.SONOROUS_AUTOPLAY_GLITCH, SoundSource.RECORDS, 3.0F, 1.0F);
   }
}
