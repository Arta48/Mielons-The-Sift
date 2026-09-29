package mielon.thesift.block.entity;

import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SonorousNoteBlocks;
import mielon.thesift.portal.PortalAutoActivation;
import mielon.thesift.portal.SonorousBeams;
import mielon.thesift.portal.SonorousConsoles;
import mielon.thesift.portal.SonorousEffects;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SonorousDeepslateBlockEntity extends BlockEntity {
   private static final int BEAM_GROW_TICKS = 40;
   private static final int BEAM_SHRINK_TICKS = 40;
   private static final int AUTOPLAY_TICKS_BETWEEN_NOTES = 15;
   private int beamColor = -1;
   private int growAge = 0;
   private boolean shrinking = false;
   private int shrinkAge = 0;
   private int pendingShrinkTicks = -1;
   private boolean autoplayActive = false;
   private boolean autoplayClosing = false;
   private int autoplayIndex = 0;
   private int autoplayCooldown = 0;
   private long[] autoplayNotePositions = new long[0];
   private int[] autoplaySoundIndices = new int[0];

   public SonorousDeepslateBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlocks.SONOROUS_DEEPSLATE_BLOCK_ENTITY, pos, state);
   }

   public void startBeam(int colorRGB) {
      this.beamColor = colorRGB;
      this.growAge = 0;
      this.shrinking = false;
      this.shrinkAge = 0;
      this.pendingShrinkTicks = -1;
      this.setChanged();
   }

   public void clearBeam() {
      if (this.beamColor != -1) {
         this.beamColor = -1;
         this.growAge = 0;
         this.shrinking = false;
         this.shrinkAge = 0;
         this.pendingShrinkTicks = -1;
         this.setChanged();
      }
   }

   public void scheduleShrink(int ticks) {
      if (this.beamColor != -1 && !this.shrinking) {
         this.pendingShrinkTicks = Math.max(0, ticks);
         this.setChanged();
      }
   }

   public void startShrink() {
      if (this.beamColor != -1 && !this.shrinking) {
         this.shrinking = true;
         this.shrinkAge = 0;
         this.pendingShrinkTicks = -1;
         this.setChanged();
      }
   }

   public boolean hasBeam() {
      return this.beamColor != -1;
   }

   public int getBeamColor() {
      return this.beamColor;
   }

   public float getBeamGrowth(float partialTick) {
      if (this.beamColor == -1) {
         return 0.0F;
      } else if (this.shrinking) {
         float shrinkFraction = Mth.clamp(((float)this.shrinkAge + partialTick) / 40.0F, 0.0F, 1.0F);
         return 1.0F - shrinkFraction;
      } else {
         return Mth.clamp(((float)this.growAge + partialTick) / 40.0F, 0.0F, 1.0F);
      }
   }

   public void startAutoplay(SonorousConsoles.MatchedSequence matched) {
      int count = matched.notes().size();
      if (count <= 0) {
         this.stopAutoplay();
      } else {
         this.autoplayNotePositions = new long[count];
         this.autoplaySoundIndices = new int[count];

         for (int i = 0; i < count; ++i) {
            SonorousConsoles.PlayedNote note = (SonorousConsoles.PlayedNote)matched.notes().get(i);
            this.autoplayNotePositions[i] = note.notePos().asLong();
            this.autoplaySoundIndices[i] = note.soundIndex1to8();
         }

         this.autoplayClosing = matched.type() == SonorousConsoles.SequenceType.CLOSING;
         this.autoplayIndex = 0;
         this.autoplayCooldown = 0;
         this.autoplayActive = true;
         this.markPersistentChanged();
      }
   }

   public boolean stopAutoplay() {
      if (!this.autoplayActive) {
         return false;
      } else {
         this.clearAutoplayState();
         this.markPersistentChanged();
         return true;
      }
   }

   private void clearAutoplayState() {
      this.autoplayActive = false;
      this.autoplayClosing = false;
      this.autoplayIndex = 0;
      this.autoplayCooldown = 0;
      this.autoplayNotePositions = new long[0];
      this.autoplaySoundIndices = new int[0];
   }

   private void tickAutoplay(ServerLevel level) {
      if (this.autoplayActive) {
         if (this.autoplayNotePositions.length != 0 && this.autoplayNotePositions.length == this.autoplaySoundIndices.length) {
            if (this.autoplayCooldown > 0) {
               --this.autoplayCooldown;
               this.markPersistentChanged();
            } else if (this.autoplayIndex >= this.autoplayNotePositions.length) {
               this.finishAutoplay(level);
            } else {
               BlockPos notePos = BlockPos.of(this.autoplayNotePositions[this.autoplayIndex]);
               int soundIndex1to8 = this.autoplaySoundIndices[this.autoplayIndex];
               if (level.getBlockState(notePos).is(Blocks.NOTE_BLOCK) && SonorousNoteBlocks.isSonorous(level, notePos)) {
                  SonorousNoteBlocks.playSound(level, notePos, soundIndex1to8 - 1);
                  SonorousEffects.spawnColorBurst(level, notePos, soundIndex1to8);
                  SonorousBeams.start(level, notePos, soundIndex1to8);
                  ++this.autoplayIndex;
                  this.autoplayCooldown = 15;
                  this.markPersistentChanged();
               } else {
                  SonorousBeams.clearGroup(level, this.worldPosition);
                  level.playSound((Entity)null, notePos, ModSounds.SONOROUS_AUTOPLAY_GLITCH, SoundSource.RECORDS, 3.0F, 1.0F);
                  this.clearAutoplayState();
                  this.markPersistentChanged();
               }
            }
         } else {
            this.clearAutoplayState();
            this.markPersistentChanged();
         }
      }
   }

   private void finishAutoplay(ServerLevel level) {
      List<SonorousConsoles.PlayedNote> sequence = new ArrayList(this.autoplayNotePositions.length);

      for (int i = 0; i < this.autoplayNotePositions.length; ++i) {
         sequence.add(new SonorousConsoles.PlayedNote(BlockPos.of(this.autoplayNotePositions[i]), this.autoplaySoundIndices[i]));
      }

      boolean closing = this.autoplayClosing;
      this.clearAutoplayState();
      this.markPersistentChanged();
      if (closing) {
         PortalAutoActivation.tryCloseNearNotes(level, sequence);
      } else {
         PortalAutoActivation.tryActivateNearNotes(level, sequence);
      }

      if (!sequence.isEmpty()) {
         SonorousBeams.scheduleRetract(level, this.worldPosition);
      }

   }

   public static void tick(Level level, BlockPos pos, BlockState state, SonorousDeepslateBlockEntity blockEntity) {
      if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
         blockEntity.tickAutoplay(serverLevel);
      }

      if (blockEntity.beamColor != -1) {
         if (blockEntity.pendingShrinkTicks >= 0) {
            if (blockEntity.pendingShrinkTicks == 0) {
               blockEntity.pendingShrinkTicks = -1;
               blockEntity.startShrink();
            } else {
               --blockEntity.pendingShrinkTicks;
               if (!level.isClientSide()) {
                  blockEntity.markPersistentChanged();
               }
            }

         } else {
            if (blockEntity.shrinking) {
               if (blockEntity.shrinkAge < 40) {
                  ++blockEntity.shrinkAge;
                  if (!level.isClientSide()) {
                     blockEntity.markPersistentChanged();
                  }
               } else {
                  blockEntity.beamColor = -1;
                  blockEntity.shrinking = false;
                  blockEntity.shrinkAge = 0;
                  blockEntity.pendingShrinkTicks = -1;
                  if (!level.isClientSide()) {
                     blockEntity.setChanged();
                  }
               }
            } else if (blockEntity.growAge < 40) {
               ++blockEntity.growAge;
               if (!level.isClientSide()) {
                  blockEntity.markPersistentChanged();
               }
            }

         }
      }
   }

   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      output.putInt("beam_color", this.beamColor);
      output.putInt("beam_grow_age", this.growAge);
      output.putBoolean("beam_shrinking", this.shrinking);
      output.putInt("beam_shrink_age", this.shrinkAge);
      output.putInt("beam_pending_shrink_ticks", this.pendingShrinkTicks);
      output.putInt("persistent_autoplay_version", 1);
      output.putBoolean("autoplay_active", this.autoplayActive);
      output.putBoolean("autoplay_closing", this.autoplayClosing);
      output.putInt("autoplay_index", this.autoplayIndex);
      output.putInt("autoplay_cooldown", this.autoplayCooldown);
      output.putInt("autoplay_note_count", this.autoplayNotePositions.length);

      for (int i = 0; i < this.autoplayNotePositions.length; ++i) {
         output.putLong("autoplay_note_pos_" + i, this.autoplayNotePositions[i]);
         output.putInt("autoplay_note_sound_" + i, this.autoplaySoundIndices[i]);
      }

   }

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.beamColor = input.getIntOr("beam_color", -1);
      this.growAge = Math.max(0, input.getIntOr("beam_grow_age", 0));
      this.shrinking = input.getBooleanOr("beam_shrinking", false);
      this.shrinkAge = Math.max(0, input.getIntOr("beam_shrink_age", 0));
      this.pendingShrinkTicks = input.getIntOr("beam_pending_shrink_ticks", -1);
      int persistentAutoplayVersion = input.getIntOr("persistent_autoplay_version", 0);
      this.autoplayActive = input.getBooleanOr("autoplay_active", false);
      this.autoplayClosing = input.getBooleanOr("autoplay_closing", false);
      this.autoplayIndex = Math.max(0, input.getIntOr("autoplay_index", 0));
      this.autoplayCooldown = Math.max(0, input.getIntOr("autoplay_cooldown", 0));
      int count = Math.max(0, Math.min(64, input.getIntOr("autoplay_note_count", 0)));
      this.autoplayNotePositions = new long[count];
      this.autoplaySoundIndices = new int[count];

      for (int i = 0; i < count; ++i) {
         this.autoplayNotePositions[i] = (Long)input.getLong("autoplay_note_pos_" + i).orElse(0L);
         this.autoplaySoundIndices[i] = input.getIntOr("autoplay_note_sound_" + i, 1);
      }

      if (count == 0) {
         this.autoplayActive = false;
      } else {
         this.autoplayIndex = Math.min(this.autoplayIndex, count);
      }

      if (persistentAutoplayVersion == 0 && this.beamColor != -1 && !this.shrinking && this.pendingShrinkTicks < 0) {
         this.pendingShrinkTicks = 60;
      }

   }

   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      return this.saveWithoutMetadata(registries);
   }

   public Packet getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   private void markPersistentChanged() {
      super.setChanged();
   }

   public void setChanged() {
      super.setChanged();
      if (this.level != null) {
         BlockState state = this.getBlockState();
         this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
      }

   }
}
